package com.glucoze.thesismanagement.user.controller;

import com.glucoze.thesismanagement.common.exception.ResourceNotFoundException;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserAvatarController {

    private final UserAccountRepository userAccountRepository;

    public UserAvatarController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/user/avatar")
    public ResponseEntity<byte[]> avatar(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.notFound().build();
        }

        UserAccount account = userAccountRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (account.getAvatarData() == null || account.getAvatarData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.parseMediaType(
            account.getAvatarContentType() == null ? MediaType.IMAGE_JPEG_VALUE : account.getAvatarContentType());
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff")
            .contentType(mediaType)
            .body(account.getAvatarData());
    }
}
