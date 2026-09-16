package com.glucoze.thesismanagement.grading.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.glucoze.thesismanagement.grading.config.GradingProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GradingCalculatorTest {

    private GradingCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new GradingCalculator(new GradingProperties());
    }

    @Test
    void calculatesConfiguredWeightedScore() {
        assertThat(calculator.calculate(new BigDecimal("8.0"), new BigDecimal("9.0")))
                .isEqualByComparingTo("8.6");
    }

    @Test
    void rejectsMissingRequiredSource() {
        assertThatThrownBy(() -> calculator.calculate(null, new BigDecimal("9.0")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void rejectsOutOfRangeScore() {
        assertThatThrownBy(() -> calculator.calculate(new BigDecimal("10.1"), new BigDecimal("9.0")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 10");
    }

    @Test
    void roundsHalfUpToOneDecimalPlace() {
        assertThat(calculator.calculate(new BigDecimal("8.1"), new BigDecimal("8.2")))
                .isEqualByComparingTo("8.2");
    }

    @Test
    void acceptsZeroAndTenBoundaryScores() {
        assertThat(calculator.calculate(BigDecimal.ZERO, BigDecimal.TEN))
                .isEqualByComparingTo("6.0");
    }

    @Test
    void roundsHalfUpAndDownToExactlyOneDecimal() {
        assertThat(calculator.calculate(new BigDecimal("8.0"), new BigDecimal("8.25")))
                .isEqualByComparingTo("8.2");
        assertThat(calculator.calculate(new BigDecimal("8.0"), new BigDecimal("8.24")))
                .isEqualByComparingTo("8.1");
    }

    @Test
    void rejectsInvalidWeightConfiguration() {
        GradingProperties properties = new GradingProperties();
        properties.setSupervisorWeight(new BigDecimal("0.50"));
        properties.setCouncilWeight(new BigDecimal("0.60"));

        assertThatThrownBy(() -> new GradingCalculator(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sum to 1.00");
    }
}