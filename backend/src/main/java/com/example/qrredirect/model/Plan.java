package com.example.qrredirect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "plans")
public class Plan {

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getMonthlyPricePounds() {
        return monthlyPricePounds;
    }

    public void setMonthlyPricePounds(BigDecimal monthlyPricePounds) {
        this.monthlyPricePounds = monthlyPricePounds;
    }

    public Integer getMaxActiveCodes() {
        return maxActiveCodes;
    }

    public void setMaxActiveCodes(Integer maxActiveCodes) {
        this.maxActiveCodes = maxActiveCodes;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    @Id
    private String code;   // 'PILOT', 'STARTER', ...

    @Column(name = "monthly_price_pounds")
    private BigDecimal monthlyPricePounds;

    @Column(name = "max_active_codes")
    private Integer maxActiveCodes;

    @Column(name = "duration_days")
    private Integer durationDays;

    // getters
}