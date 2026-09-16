package com.glucoze.thesismanagement.council.dto;

import com.glucoze.thesismanagement.common.enums.CouncilRole;
import jakarta.validation.constraints.NotNull;

public class MemberForm {

    @NotNull
    private Long lecturerId;

    @NotNull
    private CouncilRole role;

    public Long getLecturerId() { return lecturerId; }
    public void setLecturerId(Long lecturerId) { this.lecturerId = lecturerId; }
    public CouncilRole getRole() { return role; }
    public void setRole(CouncilRole role) { this.role = role; }
}