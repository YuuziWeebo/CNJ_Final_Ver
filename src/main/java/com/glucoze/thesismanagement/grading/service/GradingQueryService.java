package com.glucoze.thesismanagement.grading.service;

import com.glucoze.thesismanagement.common.enums.ScoreSource;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.exception.ResourceNotFoundException;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import com.glucoze.thesismanagement.council.entity.CouncilMember;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.council.repository.CouncilMemberRepository;
import com.glucoze.thesismanagement.grading.dto.GradeView;
import com.glucoze.thesismanagement.grading.entity.Grade;
import com.glucoze.thesismanagement.grading.entity.Result;
import com.glucoze.thesismanagement.grading.repository.GradeRepository;
import com.glucoze.thesismanagement.grading.repository.ResultRepository;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingQueryService {

    private final DefenseScheduleRepository scheduleRepository;
    private final GradeRepository scoreRepository;
    private final ResultRepository resultRepository;
    private final CouncilMemberRepository memberRepository;
    private final RegistrationRepository registrationRepository;

    public GradingQueryService(DefenseScheduleRepository scheduleRepository,
                               GradeRepository scoreRepository,
                               ResultRepository resultRepository,
                               CouncilMemberRepository memberRepository,
                               RegistrationRepository registrationRepository) {
        this.scheduleRepository = scheduleRepository;
        this.scoreRepository = scoreRepository;
        this.resultRepository = resultRepository;
        this.memberRepository = memberRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional(readOnly = true)
    public List<GradeView> listLecturerSchedules(String username) {
        Map<Long, DefenseSchedule> schedules = new LinkedHashMap<>();
        scheduleRepository.findByThesisSupervisorUserAccountUsername(username)
                .forEach(schedule -> schedules.put(schedule.getId(), schedule));
        scheduleRepository.findByCouncilMemberUsername(username)
                .forEach(schedule -> schedules.put(schedule.getId(), schedule));
        return toGradeViews(schedules.values());
    }

    @Transactional(readOnly = true)
    public List<GradeView> listStudentGrades(String username) {
        List<Registration> registrations = registrationRepository
                .findByStudentUserAccountUsernameAndStatus(username, RegistrationStatus.APPROVED);
        Collection<Long> thesisIds = registrations.stream().map(registration -> registration.getThesis().getId()).toList();
        if (thesisIds.isEmpty()) {
            return List.of();
        }
        return toGradeViews(scheduleRepository.findByThesisIdIn(thesisIds)).stream()
                .filter(GradeView::published)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countStudentPublishedGrades(String username) {
        return resultRepository.countPublishedByStudent(username, RegistrationStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public long countLecturerSchedules(String username) {
        return scheduleRepository.countByLecturerAccess(username);
    }

    @Transactional(readOnly = true)
    public GradeView getLecturerGrade(String username, Long scheduleId) {
        DefenseSchedule schedule = scheduleRepository.findByIdAndLecturerAccess(scheduleId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch chấm điểm"));
        return toGradeView(schedule);
    }

    @Transactional(readOnly = true)
    public GradeView getStudentGrade(String username, Long scheduleId) {
        DefenseSchedule schedule = findSchedule(scheduleId);
        registrationRepository.findByThesisIdAndStudentUserAccountUsernameAndStatus(
                        schedule.getThesis().getId(), username, RegistrationStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả đã công bố"));
        GradeView view = toGradeView(schedule);
        if (!view.published()) {
            throw new ResourceNotFoundException("Không tìm thấy kết quả đã công bố");
        }
        return view;
    }

    private DefenseSchedule findSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch bảo vệ"));
    }

    private GradeView toGradeView(DefenseSchedule schedule) {
        List<Grade> scores = scoreRepository.findByDefenseScheduleId(schedule.getId());
        List<CouncilMember> members = memberRepository.findByCouncilId(schedule.getCouncil().getId());
        Result result = resultRepository.findByDefenseScheduleId(schedule.getId()).orElse(null);
        return toGradeView(schedule, scores, members, result);
    }

    private List<GradeView> toGradeViews(Collection<DefenseSchedule> schedules) {
        if (schedules.isEmpty()) {
            return List.of();
        }
        List<Long> scheduleIds = schedules.stream().map(DefenseSchedule::getId).toList();
        List<Long> councilIds = schedules.stream().map(schedule -> schedule.getCouncil().getId()).distinct().toList();
        Map<Long, List<Grade>> scoresBySchedule = scoreRepository.findByDefenseScheduleIdIn(scheduleIds).stream()
                .collect(Collectors.groupingBy(score -> score.getDefenseSchedule().getId()));
        Map<Long, List<CouncilMember>> membersByCouncil = memberRepository.findByCouncilIdIn(councilIds).stream()
                .collect(Collectors.groupingBy(member -> member.getCouncil().getId()));
        Map<Long, Result> resultsBySchedule = resultRepository.findByDefenseScheduleIdIn(scheduleIds).stream()
                .collect(Collectors.toMap(result -> result.getDefenseSchedule().getId(), Function.identity()));
        return schedules.stream()
                .map(schedule -> toGradeView(
                        schedule,
                        scoresBySchedule.getOrDefault(schedule.getId(), List.of()),
                        membersByCouncil.getOrDefault(schedule.getCouncil().getId(), List.of()),
                        resultsBySchedule.get(schedule.getId())))
                .toList();
    }

    private GradeView toGradeView(DefenseSchedule schedule, List<Grade> scores,
                                  List<CouncilMember> members, Result result) {
        BigDecimal supervisor = scores.stream()
                .filter(score -> score.getScoreSource() == ScoreSource.SUPERVISOR)
                .map(Grade::getScore)
                .findFirst()
                .orElse(null);
        Set<Long> councilMemberIds = members.stream()
                .map(member -> member.getLecturer().getId())
                .collect(Collectors.toSet());
        List<BigDecimal> councilScores = scores.stream()
                .filter(score -> score.getScoreSource() == ScoreSource.COUNCIL)
                .filter(score -> score.getLecturer() != null && councilMemberIds.contains(score.getLecturer().getId()))
                .map(Grade::getScore)
                .toList();
        BigDecimal council = councilScores.isEmpty() ? null : councilScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(councilScores.size()), 4, RoundingMode.HALF_UP);
        BigDecimal finalScore = result == null ? null : result.getFinalScore();
        boolean published = result != null && result.isPublished();
        return new GradeView(schedule.getId(), schedule.getThesis().getTitle(), supervisor, council, finalScore, published);
    }
}
