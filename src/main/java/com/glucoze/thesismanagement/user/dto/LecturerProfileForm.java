package com.glucoze.thesismanagement.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LecturerProfileForm {

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotBlank
    @Size(max = 30)
    private String lecturerCode;

    @Size(max = 150)
    private String department;

    private String avatarUrl;

    public LecturerProfileForm() {
    }

    public LecturerProfileForm(String fullName, String lecturerCode, String department, String avatarUrl) {
        this.fullName = fullName;
        this.lecturerCode = lecturerCode;
        this.department = department;
        this.avatarUrl = avatarUrl;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getLecturerCode() {
        return lecturerCode;
    }

    public void setLecturerCode(String lecturerCode) {
        this.lecturerCode = lecturerCode;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
