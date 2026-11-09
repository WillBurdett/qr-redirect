package com.example.qrredirect.repo;

import com.example.qrredirect.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface PlanRepository extends JpaRepository<Plan, String> {
    Optional<Plan> findByPlanType(String planType);
}