package com.glucoze.thesismanagement.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StudentProfileForm {

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotBlank
    @Size(max = 30)
    private String studentCode;

    @Size(max = 150)
    private String className;

    private String avatarUrl;

    public StudentProfileForm() {
    }

    public StudentProfileForm(String fullName, String studentCode, String className, String avatarUrl) {
        this.fullName = fullName;
        this.studentCode = studentCode;
        this.className = className;
        this.avatarUrl = avatarUrl;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
