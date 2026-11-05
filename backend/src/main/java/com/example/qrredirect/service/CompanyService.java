package com.example.qrredirect.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.example.qrredirect.model.Plan;
import com.example.qrredirect.model.PlanType;
import com.example.qrredirect.repo.PlanRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.qrredirect.model.Company;
import com.example.qrredirect.repo.CompanyRepository;

@Service
public class CompanyService {

    private static final int MAX_NAME_LENGTH = 128;

    private final CompanyRepository repo;
    private final PlanRepository planRepo;

    public CompanyService(CompanyRepository repo, PlanRepository planRepo) {
        this.repo = repo;
        this.planRepo = planRepo;
    }

    public Company create(String rawName, String planType) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        if (repo.existsByNameIgnoreCase(name)) {
            throw duplicate();
        }

        Plan currentPlan = planRepo.findByCode(planType)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown plan: " + planType));

        Company company = new Company();
        company.setName(name);
        company.setPlanType(PlanType.valueOf(planType));
        company.setPlanStartedAt(Instant.now());

        Instant now = Instant.now();
        company.setPlanStartedAt(now);
        if (currentPlan.getDurationDays() != null) {
            company.setPlanExpiresAt(now.plus(currentPlan.getDurationDays(), ChronoUnit.DAYS));
        }

        try {
            return repo.saveAndFlush(company);
        } catch (DataIntegrityViolationException e) {
            throw duplicate();
        }
    }

    public List<Company> list() {
        return repo.findAllByOrderByNameAsc();
    }

    public Company get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    private static ResponseStatusException duplicate() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "A company with that name already exists");
    }
}