package com.example.qrredirect.web;

import java.time.Instant;

import com.example.qrredirect.model.QrCode;

import jakarta.validation.constraints.NotBlank;

/** Request/response shapes for the management API. */
public final class CodeDtos {

    private CodeDtos() {}

    public record CreateCodeRequest(@NotBlank String targetUrl) {}

    /** Both fields optional: send only what you want to change. */
    public record UpdateCodeRequest(String targetUrl, Boolean active) {}

    public record CodeResponse(
            Long id,
            String shortCode,
            String shortUrl,
            String targetUrl,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        public static CodeResponse of(QrCode qr, String shortUrl) {
            return new CodeResponse(qr.getId(), qr.getShortCode(), shortUrl, qr.getTargetUrl(),
                    qr.isActive(), qr.getCreatedAt(), qr.getUpdatedAt());
        }
    }
}
