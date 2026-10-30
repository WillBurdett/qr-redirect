package com.example.qrredirect.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.qrredirect.model.QrCode;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface QrCodeRepository extends JpaRepository<QrCode, Long>, JpaSpecificationExecutor<QrCode> {

    Optional<QrCode> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Modifying
    @Transactional
    @Query("update QrCode q set q.scanCount = q.scanCount + 1 where q.shortCode = :shortCode and q.active = true")
    int incrementScans(@Param("shortCode") String shortCode);
}