package com.example.qrredirect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    private String planType;

    @Column(name = "monthly_price_pounds")
    private BigDecimal monthlyPricePounds;

    @Column(name = "max_active_codes")
    private Integer maxActiveCodes;

    @Column(name = "duration_days")
    private Integer durationDays;


    public String getPlanType() {
        return planType;
    }

    public void setCode(String code) {
        this.planType = planType;
    }

    public Integer getDurationDays() {
        return durationDays;
    }
}