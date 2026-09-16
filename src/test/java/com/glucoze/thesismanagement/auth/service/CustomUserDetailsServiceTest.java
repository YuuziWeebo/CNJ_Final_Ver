package com.glucoze.thesismanagement.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Test
    void loadsEncodedPasswordAndRoleFromAccount() {
        UserAccount account = new UserAccount("student01", "{bcrypt}encoded-hash", Role.STUDENT);
        when(userAccountRepository.findByUsername("student01")).thenReturn(Optional.of(account));

        var userDetails = new CustomUserDetailsService(userAccountRepository).loadUserByUsername("student01");

        assertThat(userDetails.getUsername()).isEqualTo("student01");
        assertThat(userDetails.getPassword()).isEqualTo("{bcrypt}encoded-hash");
        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_STUDENT");
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void rejectsUnknownUsername() {
        when(userAccountRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CustomUserDetailsService(userAccountRepository)
                .loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}