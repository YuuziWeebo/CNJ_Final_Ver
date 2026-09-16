package com.glucoze.thesismanagement.council.entity;

import com.glucoze.thesismanagement.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "hoi_dong")
public class Council extends BaseEntity {

    @Column(name = "ten_hoi_dong", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "nam_hoc", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "hoc_ky", nullable = false, length = 20)
    private String semester;

    protected Council() {
    }

    public Council(String name, String academicYear, String semester) {
        this.name = name;
        this.academicYear = academicYear;
        this.semester = semester;
    }

    public String getName() {
        return name;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public String getSemester() {
        return semester;
    }

    public void updateDetails(String name, String academicYear, String semester) {
        this.name = name;
        this.academicYear = academicYear;
        this.semester = semester;
    }
}
