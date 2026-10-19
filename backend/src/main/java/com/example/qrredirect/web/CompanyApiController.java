package com.example.qrredirect.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.qrredirect.service.CompanyService;
import com.example.qrredirect.web.CompanyDtos.CompanyResponse;
import com.example.qrredirect.web.CompanyDtos.CreateCompanyRequest;

import jakarta.validation.Valid;

/** Operator-only management of companies. */
@RestController
@RequestMapping("/api/companies")
public class CompanyApiController {

    private final CompanyService service;

    public CompanyApiController(CompanyService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(@Valid @RequestBody CreateCompanyRequest request) {
        return CompanyResponse.of(service.create(request.name()));
    }

    @GetMapping
    public List<CompanyResponse> list() {
        return service.list().stream().map(CompanyResponse::of).toList();
    }

    @GetMapping("/{id}")
    public CompanyResponse get(@PathVariable Long id) {
        return CompanyResponse.of(service.get(id));
    }
}