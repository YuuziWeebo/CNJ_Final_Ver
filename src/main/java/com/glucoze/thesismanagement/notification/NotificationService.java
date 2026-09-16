package com.glucoze.thesismanagement.notification;

import com.glucoze.thesismanagement.common.enums.ReportStatus;
import com.glucoze.thesismanagement.common.exception.ResourceNotFoundException;
import com.glucoze.thesismanagement.council.entity.CouncilMember;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import com.glucoze.thesismanagement.thesis.entity.Report;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class NotificationService {
    private static final int PAGE_SIZE = 20;
    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public Page<NotificationView> listForUser(String username, int requestedPage) {
        int page = Math.max(0, requestedPage);
        return repository.findByUserAccountUsername(username,
                        PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toView);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        return repository.countByUserAccountUsernameAndReadFalse(username);
    }

    @Transactional
    public void markRead(String username, Long id) {
        Notification notification = repository.findByIdAndUserAccountUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));
        notification.markRead();
    }

    @Transactional
    public void markAllRead(String username) { repository.markAllReadByUsername(username); }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrationSubmitted(Registration registration) {
        save(registration.getThesis().getSupervisor().getUserAccount(), NotificationType.REGISTRATION_SUBMITTED,
                "Có đăng ký đề tài mới", registration.getStudent().getFullName() + " đã đăng ký đề tài “"
                        + registration.getThesis().getTitle() + "”.", "/lecturer/registrations");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrationReviewed(Registration registration, boolean approved) {
        save(registration.getStudent().getUserAccount(), approved ? NotificationType.REGISTRATION_APPROVED : NotificationType.REGISTRATION_REJECTED,
                approved ? "Đăng ký đã được duyệt" : "Đăng ký bị từ chối",
                "Đăng ký đề tài “" + registration.getThesis().getTitle() + "” " + (approved ? "đã được duyệt." : "đã bị từ chối."),
                "/student/theses");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reportSubmitted(Report report, ReportStatus previousStatus) {
        boolean resubmission = previousStatus == ReportStatus.REVISION_REQUIRED;
        save(report.getRegistration().getThesis().getSupervisor().getUserAccount(),
                resubmission ? NotificationType.REPORT_RESUBMITTED : NotificationType.REPORT_SUBMITTED,
                resubmission ? "Báo cáo đã được nộp lại" : "Có báo cáo mới",
                report.getRegistration().getStudent().getFullName() + " đã " + (resubmission ? "nộp lại" : "nộp")
                        + " báo cáo cho đề tài “" + report.getRegistration().getThesis().getTitle() + "”.", "/lecturer/reports");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reportReviewed(Report report, boolean approved) {
        save(report.getRegistration().getStudent().getUserAccount(),
                approved ? NotificationType.REPORT_APPROVED : NotificationType.REPORT_REVISION_REQUIRED,
                approved ? "Báo cáo đã được duyệt" : "Báo cáo cần chỉnh sửa",
                approved ? "Báo cáo khóa luận của bạn đã được duyệt."
                        : "Giảng viên yêu cầu chỉnh sửa báo cáo khóa luận của bạn.", "/student/reports");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void councilAssigned(CouncilMember member) {
        save(member.getLecturer().getUserAccount(), NotificationType.COUNCIL_ASSIGNED, "Phân công hội đồng",
                "Bạn được phân công vào " + member.getCouncil().getName() + " với vai trò " + member.getRole() + ".",
                "/lecturer/grading");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void defenseScheduled(DefenseSchedule schedule, Registration registration, List<CouncilMember> members) {
        Map<String, UserAccount> lecturers = new LinkedHashMap<>();
        UserAccount supervisor = schedule.getThesis().getSupervisor().getUserAccount();
        lecturers.put(supervisor.getUsername(), supervisor);
        members.forEach(member -> lecturers.putIfAbsent(member.getLecturer().getUserAccount().getUsername(),
                member.getLecturer().getUserAccount()));
        save(registration.getStudent().getUserAccount(), NotificationType.DEFENSE_SCHEDULED, "Đã có lịch bảo vệ",
                "Đề tài của bạn đã được xếp lịch bảo vệ tại " + schedule.getRoom() + ".", "/student/theses");
        lecturers.values().forEach(account -> save(account, NotificationType.DEFENSE_SCHEDULED, "Lịch bảo vệ mới",
                "Đề tài “" + schedule.getThesis().getTitle() + "” đã được xếp lịch bảo vệ.", "/lecturer/grading"));
        lecturers.values().forEach(account -> save(account, NotificationType.GRADING_TASK_ASSIGNED, "Nhiệm vụ chấm điểm",
                "Bạn có nhiệm vụ chấm điểm cho đề tài “" + schedule.getThesis().getTitle() + "”.", "/lecturer/grading"));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void resultPublished(Registration registration) {
        save(registration.getStudent().getUserAccount(), NotificationType.RESULT_PUBLISHED, "Kết quả đã được công bố",
                "Kết quả bảo vệ đề tài “" + registration.getThesis().getTitle() + "” đã được công bố.", "/student/grading");
    }

    private void save(UserAccount recipient, NotificationType type, String title, String message, String targetUrl) {
        repository.save(new Notification(recipient, type, title, message, targetUrl));
    }

    private NotificationView toView(Notification n) {
        return new NotificationView(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getTargetUrl(), n.isRead(), n.getCreatedAt());
    }
}
