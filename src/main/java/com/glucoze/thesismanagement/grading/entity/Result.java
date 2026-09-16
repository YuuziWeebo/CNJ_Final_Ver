package com.glucoze.thesismanagement.grading.entity;

import com.glucoze.thesismanagement.common.BaseEntity;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "ket_qua")
public class Result extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "lich_bao_ve_id", nullable = false, unique = true)
    private DefenseSchedule defenseSchedule;

    @Column(name = "diem_cuoi_cung", nullable = false, precision = 3, scale = 1)
    private BigDecimal finalScore;

    @Column(nullable = false)
    private boolean published;

    protected Result() {
    }

    public Result(DefenseSchedule defenseSchedule, BigDecimal finalScore) {
        this.defenseSchedule = defenseSchedule;
        this.finalScore = finalScore;
    }

    public DefenseSchedule getDefenseSchedule() { return defenseSchedule; }
    public BigDecimal getFinalScore() { return finalScore; }
    public boolean isPublished() { return published; }

    public void publish() {
        this.published = true;
    }
}
