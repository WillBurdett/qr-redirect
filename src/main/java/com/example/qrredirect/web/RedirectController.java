package com.example.qrredirect.web;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.qrredirect.service.QrCodeService;

/** The public endpoint every printed QR code points at. */
@RestController
public class RedirectController {

    private static final Pattern CODE_FORMAT = Pattern.compile("[0-9A-Za-z]{4,16}");

    private static final String NOT_FOUND_PAGE = """
            <!doctype html>
            <html lang="en"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>QR code not found</title>
            <style>body{font-family:system-ui,sans-serif;max-width:32rem;margin:20vh auto;padding:0 1rem;text-align:center;color:#222}</style>
            </head><body>
            <h1>This QR code isn't active</h1>
            <p>The link may have been switched off or never existed. Please contact whoever gave you this code.</p>
            </body></html>
            """;

    private final QrCodeService service;

    public RedirectController(QrCodeService service) {
        this.service = service;
    }

    /**
     * 302 (never 301): browsers cache a 301 for good, which would defeat reassigning a code.
     * no-store keeps intermediaries from caching it too.
     */
    @GetMapping("/q/{code}")
    public ResponseEntity<String> redirect(@PathVariable String code) {
        // Cheap format check so junk never reaches the cache or the database
        if (CODE_FORMAT.matcher(code).matches()) {
            Optional<String> target = service.resolveTarget(code);
            if (target.isPresent()) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(target.get()))
                        .cacheControl(CacheControl.noStore())
                        .build();
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noStore())
                .body(NOT_FOUND_PAGE);
    }
}
