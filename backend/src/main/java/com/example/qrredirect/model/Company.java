package com.example.qrredirect.model;

import java.time.Instant;

import jakarta.persistence.*;

/** A customer company that owns QR codes. Operators manage these; companies don't log in. */
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private PlanType planType = PlanType.PILOT;

    @Column(name = "plan_started_at")
    private Instant planStartedAt;

    @Column(name = "plan_expires_at")
    private Instant planExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(PlanType planType) {
        this.planType = planType;
    }

    public Instant getPlanStartedAt() {
        return planStartedAt;
    }

    public void setPlanStartedAt(Instant planStartedAt) {
        this.planStartedAt = planStartedAt;
    }

    public Instant getPlanExpiresAt() {
        return planExpiresAt;
    }

    public void setPlanExpiresAt(Instant planExpiresAt) {
        this.planExpiresAt = planExpiresAt;
    }


    public Instant getCreatedAt() { return createdAt; }
}