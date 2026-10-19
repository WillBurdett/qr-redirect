package com.example.qrredirect.service;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.example.qrredirect.config.AppProperties;

/**
 * Rules for URLs companies are allowed to point a code at. Redirect services get abused for
 * phishing, so keep this strict. Next steps worth considering: a domain blocklist and a
 * Safe Browsing check.
 */
@Component
public class TargetUrlPolicy {

    private static final int MAX_LENGTH = 2048;

    private final boolean allowHttp;

    public TargetUrlPolicy(AppProperties props) {
        this.allowHttp = props.allowHttp();
    }

    /** Returns the trimmed URL if acceptable, otherwise throws a 400. */
    public String validate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw bad("targetUrl is required");
        }
        String url = raw.trim();
        if (url.length() > MAX_LENGTH) {
            throw bad("targetUrl must be at most " + MAX_LENGTH + " characters");
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw bad("targetUrl is not a valid URL");
        }

        String scheme = uri.getScheme();
        boolean schemeOk = "https".equalsIgnoreCase(scheme)
                || (allowHttp && "http".equalsIgnoreCase(scheme));
        if (!schemeOk) {
            throw bad(allowHttp ? "targetUrl must start with http:// or https://"
                                : "targetUrl must start with https://");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw bad("targetUrl must include a host name");
        }
        if (uri.getUserInfo() != null) {
            // blocks tricks like https://trusted.com@evil.com
            throw bad("targetUrl must not contain credentials");
        }
        return url;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
