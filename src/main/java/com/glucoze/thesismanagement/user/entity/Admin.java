package com.glucoze.thesismanagement.user.entity;

import com.glucoze.thesismanagement.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "admins")
public class Admin extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(name = "ho_ten", nullable = false, length = 150)
    private String fullName;

    protected Admin() {
    }

    public Admin(UserAccount userAccount, String fullName) {
        this.userAccount = userAccount;
        this.fullName = fullName;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public String getFullName() {
        return fullName;
    }

    public void updateProfile(String fullName) {
        this.fullName = fullName;
    }
}
