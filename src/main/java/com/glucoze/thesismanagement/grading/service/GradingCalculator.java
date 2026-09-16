package com.glucoze.thesismanagement.grading.service;

import com.glucoze.thesismanagement.grading.config.GradingProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class GradingCalculator {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = BigDecimal.TEN;
    private static final int FINAL_SCALE = 1;

    private final GradingProperties properties;

    public GradingCalculator(GradingProperties properties) {
        this.properties = properties;
        if (!properties.isWeightSumValid()
                || properties.getSupervisorWeight().compareTo(MIN_SCORE) < 0
                || properties.getCouncilWeight().compareTo(MIN_SCORE) < 0
                || properties.getSupervisorWeight().compareTo(BigDecimal.ONE) > 0
                || properties.getCouncilWeight().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Trọng số điểm phải nằm trong khoảng 0 đến 1 và có tổng bằng 1.00 (sum to 1.00)");
        }
    }

    public BigDecimal calculate(BigDecimal supervisorScore, BigDecimal councilScore) {
        requireScore(supervisorScore, "Thiếu điểm GVHD (missing supervisor score)");
        requireScore(councilScore, "Thiếu điểm hội đồng (missing council score)");
        validateScore(supervisorScore, "Điểm GVHD");
        validateScore(councilScore, "Điểm hội đồng");
        return supervisorScore.multiply(properties.getSupervisorWeight())
                .add(councilScore.multiply(properties.getCouncilWeight()))
                .setScale(FINAL_SCALE, RoundingMode.HALF_UP);
    }

    public void validateScore(BigDecimal score, String label) {
        requireScore(score, label + " đang thiếu");
        validateRange(score, label);
    }

    private void requireScore(BigDecimal score, String message) {
        if (score == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateRange(BigDecimal score, String label) {
        if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new IllegalArgumentException(label + " phải nằm trong khoảng 0 đến 10 (between 0 and 10)");
        }
    }
}