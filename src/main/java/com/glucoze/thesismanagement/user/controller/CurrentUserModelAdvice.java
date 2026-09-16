package com.glucoze.thesismanagement.user.controller;

import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserModelAdvice {

    private final UserAccountRepository userAccountRepository;
    private final AdminRepository adminRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;

    public CurrentUserModelAdvice(UserAccountRepository userAccountRepository,
                                  AdminRepository adminRepository,
                                  LecturerRepository lecturerRepository,
                                  StudentRepository studentRepository) {
        this.userAccountRepository = userAccountRepository;
        this.adminRepository = adminRepository;
        this.lecturerRepository = lecturerRepository;
        this.studentRepository = studentRepository;
    }

    @ModelAttribute
    public void currentUserMetadata(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            model.addAttribute("currentUserName", null);
            model.addAttribute("currentAvatarUrl", null);
            return;
        }
        String username = userDetails.getUsername();
        UserAccount account = userAccountRepository.findByUsername(username).orElse(null);
        if (account == null) {
            model.addAttribute("currentUserName", username);
            model.addAttribute("currentAvatarUrl", null);
            return;
        }
        model.addAttribute("currentUserName", displayName(account));
        model.addAttribute("currentAvatarUrl", avatarUrl(account));
    }

    private String displayName(UserAccount account) {
        return switch (account.getRole()) {
            case STUDENT -> studentRepository.findByUserAccountUsername(account.getUsername())
                    .map(student -> student.getFullName()).orElse(account.getUsername());
            case LECTURER -> lecturerRepository.findByUserAccountUsername(account.getUsername())
                    .map(lecturer -> lecturer.getFullName()).orElse(account.getUsername());
            case ADMIN -> adminRepository.findByUserAccountUsername(account.getUsername())
                    .map(admin -> admin.getFullName()).orElse(account.getUsername());
        };
    }

    private String avatarUrl(UserAccount account) {
        return account.getAvatarData() != null && account.getAvatarData().length > 0
                ? "/user/avatar?v=" + account.getUpdatedAt()
                : null;
    }
}
