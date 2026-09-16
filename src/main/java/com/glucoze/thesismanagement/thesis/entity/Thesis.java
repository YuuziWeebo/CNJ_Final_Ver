package com.glucoze.thesismanagement.thesis.entity;

import com.glucoze.thesismanagement.common.BaseEntity;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "de_tai")
public class Thesis extends BaseEntity {

    @Column(name = "ten_de_tai", nullable = false, length = 200)
    private String title;

    @Column(name = "mo_ta", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "nam_hoc", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "hoc_ky", nullable = false, length = 20)
    private String semester;

    @ManyToOne(optional = false)
    @JoinColumn(name = "giang_vien_huong_dan_id", nullable = false)
    private Lecturer supervisor;

    protected Thesis() {
    }

    public Thesis(String title, String description, String academicYear, String semester, Lecturer supervisor) {
        this.title = title;
        this.description = description;
        this.academicYear = academicYear;
        this.semester = semester;
        this.supervisor = supervisor;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public String getSemester() {
        return semester;
    }

    public Lecturer getSupervisor() {
        return supervisor;
    }

    public void updateDetails(String title, String description, String academicYear, String semester) {
        this.title = title;
        this.description = description;
        this.academicYear = academicYear;
        this.semester = semester;
    }
}
