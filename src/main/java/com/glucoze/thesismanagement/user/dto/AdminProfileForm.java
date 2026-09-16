package com.glucoze.thesismanagement.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminProfileForm {

    @NotBlank
    @Size(max = 150)
    private String fullName;

    private String avatarUrl;

    public AdminProfileForm() {
    }

    public AdminProfileForm(String fullName) {
        this.fullName = fullName;
    }

    public AdminProfileForm(String fullName, String avatarUrl) {
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}