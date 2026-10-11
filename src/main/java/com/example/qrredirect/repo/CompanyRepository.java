package com.example.qrredirect.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.qrredirect.model.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Company> findAllByOrderByNameAsc();
}