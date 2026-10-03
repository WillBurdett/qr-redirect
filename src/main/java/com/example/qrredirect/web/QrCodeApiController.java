package com.example.qrredirect.web;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.qrredirect.model.QrCode;
import com.example.qrredirect.service.QrCodeService;
import com.example.qrredirect.service.QrImageService;
import com.example.qrredirect.web.CodeDtos.CodeResponse;
import com.example.qrredirect.web.CodeDtos.CreateCodeRequest;
import com.example.qrredirect.web.CodeDtos.UpdateCodeRequest;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import jakarta.validation.Valid;

/** Management API. Every call is scoped to the company that owns the API key. */
@RestController
@RequestMapping("/api/codes")
public class QrCodeApiController {

    private static final MediaType SVG = MediaType.parseMediaType("image/svg+xml");

    private final QrCodeService service;
    private final QrImageService images;

    public QrCodeApiController(QrCodeService service, QrImageService images) {
        this.service = service;
        this.images = images;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CodeResponse create(@RequestAttribute(ApiKeyFilter.COMPANY_ATTRIBUTE) String companyId,
                               @Valid @RequestBody CreateCodeRequest request) {
        return toResponse(service.create(companyId, request.targetUrl()));
    }

    @GetMapping
    public List<CodeResponse> list(@RequestAttribute(ApiKeyFilter.COMPANY_ATTRIBUTE) String companyId) {
        return service.list(companyId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CodeResponse get(@RequestAttribute(ApiKeyFilter.COMPANY_ATTRIBUTE) String companyId,
                            @PathVariable Long id) {
        return toResponse(service.get(companyId, id));
    }

    /** Reassign where a code points, or switch it off with {"active": false}. */
    @PutMapping("/{id}")
    public CodeResponse update(@RequestAttribute(ApiKeyFilter.COMPANY_ATTRIBUTE) String companyId,
                               @PathVariable Long id,
                               @RequestBody UpdateCodeRequest request) {
        return toResponse(service.update(companyId, id, request.targetUrl(), request.active()));
    }

    /**
     * The printable QR image. format=png|svg, size (px, PNG only) 64-4096, ec=L|M|Q|H.
     * Use SVG for print. Use Q or H if you plan to overlay a logo.
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<?> image(@RequestAttribute(ApiKeyFilter.COMPANY_ATTRIBUTE) String companyId,
                                   @PathVariable Long id,
                                   @RequestParam(defaultValue = "png") String format,
                                   @RequestParam(defaultValue = "512") int size,
                                   @RequestParam(defaultValue = "M") String ec) {
        QrCode qr = service.get(companyId, id);
        String content = images.shortUrl(qr.getShortCode());
        ErrorCorrectionLevel level = parseLevel(ec);

        return switch (format.toLowerCase(Locale.ROOT)) {
            case "png" -> {
                if (size < 64 || size > 4096) {
                    throw bad("size must be between 64 and 4096");
                }
                yield ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + qr.getShortCode() + ".png\"")
                        .body(images.png(content, size, level));
            }
            case "svg" -> ResponseEntity.ok()
                    .contentType(SVG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + qr.getShortCode() + ".svg\"")
                    .body(images.svg(content, level).getBytes(StandardCharsets.UTF_8));
            default -> throw bad("format must be png or svg");
        };
    }

    private CodeResponse toResponse(QrCode qr) {
        return CodeResponse.of(qr, images.shortUrl(qr.getShortCode()));
    }

    private static ErrorCorrectionLevel parseLevel(String ec) {
        try {
            return ErrorCorrectionLevel.valueOf(ec.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw bad("ec must be one of L, M, Q, H");
        }
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
