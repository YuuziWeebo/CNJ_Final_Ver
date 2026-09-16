package com.glucoze.thesismanagement.user.entity;

import com.glucoze.thesismanagement.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "giang_vien")
public class Lecturer extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(name = "ho_ten", nullable = false, length = 150)
    private String fullName;

    @Column(name = "ma_giang_vien", nullable = false, unique = true, length = 30)
    private String lecturerCode;

    @Column(name = "bo_mon", length = 150)
    private String department;

    protected Lecturer() {
    }

    public Lecturer(UserAccount userAccount, String fullName, String lecturerCode, String department) {
        this.userAccount = userAccount;
        this.fullName = fullName;
        this.lecturerCode = lecturerCode;
        this.department = department;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public String getFullName() {
        return fullName;
    }

    public String getLecturerCode() {
        return lecturerCode;
    }

    public String getDepartment() {
        return department;
    }

    public void updateProfile(String fullName, String lecturerCode, String department) {
        this.fullName = fullName;
        this.lecturerCode = lecturerCode;
        this.department = department;
    }
}
