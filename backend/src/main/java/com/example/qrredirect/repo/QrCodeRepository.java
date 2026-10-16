package com.example.qrredirect.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.qrredirect.model.QrCode;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QrCodeRepository extends JpaRepository<QrCode, Long>, JpaSpecificationExecutor<QrCode> {

    Optional<QrCode> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);
}