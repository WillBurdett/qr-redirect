package com.example.qrredirect.web;

import java.time.Instant;

import com.example.qrredirect.model.Company;

import jakarta.validation.constraints.NotBlank;

/** Request/response shapes for the companies API. */
public final class CompanyDtos {

    private CompanyDtos() {}

    public record CreateCompanyRequest(@NotBlank String name) {}

    public record CompanyResponse(Long id, String name, Instant createdAt) {

        public static CompanyResponse of(Company company) {
            return new CompanyResponse(company.getId(), company.getName(), company.getCreatedAt());
        }
    }
}