package com.glucoze.thesismanagement.user.dto;

import com.glucoze.thesismanagement.common.enums.Role;

public record UserSummaryView(Long id, String username, String fullName, Role role, boolean enabled) {
}