package com.glucoze.thesismanagement.thesis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ThesisForm {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    @Size(max = 20)
    private String academicYear;

    @NotBlank
    @Size(max = 20)
    private String semester;

    public ThesisForm() {
    }

    public ThesisForm(String title, String description, String academicYear, String semester) {
        this.title = title;
        this.description = description;
        this.academicYear = academicYear;
        this.semester = semester;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }
}