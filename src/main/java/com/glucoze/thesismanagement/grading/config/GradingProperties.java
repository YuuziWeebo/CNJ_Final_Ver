package com.glucoze.thesismanagement.grading.config;

import java.math.BigDecimal;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "grading")
@Validated
public class GradingProperties {

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal supervisorWeight = new BigDecimal("0.40");

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal councilWeight = new BigDecimal("0.60");

    @AssertTrue(message = "Tổng trọng số điểm phải bằng 1.00")
    public boolean isWeightSumValid() {
        return supervisorWeight != null && councilWeight != null
                && supervisorWeight.add(councilWeight).compareTo(BigDecimal.ONE) == 0;
    }

    public BigDecimal getSupervisorWeight() { return supervisorWeight; }
    public void setSupervisorWeight(BigDecimal supervisorWeight) { this.supervisorWeight = supervisorWeight; }
    public BigDecimal getCouncilWeight() { return councilWeight; }
    public void setCouncilWeight(BigDecimal councilWeight) { this.councilWeight = councilWeight; }
}