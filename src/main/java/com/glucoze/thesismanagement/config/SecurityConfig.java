package com.glucoze.thesismanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * Permission matrix:
     * <ul>
     *     <li>Anonymous: home, login, CSS, and JavaScript resources.</li>
     *     <li>STUDENT: student endpoints, plus the public endpoints.</li>
     *     <li>LECTURER: lecturer endpoints, plus the public endpoints.</li>
     *     <li>ADMIN: admin endpoints, plus the public endpoints.</li>
     *     <li>Any authenticated role: other endpoints until a feature-specific rule exists.</li>
     * </ul>
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/home", "/login", "/css/**", "/js/**", "/img/**").permitAll()
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        .requestMatchers("/lecturer/**").hasRole("LECTURER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(formLogin -> formLogin
                    .loginPage("/login")
                    .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/home").permitAll());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
