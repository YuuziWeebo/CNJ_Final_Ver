package com.glucoze.thesismanagement.council.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

public class CouncilForm {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 20)
    private String academicYear;

    @NotBlank
    @Size(max = 20)
    private String semester;

    @NotNull(groups = Create.class)
    private Long chairId;

    @NotNull(groups = Create.class)
    private Long secretaryId;

    @NotNull(groups = Create.class)
    private Long memberId;

    public interface Create extends Default {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public Long getChairId() { return chairId; }
    public void setChairId(Long chairId) { this.chairId = chairId; }
    public Long getSecretaryId() { return secretaryId; }
    public void setSecretaryId(Long secretaryId) { this.secretaryId = secretaryId; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
}
