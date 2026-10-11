package com.example.qrredirect.web;

import java.time.Instant;

import com.example.qrredirect.model.QrCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request/response shapes for the QR code API. */
public final class CodeDtos {

    private CodeDtos() {}

    public record CreateCodeRequest(@NotNull Long companyId, @NotBlank String targetUrl) {}

    /** Both fields optional: send only what you want to change. */
    public record UpdateCodeRequest(String targetUrl, Boolean active) {}

    public record CodeResponse(
            Long id,
            String shortCode,
            String shortUrl,
            Long companyId,
            String companyName,
            String targetUrl,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        public static CodeResponse of(QrCode qr, String shortUrl) {
            return new CodeResponse(qr.getId(), qr.getShortCode(), shortUrl,
                    qr.getCompany().getId(), qr.getCompany().getName(),
                    qr.getTargetUrl(), qr.isActive(), qr.getCreatedAt(), qr.getUpdatedAt());
        }
    }
}