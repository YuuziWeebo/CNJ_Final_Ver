package com.glucoze.thesismanagement.user.entity;

import com.glucoze.thesismanagement.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sinh_vien")
public class Student extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(name = "ho_ten", nullable = false, length = 150)
    private String fullName;

    @Column(name = "ma_sinh_vien", nullable = false, unique = true, length = 30)
    private String studentCode;

    @Column(name = "lop", length = 150)
    private String className;

    protected Student() {
    }

    public Student(UserAccount userAccount, String fullName, String studentCode, String className) {
        this.userAccount = userAccount;
        this.fullName = fullName;
        this.studentCode = studentCode;
        this.className = className;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public String getFullName() {
        return fullName;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public String getClassName() {
        return className;
    }

    public void updateProfile(String fullName, String studentCode, String className) {
        this.fullName = fullName;
        this.studentCode = studentCode;
        this.className = className;
    }
}
