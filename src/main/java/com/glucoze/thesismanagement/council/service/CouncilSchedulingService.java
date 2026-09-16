package com.glucoze.thesismanagement.council.service;

import com.glucoze.thesismanagement.common.exception.ResourceNotFoundException;
import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.enums.CouncilRole;
import com.glucoze.thesismanagement.council.dto.CouncilForm;
import com.glucoze.thesismanagement.council.dto.CouncilView;
import com.glucoze.thesismanagement.council.dto.MemberForm;
import com.glucoze.thesismanagement.council.dto.MemberView;
import com.glucoze.thesismanagement.council.dto.OptionView;
import com.glucoze.thesismanagement.council.dto.ScheduleForm;
import com.glucoze.thesismanagement.council.dto.ScheduleView;
import com.glucoze.thesismanagement.council.entity.Council;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import com.glucoze.thesismanagement.council.entity.CouncilMember;
import com.glucoze.thesismanagement.council.repository.CouncilRepository;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.council.repository.CouncilMemberRepository;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.notification.NotificationService;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import com.glucoze.thesismanagement.audit.AuditAction;
import com.glucoze.thesismanagement.audit.AuditEntityType;
import com.glucoze.thesismanagement.audit.AuditLogService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouncilSchedulingService {

    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository memberRepository;
    private final DefenseScheduleRepository defenseScheduleRepository;
    private final LecturerRepository lecturerRepository;
    private final ThesisRepository thesisRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public CouncilSchedulingService(CouncilRepository councilRepository,
                                    CouncilMemberRepository memberRepository,
                                    DefenseScheduleRepository defenseScheduleRepository,
                                    LecturerRepository lecturerRepository,
                                    ThesisRepository thesisRepository,
                                    RegistrationRepository registrationRepository,
                                    NotificationService notificationService,
                                    AuditLogService auditLogService) {
        this.councilRepository = councilRepository;
        this.memberRepository = memberRepository;
        this.defenseScheduleRepository = defenseScheduleRepository;
        this.lecturerRepository = lecturerRepository;
        this.thesisRepository = thesisRepository;
        this.registrationRepository = registrationRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CouncilView> listCouncils() {
        List<Council> councils = councilRepository.findAll();
        if (councils.isEmpty()) {
            return List.of();
        }
        List<Long> councilIds = councils.stream().map(Council::getId).toList();
        Map<Long, Long> memberCounts = memberRepository.countMembersByCouncilIds(councilIds).stream()
                .collect(Collectors.toMap(
                        CouncilMemberRepository.CouncilMemberCount::getCouncilId,
                        CouncilMemberRepository.CouncilMemberCount::getMemberCount));
        return councils.stream()
                .map(council -> new CouncilView(council.getId(), council.getName(), council.getAcademicYear(),
                        council.getSemester(), memberCounts.getOrDefault(council.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public long countCouncils() {
        return councilRepository.count();
    }

    @Transactional(readOnly = true)
    public List<OptionView> lecturerOptions() {
        return lecturerRepository.findAll().stream()
                .map(lecturer -> new OptionView(lecturer.getId(), lecturer.getLecturerCode() + " - " + lecturer.getFullName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OptionView> thesisOptions() {
        return thesisRepository.findAll().stream()
                .map(thesis -> new OptionView(thesis.getId(), thesis.getTitle()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OptionView> councilOptions() {
        return councilRepository.findAll().stream()
                .map(council -> new OptionView(council.getId(), council.getName()))
                .toList();
    }

    @Transactional
    public void createCouncil(CouncilForm form) {
        ensureDistinctMembers(form.getChairId(), form.getSecretaryId(), form.getMemberId());
        Council council = councilRepository.save(new Council(clean(form.getName()), clean(form.getAcademicYear()), clean(form.getSemester())));
        assignMember(council, findLecturer(form.getChairId()), CouncilRole.CHAIR);
        assignMember(council, findLecturer(form.getSecretaryId()), CouncilRole.SECRETARY);
        assignMember(council, findLecturer(form.getMemberId()), CouncilRole.MEMBER);
        auditLogService.append(AuditAction.COUNCIL_CREATE, AuditEntityType.COUNCIL, council.getId(),
                "Đã tạo hội đồng “" + council.getName() + "”");
    }

    @Transactional(readOnly = true)
    public CouncilForm getCouncilForm(Long councilId) {
        Council council = findCouncil(councilId);
        CouncilForm form = new CouncilForm();
        form.setName(council.getName());
        form.setAcademicYear(council.getAcademicYear());
        form.setSemester(council.getSemester());
        return form;
    }

    @Transactional
    public void updateCouncil(Long councilId, CouncilForm form) {
        Council council = findCouncil(councilId);
        council.updateDetails(clean(form.getName()), clean(form.getAcademicYear()), clean(form.getSemester()));
        auditLogService.append(AuditAction.COUNCIL_UPDATE, AuditEntityType.COUNCIL, councilId,
                "Đã cập nhật hội đồng “" + council.getName() + "”");
    }

    @Transactional
    public void deleteCouncil(Long councilId) {
        Council council = findCouncil(councilId);
        if (defenseScheduleRepository.existsByCouncilId(councilId)) {
            throw new DomainRuleViolationException("Không thể xóa hội đồng đã có lịch bảo vệ");
        }
        memberRepository.findByCouncilId(councilId).forEach(memberRepository::delete);
        councilRepository.delete(council);
        auditLogService.append(AuditAction.COUNCIL_DELETE, AuditEntityType.COUNCIL, councilId,
                "Đã xóa hội đồng “" + council.getName() + "”");
    }

    @Transactional(readOnly = true)
    public List<MemberView> listMembers(Long councilId) {
        findCouncil(councilId);
        return memberRepository.findByCouncilId(councilId).stream()
                .map(member -> new MemberView(member.getId(), member.getLecturer().getFullName(),
                        member.getLecturer().getLecturerCode(), member.getRole()))
                .toList();
    }

    @Transactional
    public void addMember(Long councilId, MemberForm form) {
        Council council = findCouncil(councilId);
        Lecturer lecturer = findLecturer(form.getLecturerId());
        if (memberRepository.existsByCouncilIdAndLecturerId(councilId, lecturer.getId())) {
            throw new DomainRuleViolationException("Giảng viên đã là thành viên hội đồng");
        }
        if (memberRepository.existsByCouncilIdAndRole(councilId, form.getRole())) {
            throw new DomainRuleViolationException("Vai trò trong hội đồng đã được phân công");
        }
        assignMember(council, lecturer, form.getRole());
    }

    @Transactional
    public void deleteMember(Long councilId, Long memberId) {
        findCouncil(councilId);
        List<CouncilMember> members = memberRepository.findByCouncilId(councilId);
        if (members.size() <= 3) {
            throw new DomainRuleViolationException("Hội đồng phải có ít nhất ba thành viên");
        }
        CouncilMember member = members.stream()
                .filter(candidate -> memberId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên hội đồng"));
        if (member.getRole() == CouncilRole.CHAIR || member.getRole() == CouncilRole.SECRETARY) {
            throw new DomainRuleViolationException("Hội đồng phải giữ Chủ tịch và Thư ký (Chairperson and Secretary must remain)");
        }
        memberRepository.delete(member);
        auditLogService.append(AuditAction.COUNCIL_MEMBER_REMOVE, AuditEntityType.COUNCIL_MEMBER, memberId,
                "Đã gỡ thành viên khỏi hội đồng #" + councilId);
    }

    @Transactional(readOnly = true)
    public List<ScheduleView> listSchedules() {
        return defenseScheduleRepository.findAll().stream()
                .map(schedule -> new ScheduleView(schedule.getId(), schedule.getThesis().getTitle(),
                        schedule.getCouncil().getName(), schedule.getRoom(), schedule.getStartTime(), schedule.getEndTime()))
                .toList();
    }

    @Transactional
    public void createSchedule(ScheduleForm form) {
        ScheduleConflictPolicy.requireValidInterval(form.getStartTime(), form.getEndTime());
        String room = clean(form.getRoom());
        // A room is currently stored as text and has no row that can be locked.
        // Serialize this low-volume admin operation on a stable database row so
        // two instances cannot both pass an empty room-conflict query.
        councilRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hội đồng"));
        Thesis thesis = thesisRepository.findByIdForUpdate(form.getThesisId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề tài"));
        Council council = councilRepository.findByIdForUpdate(form.getCouncilId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hội đồng"));
        Registration registration = registrationRepository.findFirstByThesisIdAndStatus(thesis.getId(), RegistrationStatus.APPROVED)
                .orElseThrow(() -> new DomainRuleViolationException("Chỉ đề tài đã được duyệt mới được xếp lịch bảo vệ (approved thesis required)"));
        if (defenseScheduleRepository.existsByThesisId(thesis.getId())) {
            throw new DomainRuleViolationException("Đề tài đã có lịch bảo vệ (already has a defense schedule)");
        }
        List<CouncilMember> members = memberRepository.findByCouncilId(council.getId());
        ensureCompleteCouncil(members);
        lockCouncilMembers(members);

        LocalDateTime conflictStart = form.getStartTime().minusMinutes(ScheduleConflictPolicy.BUFFER_MINUTES);
        LocalDateTime conflictEnd = form.getEndTime().plusMinutes(ScheduleConflictPolicy.BUFFER_MINUTES);
        if (!defenseScheduleRepository.findCouncilConflicts(council.getId(), conflictStart, conflictEnd).isEmpty()) {
            throw new DomainRuleViolationException("Hội đồng bị trùng lịch (Council conflict)");
        }
        if (!defenseScheduleRepository.findRoomConflicts(room, conflictStart, conflictEnd).isEmpty()) {
            throw new DomainRuleViolationException("Phòng bị trùng lịch (Room conflict)");
        }
        for (CouncilMember member : members) {
            if (!defenseScheduleRepository.findLecturerConflicts(member.getLecturer().getId(), conflictStart, conflictEnd).isEmpty()) {
                throw new DomainRuleViolationException("Giảng viên bị trùng lịch (Lecturer conflict)");
            }
        }
        DefenseSchedule schedule = new DefenseSchedule(thesis, council, room, form.getStartTime(), form.getEndTime());
        defenseScheduleRepository.saveAndFlush(schedule);
        notificationService.defenseScheduled(schedule, registration, members);
        auditLogService.append(AuditAction.SCHEDULE_CREATE, AuditEntityType.DEFENSE_SCHEDULE, schedule.getId(),
                "Đã tạo lịch bảo vệ cho đề tài #" + thesis.getId());
    }

    private void ensureCompleteCouncil(List<CouncilMember> members) {
        if (members.size() < 3
                || !hasPosition(members, CouncilRole.CHAIR)
                || !hasPosition(members, CouncilRole.SECRETARY)
                || !hasPosition(members, CouncilRole.MEMBER)) {
            throw new DomainRuleViolationException("Hội đồng cần đủ Chủ tịch, Thư ký và Ủy viên");
        }
    }

    private void lockCouncilMembers(List<CouncilMember> members) {
        List<Long> memberIds = members.stream()
                .map(member -> member.getLecturer().getId())
                .sorted()
                .toList();
        lecturerRepository.findAllByIdForUpdate(memberIds);
    }

    private boolean hasPosition(List<CouncilMember> members, CouncilRole position) {
        return members.stream().anyMatch(member -> member.getRole() == position);
    }

    private void assignMember(Council council, Lecturer lecturer, CouncilRole role) {
        CouncilMember member = new CouncilMember(council, lecturer, role);
        memberRepository.save(member);
        notificationService.councilAssigned(member);
        auditLogService.append(AuditAction.COUNCIL_MEMBER_ADD, AuditEntityType.COUNCIL_MEMBER, member.getId(),
                "Đã thêm thành viên vào hội đồng #" + council.getId());
    }

    private void ensureDistinctMembers(Long chair, Long secretary, Long member) {
        if (chair == null || secretary == null || member == null
            || chair.equals(secretary) || chair.equals(member) || secretary.equals(member)) {
            throw new DomainRuleViolationException("Ba thành viên hội đồng phải là ba giảng viên khác nhau");
        }
    }

    private Council findCouncil(Long id) {
        return councilRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hội đồng"));
    }

    private Lecturer findLecturer(Long id) {
        return lecturerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giảng viên"));
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainRuleViolationException("Thiếu thông tin bắt buộc");
        }
        return value.trim();
    }
}
