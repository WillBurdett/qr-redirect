package com.example.qrredirect.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.qrredirect.model.QrCode;

public interface QrCodeRepository extends JpaRepository<QrCode, Long> {

    Optional<QrCode> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<QrCode> findByCompanyIdOrderByCreatedAtDesc(String companyId);

    Optional<QrCode> findByIdAndCompanyId(Long id, String companyId);
}
