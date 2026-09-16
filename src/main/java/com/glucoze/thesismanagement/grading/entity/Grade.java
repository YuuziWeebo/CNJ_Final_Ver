package com.glucoze.thesismanagement.grading.entity;

import com.glucoze.thesismanagement.common.BaseEntity;
import com.glucoze.thesismanagement.common.enums.ScoreSource;
import com.glucoze.thesismanagement.common.converter.ScoreSourceConverter;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Entity
@Table(name = "diem_cham", uniqueConstraints = {
        @UniqueConstraint(name = "uk_diem_cham_lich_nguon_giang_vien", columnNames = {"lich_bao_ve_id", "nguon_diem", "giang_vien_id"})
})
public class Grade extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "lich_bao_ve_id", nullable = false)
    private DefenseSchedule defenseSchedule;

    @ManyToOne(optional = false)
    @JoinColumn(name = "giang_vien_id", nullable = false)
    private Lecturer lecturer;

    @jakarta.persistence.Convert(converter = ScoreSourceConverter.class)
    @Column(name = "nguon_diem", nullable = false, length = 20)
    private ScoreSource scoreSource;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    @Column(name = "diem", nullable = false, precision = 3, scale = 1)
    private BigDecimal score;

    protected Grade() {
    }

    public Grade(DefenseSchedule defenseSchedule, Lecturer lecturer, ScoreSource scoreSource, BigDecimal score) {
        this.defenseSchedule = defenseSchedule;
        this.lecturer = lecturer;
        this.scoreSource = scoreSource;
        this.score = score;
    }

    public DefenseSchedule getDefenseSchedule() {
        return defenseSchedule;
    }

    public Lecturer getLecturer() {
        return lecturer;
    }

    public ScoreSource getScoreSource() {
        return scoreSource;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void updateScore(BigDecimal score) {
        this.score = score;
    }
}
