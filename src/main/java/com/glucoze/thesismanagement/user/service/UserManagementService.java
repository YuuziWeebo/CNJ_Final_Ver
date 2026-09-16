package com.glucoze.thesismanagement.user.service;

import com.glucoze.thesismanagement.common.exception.ResourceNotFoundException;
import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.user.dto.AdminUserForm;
import com.glucoze.thesismanagement.user.dto.AdminUserEditForm;
import com.glucoze.thesismanagement.user.dto.UserSummaryView;
import com.glucoze.thesismanagement.user.entity.Admin;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.entity.Student;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import com.glucoze.thesismanagement.council.repository.CouncilMemberRepository;
import com.glucoze.thesismanagement.grading.repository.GradeRepository;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.glucoze.thesismanagement.audit.AuditAction;
import com.glucoze.thesismanagement.audit.AuditEntityType;
import com.glucoze.thesismanagement.audit.AuditLogService;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    private final UserAccountRepository userAccountRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final AdminRepository adminRepository;
    private final RegistrationRepository registrationRepository;
    private final ThesisRepository thesisRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final GradeRepository gradeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserManagementService(UserAccountRepository userAccountRepository,
                                 StudentRepository studentRepository,
                                 LecturerRepository lecturerRepository,
                                 AdminRepository adminRepository,
                                 RegistrationRepository registrationRepository,
                                 ThesisRepository thesisRepository,
                                 CouncilMemberRepository councilMemberRepository,
                                 GradeRepository gradeRepository,
                                 PasswordEncoder passwordEncoder,
                                 AuditLogService auditLogService) {
        this.userAccountRepository = userAccountRepository;
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.adminRepository = adminRepository;
        this.registrationRepository = registrationRepository;
        this.thesisRepository = thesisRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.gradeRepository = gradeRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryView> listAccounts() {
        List<UserAccount> accounts = userAccountRepository.findAll();
        if (accounts.isEmpty()) {
            return List.of();
        }
        List<Long> accountIds = accounts.stream().map(UserAccount::getId).toList();
        Map<Long, String> namesByAccountId = new HashMap<>();
        studentRepository.findByUserAccountIdIn(accountIds)
                .forEach(student -> namesByAccountId.put(student.getUserAccount().getId(), student.getFullName()));
        lecturerRepository.findByUserAccountIdIn(accountIds)
                .forEach(lecturer -> namesByAccountId.put(lecturer.getUserAccount().getId(), lecturer.getFullName()));
        adminRepository.findByUserAccountIdIn(accountIds)
                .forEach(admin -> namesByAccountId.put(admin.getUserAccount().getId(), admin.getFullName()));
        return accounts.stream()
                .map(account -> toSummary(account, namesByAccountId))
                .toList();
    }

    @Transactional(readOnly = true)
    public long countAccounts() {
        return userAccountRepository.count();
    }

    @Transactional
    public void createAccount(AdminUserForm form) {
        String username = form.getUsername().trim();
        if (userAccountRepository.existsByUsername(username)) {
            throw new DomainRuleViolationException("Tên đăng nhập đã được sử dụng");
        }

        String profileCode = validateRoleProfile(form);

        UserAccount account = userAccountRepository.save(
                new UserAccount(username, passwordEncoder.encode(form.getPassword()), form.getRole()));
        String name = form.getFullName().trim();
        switch (form.getRole()) {
            case STUDENT -> createStudent(account, name, profileCode, form);
            case LECTURER -> createLecturer(account, name, profileCode, form);
            case ADMIN -> adminRepository.save(new Admin(account, name));
        }
        auditLogService.append(AuditAction.USER_CREATE, AuditEntityType.USER_ACCOUNT, account.getId(),
                "Đã tạo tài khoản @" + account.getUsername());
    }

    @Transactional(readOnly = true)
    public AdminUserEditForm getAccountForEdit(Long accountId) {
        UserAccount account = getAccount(accountId);
        AdminUserEditForm form = new AdminUserEditForm();
        form.setId(account.getId());
        form.setEnabled(account.isEnabled());
        switch (account.getRole()) {
            case STUDENT -> studentRepository.findByUserAccountUsername(account.getUsername()).ifPresent(student -> {
                form.setFullName(student.getFullName());
                form.setStudentCode(student.getStudentCode());
                form.setClassName(student.getClassName());
            });
            case LECTURER -> lecturerRepository.findByUserAccountUsername(account.getUsername()).ifPresent(lecturer -> {
                form.setFullName(lecturer.getFullName());
                form.setLecturerCode(lecturer.getLecturerCode());
                form.setDepartment(lecturer.getDepartment());
            });
            case ADMIN -> adminRepository.findByUserAccountUsername(account.getUsername())
                    .ifPresent(admin -> form.setFullName(admin.getFullName()));
        }
        return form;
    }

    @Transactional
    public void updateAccount(AdminUserEditForm form, String actorUsername) {
        UserAccount account = getAccount(form.getId());
        if (account.getUsername().equals(actorUsername) && !form.isEnabled()) {
            throw new DomainRuleViolationException("Bạn không thể khóa tài khoản của chính mình");
        }
        boolean enabledChanged = account.isEnabled() != form.isEnabled();
        boolean passwordReset = form.getNewPassword() != null && !form.getNewPassword().isBlank();
        account.setEnabled(form.isEnabled());
        if (passwordReset) {
            account.updatePassword(passwordEncoder.encode(form.getNewPassword()));
        }
        String name = requiredField(form.getFullName(), "Vui lòng nhập họ và tên");
        switch (account.getRole()) {
            case STUDENT -> updateStudentByAccount(account, name, form);
            case LECTURER -> updateLecturerByAccount(account, name, form);
            case ADMIN -> adminRepository.findByUserAccountUsername(account.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ quản trị viên"))
                    .updateProfile(name);
        }
        auditLogService.append(AuditAction.USER_UPDATE, AuditEntityType.USER_ACCOUNT, account.getId(),
                "Đã cập nhật tài khoản @" + account.getUsername());
        if (enabledChanged) {
            auditLogService.append(form.isEnabled() ? AuditAction.USER_ENABLE : AuditAction.USER_DISABLE,
                    AuditEntityType.USER_ACCOUNT, account.getId(),
                    (form.isEnabled() ? "Đã mở khóa tài khoản @" : "Đã khóa tài khoản @") + account.getUsername());
        }
        if (passwordReset) {
            auditLogService.append(AuditAction.USER_PASSWORD_RESET, AuditEntityType.USER_ACCOUNT, account.getId(),
                    "Đã đặt lại mật khẩu cho tài khoản @" + account.getUsername());
        }
    }

    @Transactional
    public void deleteAccount(Long accountId, String actorUsername) {
        UserAccount account = getAccount(accountId);
        if (account.getUsername().equals(actorUsername)) {
            throw new DomainRuleViolationException("Bạn không thể xóa tài khoản của chính mình");
        }
        switch (account.getRole()) {
            case STUDENT -> deleteStudentAccount(account);
            case LECTURER -> deleteLecturerAccount(account);
            case ADMIN -> adminRepository.findByUserAccountUsername(account.getUsername())
                    .ifPresent(adminRepository::delete);
        }
        auditLogService.append(AuditAction.USER_DELETE, AuditEntityType.USER_ACCOUNT, account.getId(),
                "Đã xóa tài khoản @" + account.getUsername());
        userAccountRepository.delete(account);
    }

    private void deleteStudentAccount(UserAccount account) {
        Student student = studentRepository.findByUserAccountUsername(account.getUsername())
                .orElse(null);
        if (student != null && registrationRepository.existsByStudentId(student.getId())) {
            throw new DomainRuleViolationException("Không thể xóa tài khoản sinh viên đang liên kết với đăng ký đề tài");
        }
        if (student != null) {
            studentRepository.delete(student);
        }
    }

    private void deleteLecturerAccount(UserAccount account) {
        Lecturer lecturer = lecturerRepository.findByUserAccountUsername(account.getUsername())
                .orElse(null);
        if (lecturer != null && (thesisRepository.existsBySupervisorId(lecturer.getId())
                || councilMemberRepository.existsByLecturerId(lecturer.getId())
                || gradeRepository.existsByLecturerId(lecturer.getId()))) {
            throw new DomainRuleViolationException("Không thể xóa tài khoản giảng viên đang liên kết với đề tài, hội đồng hoặc điểm chấm");
        }
        if (lecturer != null) {
            lecturerRepository.delete(lecturer);
        }
    }

    private void updateStudentByAccount(UserAccount account, String name, AdminUserEditForm form) {
        Student student = studentRepository.findByUserAccountUsername(account.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ sinh viên"));
        String code = requiredField(form.getStudentCode(), "Vui lòng nhập mã sinh viên");
        ensureStudentCodeAvailable(code, student.getStudentCode());
        student.updateProfile(name, code, trimToNull(form.getClassName()));
    }

    private void updateLecturerByAccount(UserAccount account, String name, AdminUserEditForm form) {
        Lecturer lecturer = lecturerRepository.findByUserAccountUsername(account.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ giảng viên"));
        String code = requiredField(form.getLecturerCode(), "Vui lòng nhập mã giảng viên");
        ensureLecturerCodeAvailable(code, lecturer.getLecturerCode());
        lecturer.updateProfile(name, code, trimToNull(form.getDepartment()));
    }

    private UserAccount getAccount(Long accountId) {
        return userAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }

    private UserSummaryView toSummary(UserAccount account, Map<Long, String> namesByAccountId) {
        String fullName = namesByAccountId.getOrDefault(account.getId(), "-");
        return new UserSummaryView(account.getId(), account.getUsername(), fullName, account.getRole(), account.isEnabled());
    }

    private void createStudent(UserAccount account, String name, String profileCode, AdminUserForm form) {
        studentRepository.save(new Student(account, name, profileCode, trimToNull(form.getClassName())));
    }

    private String validateRoleProfile(AdminUserForm form) {
        return switch (form.getRole()) {
            case STUDENT -> {
                String code = requiredField(form.getStudentCode(), "Vui lòng nhập mã sinh viên");
                if (studentRepository.existsByStudentCode(code)) {
                    throw new DomainRuleViolationException("Mã sinh viên đã được sử dụng");
                }
                yield code;
            }
            case LECTURER -> {
                String code = requiredField(form.getLecturerCode(), "Vui lòng nhập mã giảng viên");
                if (lecturerRepository.existsByLecturerCode(code)) {
                    throw new DomainRuleViolationException("Mã giảng viên đã được sử dụng");
                }
                yield code;
            }
            case ADMIN -> null;
        };
    }

    private void createLecturer(UserAccount account, String name, String profileCode, AdminUserForm form) {
        lecturerRepository.save(new Lecturer(account, name, profileCode, trimToNull(form.getDepartment())));
    }

    private void ensureStudentCodeAvailable(String requestedCode, String currentCode) {
        if (!requestedCode.trim().equals(currentCode) && studentRepository.existsByStudentCode(requestedCode.trim())) {
            throw new DomainRuleViolationException("Mã sinh viên đã được sử dụng");
        }
    }

    private void ensureLecturerCodeAvailable(String requestedCode, String currentCode) {
        if (!requestedCode.trim().equals(currentCode) && lecturerRepository.existsByLecturerCode(requestedCode.trim())) {
            throw new DomainRuleViolationException("Mã giảng viên đã được sử dụng");
        }
    }

    private void requireField(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainRuleViolationException(message);
        }
    }

    private String requiredField(String value, String message) {
        requireField(value, message);
        return value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
