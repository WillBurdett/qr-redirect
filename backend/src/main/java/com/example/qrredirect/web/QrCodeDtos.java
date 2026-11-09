package com.example.qrredirect.web;

import java.time.Instant;
import java.util.List;

import com.example.qrredirect.model.QrCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request/response shapes for the QR code API. */
public final class QrCodeDtos {

    private QrCodeDtos() {}

    public record CreateQrCodeRequest(@NotNull Long companyId, @NotBlank String targetUrl) {}

    /** Both fields optional: send only what you want to change. */
    public record UpdateCodeRequest(String targetUrl, Boolean active) {}

    public record QrCodeResponse(
            Long id,
            String shortQrId,
            String shortUrl,
            Long companyId,
            String companyName,
            String targetUrl,
            boolean active,
            Long scanCount,
            Instant createdAt,
            Instant updatedAt) {

        public static QrCodeResponse of(QrCode qr, String shortUrl) {
            return new QrCodeResponse(qr.getId(), qr.getShortQrId(), shortUrl,
                    qr.getCompany().getId(), qr.getCompany().getName(),
                    qr.getTargetUrl(), qr.isActive(), qr.getScanCount(), qr.getCreatedAt(), qr.getUpdatedAt());
        }
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {}
}