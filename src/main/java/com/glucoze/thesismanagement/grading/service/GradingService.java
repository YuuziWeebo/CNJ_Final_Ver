package com.glucoze.thesismanagement.grading.service;

import com.glucoze.thesismanagement.common.enums.ScoreSource;
import com.glucoze.thesismanagement.common.enums.CouncilRole;
import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.common.exception.ResourceNotFoundException;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import com.glucoze.thesismanagement.council.entity.CouncilMember;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.council.repository.CouncilMemberRepository;
import com.glucoze.thesismanagement.grading.dto.ScoreForm;
import com.glucoze.thesismanagement.grading.entity.Grade;
import com.glucoze.thesismanagement.grading.entity.Result;
import com.glucoze.thesismanagement.grading.repository.GradeRepository;
import com.glucoze.thesismanagement.grading.repository.ResultRepository;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.notification.NotificationService;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.audit.AuditAction;
import com.glucoze.thesismanagement.audit.AuditEntityType;
import com.glucoze.thesismanagement.audit.AuditLogService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingService {

    private final DefenseScheduleRepository scheduleRepository;
    private final GradeRepository scoreRepository;
    private final ResultRepository resultRepository;
    private final CouncilMemberRepository memberRepository;
    private final LecturerRepository lecturerRepository;
    private final GradingCalculator calculator;
    private final RegistrationRepository registrationRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public GradingService(DefenseScheduleRepository scheduleRepository,
                          GradeRepository scoreRepository,
                          ResultRepository resultRepository,
                          CouncilMemberRepository memberRepository,
                          LecturerRepository lecturerRepository,
                          GradingCalculator calculator, RegistrationRepository registrationRepository,
                          NotificationService notificationService, AuditLogService auditLogService) {
        this.scheduleRepository = scheduleRepository;
        this.scoreRepository = scoreRepository;
        this.resultRepository = resultRepository;
        this.memberRepository = memberRepository;
        this.lecturerRepository = lecturerRepository;
        this.calculator = calculator;
        this.registrationRepository = registrationRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void enterSupervisorScore(String username, Long scheduleId, ScoreForm form) {
        DefenseSchedule schedule = findScheduleForUpdate(scheduleId);
        ensureNotFinalized(scheduleId);
        if (!schedule.getThesis().getSupervisor().getUserAccount().getUsername().equals(username)) {
            throw new DomainRuleViolationException("Giảng viên không phải người hướng dẫn đề tài này");
        }
        saveScore(schedule, username, ScoreSource.SUPERVISOR, form.getScore());
    }

    @Transactional
    public void enterCouncilScore(String username, Long scheduleId, ScoreForm form) {
        DefenseSchedule schedule = findScheduleForUpdate(scheduleId);
        ensureNotFinalized(scheduleId);
        Long lecturerId = findLecturerId(schedule, username);
        if (!memberRepository.existsByCouncilIdAndLecturerId(schedule.getCouncil().getId(), lecturerId)) {
            throw new DomainRuleViolationException("Giảng viên không thuộc hội đồng này");
        }
        saveScore(schedule, username, ScoreSource.COUNCIL, form.getScore());
    }

    @Transactional
    public void publishResult(String username, Long scheduleId) {
        DefenseSchedule schedule = findScheduleForUpdate(scheduleId);
        Result existingResult = resultRepository.findByDefenseScheduleId(scheduleId).orElse(null);
        if (existingResult != null && existingResult.isPublished()) {
            throw new DomainRuleViolationException("Kết quả cuối cùng đã được công bố và không thể thay đổi");
        }
        List<CouncilMember> members = requireCompleteCouncil(schedule);
        ensureCanPublish(username, members);
        BigDecimal supervisorScore = scoreRepository.findAllByDefenseScheduleIdAndScoreSource(scheduleId, ScoreSource.SUPERVISOR)
                .stream().map(Grade::getScore).findFirst().orElse(null);
        List<BigDecimal> councilScores = requireAllCouncilScores(schedule, members);
        councilScores.forEach(score -> calculator.validateScore(score, "Điểm hội đồng"));
        BigDecimal councilAverage = councilScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(councilScores.size()), 4, RoundingMode.HALF_UP);
        BigDecimal finalScore = calculator.calculate(supervisorScore, councilAverage);
        Result result = existingResult == null ? new Result(schedule, finalScore) : existingResult;
        result.publish();
        resultRepository.save(result);
        auditLogService.append(AuditAction.RESULT_PUBLISH, AuditEntityType.RESULT, result.getId(),
                "Đã công bố kết quả cho lịch bảo vệ #" + scheduleId);
        registrationRepository.findFirstByThesisIdAndStatus(schedule.getThesis().getId(), RegistrationStatus.APPROVED)
                .ifPresent(notificationService::resultPublished);
    }

    private void saveScore(DefenseSchedule schedule, String username, ScoreSource source, BigDecimal score) {
        calculator.validateScore(score, source.toString());
        Long lecturerId = findLecturerId(schedule, username);
        Lecturer lecturer = lecturerRepository.findById(lecturerId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giảng viên"));
        Grade existing = scoreRepository.findByDefenseScheduleIdAndLecturerIdAndScoreSource(
                schedule.getId(), lecturerId, source).orElse(null);
        Grade grade = existing == null ? new Grade(schedule, lecturer, source, score) : existing;
        grade.updateScore(score);
        scoreRepository.save(grade);
        auditLogService.append(existing == null ? AuditAction.GRADE_CREATE : AuditAction.GRADE_UPDATE,
                AuditEntityType.GRADE, grade.getId(),
                (existing == null ? "Đã nhập" : "Đã cập nhật") + " điểm cho lịch bảo vệ #" + schedule.getId());
    }

    private void ensureCanPublish(String username, List<CouncilMember> members) {
        CouncilMember membership = members.stream()
                .filter(member -> member.getLecturer().getUserAccount().getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolationException(
                        "Chỉ Chủ tịch hoặc Thư ký của hội đồng này được công bố kết quả"));
        if (membership.getRole() != CouncilRole.CHAIR && membership.getRole() != CouncilRole.SECRETARY) {
            throw new DomainRuleViolationException("Ủy viên không có quyền công bố kết quả");
        }
    }

    private Long findLecturerId(DefenseSchedule schedule, String username) {
        return schedule.getThesis().getSupervisor().getUserAccount().getUsername().equals(username)
                ? schedule.getThesis().getSupervisor().getId()
                : memberRepository.findByCouncilId(schedule.getCouncil().getId()).stream()
                .filter(member -> member.getLecturer().getUserAccount().getUsername().equals(username))
                .map(member -> member.getLecturer().getId())
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolationException("Giảng viên không liên quan đến buổi bảo vệ này"));
    }

    private DefenseSchedule findScheduleForUpdate(Long scheduleId) {
        return scheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch bảo vệ"));
    }

    private void ensureNotFinalized(Long scheduleId) {
        if (resultRepository.findByDefenseScheduleId(scheduleId).filter(Result::isPublished).isPresent()) {
            throw new DomainRuleViolationException("Kết quả đã được công bố; không thể thêm hoặc sửa điểm");
        }
    }

    private List<CouncilMember> requireCompleteCouncil(DefenseSchedule schedule) {
        List<CouncilMember> members = memberRepository.findByCouncilId(schedule.getCouncil().getId());
        Set<Long> memberIds = new HashSet<>();
        Set<CouncilRole> roles = EnumSet.noneOf(CouncilRole.class);
        for (CouncilMember member : members) {
            if (member.getLecturer() == null || member.getLecturer().getId() == null
                    || member.getRole() == null) {
                throw new DomainRuleViolationException("Dữ liệu thành viên hội đồng không hợp lệ");
            }
            memberIds.add(member.getLecturer().getId());
            roles.add(member.getRole());
        }
        if (members.size() != 3 || memberIds.size() != 3
                || !roles.equals(EnumSet.allOf(CouncilRole.class))) {
            throw new DomainRuleViolationException(
                    "Hội đồng phải có đúng Chủ tịch, Thư ký và Ủy viên trước khi công bố");
        }
        return members;
    }

    private List<BigDecimal> requireAllCouncilScores(DefenseSchedule schedule, List<CouncilMember> members) {
        Set<Long> expectedIds = members.stream()
                .map(member -> member.getLecturer().getId())
                .collect(java.util.stream.Collectors.toSet());
        List<Grade> scores = scoreRepository.findAllByDefenseScheduleIdAndScoreSource(
                schedule.getId(), ScoreSource.COUNCIL);
        Set<Long> submittedIds = new HashSet<>();
        for (Grade score : scores) {
            if (score.getLecturer() == null || score.getLecturer().getId() == null) {
                throw new DomainRuleViolationException("Dữ liệu điểm hội đồng không hợp lệ");
            }
            submittedIds.add(score.getLecturer().getId());
        }
        if (scores.size() != 3 || submittedIds.size() != 3 || !submittedIds.equals(expectedIds)) {
            throw new DomainRuleViolationException(
                    "Cần đủ điểm của đúng 3 thành viên hội đồng trước khi công bố");
        }
        return scores.stream().map(Grade::getScore).toList();
    }

}
