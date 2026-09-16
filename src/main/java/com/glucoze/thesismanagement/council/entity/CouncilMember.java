package com.glucoze.thesismanagement.council.entity;

import com.glucoze.thesismanagement.common.BaseEntity;
import com.glucoze.thesismanagement.common.enums.CouncilRole;
import com.glucoze.thesismanagement.common.converter.CouncilRoleConverter;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "thanh_vien_hoi_dong", uniqueConstraints = {
        @UniqueConstraint(name = "uk_thanh_vien_hoi_dong_giang_vien", columnNames = {"hoi_dong_id", "giang_vien_id"}),
        @UniqueConstraint(name = "uk_thanh_vien_hoi_dong_vai_tro", columnNames = {"hoi_dong_id", "vai_tro"})
})
public class CouncilMember extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "hoi_dong_id", nullable = false)
    private Council council;

    @ManyToOne(optional = false)
    @JoinColumn(name = "giang_vien_id", nullable = false)
    private Lecturer lecturer;

    @jakarta.persistence.Convert(converter = CouncilRoleConverter.class)
    @Column(name = "vai_tro", nullable = false, length = 20)
    private CouncilRole role;

    protected CouncilMember() {
    }

    public CouncilMember(Council council, Lecturer lecturer, CouncilRole role) {
        this.council = council;
        this.lecturer = lecturer;
        this.role = role;
    }

    public Council getCouncil() {
        return council;
    }

    public Lecturer getLecturer() {
        return lecturer;
    }

    public CouncilRole getRole() {
        return role;
    }
}