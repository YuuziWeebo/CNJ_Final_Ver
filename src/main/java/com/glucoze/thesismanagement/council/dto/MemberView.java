package com.glucoze.thesismanagement.council.dto;

import com.glucoze.thesismanagement.common.enums.CouncilRole;

public record MemberView(Long id, String fullName, String lecturerCode, CouncilRole role) {
}