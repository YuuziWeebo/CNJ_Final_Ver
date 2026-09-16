package com.glucoze.thesismanagement.user.dto;

import jakarta.validation.constraints.Size;

public class AdminUserEditForm {

    private Long id;

    @Size(max = 150)
    private String fullName;

    @Size(min = 8, max = 100)
    private String newPassword;

    private boolean enabled;

    @Size(max = 30)
    private String studentCode;

    @Size(max = 30)
    private String lecturerCode;

    @Size(max = 150)
    private String className;

    @Size(max = 150)
    private String department;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getLecturerCode() {
        return lecturerCode;
    }

    public void setLecturerCode(String lecturerCode) {
        this.lecturerCode = lecturerCode;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}