package com.glucoze.thesismanagement.user.service;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.common.exception.ResourceNotFoundException;
import com.glucoze.thesismanagement.user.dto.AdminProfileForm;
import com.glucoze.thesismanagement.user.dto.LecturerProfileForm;
import com.glucoze.thesismanagement.user.dto.StudentProfileForm;
import com.glucoze.thesismanagement.user.entity.Admin;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.entity.Student;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Owns self-service profile reads, edits and avatar updates. */
@Service
public class UserProfileService {

    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final AdminRepository adminRepository;
    private final UserAvatarStorage userAvatarStorage;

    public UserProfileService(StudentRepository studentRepository,
                              LecturerRepository lecturerRepository,
                              AdminRepository adminRepository,
                              UserAvatarStorage userAvatarStorage) {
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.adminRepository = adminRepository;
        this.userAvatarStorage = userAvatarStorage;
    }

    @Transactional(readOnly = true)
    public StudentProfileForm getStudentProfile(String username) {
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ sinh viên"));
        return new StudentProfileForm(student.getFullName(), student.getStudentCode(), student.getClassName(),
                avatarUrl(student.getUserAccount()));
    }

    @Transactional
    public void updateStudentProfileAndAvatar(String username, StudentProfileForm form, MultipartFile avatar) {
        updateStudentProfile(username, form);
        updateStudentAvatar(username, avatar);
    }

    @Transactional
    public void updateStudentProfile(String username, StudentProfileForm form) {
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ sinh viên"));
        ensureStudentCodeAvailable(form.getStudentCode(), student.getStudentCode());
        student.updateProfile(form.getFullName().trim(), form.getStudentCode().trim(), trimToNull(form.getClassName()));
    }

    @Transactional(readOnly = true)
    public LecturerProfileForm getLecturerProfile(String username) {
        Lecturer lecturer = lecturerRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ giảng viên"));
        return new LecturerProfileForm(lecturer.getFullName(), lecturer.getLecturerCode(), lecturer.getDepartment(),
                avatarUrl(lecturer.getUserAccount()));
    }

    @Transactional
    public void updateLecturerProfileAndAvatar(String username, LecturerProfileForm form, MultipartFile avatar) {
        updateLecturerProfile(username, form);
        updateLecturerAvatar(username, avatar);
    }

    @Transactional
    public void updateLecturerProfile(String username, LecturerProfileForm form) {
        Lecturer lecturer = lecturerRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ giảng viên"));
        ensureLecturerCodeAvailable(form.getLecturerCode(), lecturer.getLecturerCode());
        lecturer.updateProfile(form.getFullName().trim(), form.getLecturerCode().trim(), trimToNull(form.getDepartment()));
    }

    @Transactional(readOnly = true)
    public AdminProfileForm getAdminProfile(String username) {
        Admin admin = adminRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ quản trị viên"));
        return new AdminProfileForm(admin.getFullName(), avatarUrl(admin.getUserAccount()));
    }

    @Transactional
    public void updateAdminProfile(String username, AdminProfileForm form) {
        Admin admin = adminRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ quản trị viên"));
        admin.updateProfile(form.getFullName().trim());
    }

    @Transactional
    public void updateAdminProfileAndAvatar(String username, AdminProfileForm form, MultipartFile avatar) {
        updateAdminProfile(username, form);
        updateAdminAvatar(username, avatar);
    }

    @Transactional
    public void updateAdminAvatar(String username, MultipartFile avatar) {
        Admin admin = adminRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ quản trị viên"));
        updateAvatar(admin.getUserAccount(), avatar);
    }

    @Transactional
    public void updateStudentAvatar(String username, MultipartFile avatar) {
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ sinh viên"));
        updateAvatar(student.getUserAccount(), avatar);
    }

    @Transactional
    public void updateLecturerAvatar(String username, MultipartFile avatar) {
        Lecturer lecturer = lecturerRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ giảng viên"));
        updateAvatar(lecturer.getUserAccount(), avatar);
    }

    private void updateAvatar(UserAccount account, MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return;
        }
        try {
            userAvatarStorage.validate(avatar);
            account.updateAvatar(avatar.getBytes(), avatar.getContentType());
        } catch (IOException exception) {
            throw new DomainRuleViolationException("Không thể đọc ảnh đại diện", exception);
        }
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

    private String avatarUrl(UserAccount account) {
        return account.getAvatarData() != null && account.getAvatarData().length > 0
                ? "/user/avatar?v=" + account.getUpdatedAt()
                : null;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
