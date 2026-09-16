package com.glucoze.thesismanagement.user.entity;

import com.glucoze.thesismanagement.common.BaseEntity;
import com.glucoze.thesismanagement.common.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_accounts")
public class UserAccount extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "avatar_data", columnDefinition = "LONGBLOB")
    private byte[] avatarData;

    @Column(name = "avatar_content_type", length = 50)
    private String avatarContentType;

    protected UserAccount() {
    }

    public UserAccount(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public byte[] getAvatarData() {
        return avatarData;
    }

    public String getAvatarContentType() {
        return avatarContentType;
    }

    public void updateAvatar(byte[] avatarData, String avatarContentType) {
        this.avatarData = avatarData;
        this.avatarContentType = avatarContentType;
    }
}