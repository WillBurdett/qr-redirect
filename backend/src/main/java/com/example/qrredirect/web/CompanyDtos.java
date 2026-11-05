package com.example.qrredirect.web;

import java.time.Instant;

import com.example.qrredirect.model.Company;

import com.example.qrredirect.model.PlanType;
import jakarta.validation.constraints.NotBlank;

/** Request/response shapes for the companies API. */
public final class CompanyDtos {

    private CompanyDtos() {}

    public record CreateCompanyRequest(@NotBlank String name, String planType) {}

    public record CompanyResponse(Long id, String name, PlanType planType, Instant planStartedAt, Instant planExpiresAt, Instant createdAt) {

        public static CompanyResponse of(Company company) {
            return new CompanyResponse(company.getId(), company.getName(), company.getPlanType(), company.getPlanStartedAt(), company.getPlanExpiresAt(), company.getCreatedAt());
        }
    }
}