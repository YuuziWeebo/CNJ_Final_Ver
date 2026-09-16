package com.glucoze.thesismanagement.grading.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ScoreForm {

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private BigDecimal score;

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }
}
