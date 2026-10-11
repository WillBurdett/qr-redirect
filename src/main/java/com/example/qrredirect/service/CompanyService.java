package com.example.qrredirect.service;

import java.util.List;

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

    public CompanyService(CompanyRepository repo) {
        this.repo = repo;
    }

    public Company create(String rawName) {
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
        Company company = new Company();
        company.setName(name);
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