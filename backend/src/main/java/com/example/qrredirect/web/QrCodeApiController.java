package com.example.qrredirect.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.example.qrredirect.model.QrCode;
import com.example.qrredirect.service.QrCodeService;
import com.example.qrredirect.service.QrImageService;
import com.example.qrredirect.web.CodeDtos.CodeResponse;
import com.example.qrredirect.web.CodeDtos.CreateCodeRequest;
import com.example.qrredirect.web.CodeDtos.UpdateCodeRequest;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import jakarta.validation.Valid;

/** Operator-only management of QR codes. */
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
    public CodeResponse create(@Valid @RequestBody CreateCodeRequest request) {
        return toResponse(service.create(request.companyId(), request.targetUrl()));
    }

    @GetMapping
    public CodeDtos.PageResponse<CodeResponse> list(@RequestParam(required = false) Long companyId,
                                                    @RequestParam(required = false) Long id,
                                                    @RequestParam(required = false) Boolean active,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        Page<QrCode> result = service.search(companyId, id, active, page, size);
        return new CodeDtos.PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/{id}")
    public CodeResponse get(@PathVariable Long id) {
        return toResponse(service.get(id));
    }

    /** Reassign where a code points, or switch it off with {"active": false}. */
    @PutMapping("/{id}")
    public CodeResponse update(@PathVariable Long id, @RequestBody UpdateCodeRequest request) {
        return toResponse(service.update(id, request.targetUrl(), request.active()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /**
     * The printable QR image. format=png|svg, size (px, PNG only) 64-4096, ec=L|M|Q|H.
     * Use SVG for print. Use Q or H if you plan to overlay a logo.
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<?> image(@PathVariable Long id,
                                   @RequestParam(defaultValue = "png") String format,
                                   @RequestParam(defaultValue = "2048") int size,
                                   @RequestParam(defaultValue = "Q") String ec,
                                   @RequestParam(defaultValue = "false") boolean label) {
        QrCode qr = service.get(id);
        String content = images.shortUrl(qr.getShortCode());
        String labelText = label ? String.valueOf(qr.getId()) : null;
        ErrorCorrectionLevel level = parseLevel(ec);

        return switch (format.toLowerCase(Locale.ROOT)) {
            case "png" -> {
                if (size < 64 || size > 4096) {
                    throw bad("size must be between 64 and 4096");
                }
                yield ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + qr.getShortCode() + ".png\"")
                        .body(images.png(content, size, level, labelText));
            }
            case "svg" -> ResponseEntity.ok()
                    .contentType(SVG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + qr.getShortCode() + ".svg\"")
                    .body(images.svg(content, level, labelText).getBytes(StandardCharsets.UTF_8));
            default -> throw bad("format must be png or svg");
        };
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id,
                                           @RequestParam(defaultValue = "2048") int size,
                                           @RequestParam(defaultValue = "Q") String ec) throws IOException {
        if (size < 64 || size > 4096) {
            throw bad("size must be between 64 and 4096");
        }
        QrCode qr = service.get(id);
        String code = qr.getShortCode();
        String content = images.shortUrl(code);
        String labelText = String.valueOf(qr.getId());
        ErrorCorrectionLevel level = parseLevel(ec);

        byte[] png = images.png(content, size, level, labelText);
        byte[] svg = images.svg(content, level, labelText).getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(code + ".png"));
            zip.write(png);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(code + ".svg"));
            zip.write(svg);
            zip.closeEntry();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"qr-" + code + ".zip\"")
                .body(out.toByteArray());
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