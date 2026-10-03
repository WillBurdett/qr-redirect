package com.example.qrredirect.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.qrredirect.config.AppProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Minimal per-company API key auth for /api/**. The key identifies the company, and the company
 * id is exposed to controllers as the "companyId" request attribute so every query is scoped to it.
 * The public redirect (/q/**) is intentionally not protected.
 *
 * Replace with Spring Security (JWT/OAuth, user accounts) when you outgrow this.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";
    public static final String COMPANY_ATTRIBUTE = "companyId";

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    /** key -> companyId */
    private final Map<String, String> companyByKey = new HashMap<>();

    public ApiKeyFilter(AppProperties props) {
        props.apiKeys().forEach((companyId, key) -> {
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("Blank API key configured for company '" + companyId + "'");
            }
            if (companyByKey.put(key, companyId) != null) {
                throw new IllegalStateException("The same API key is configured for more than one company");
            }
        });
        if (companyByKey.isEmpty()) {
            log.warn("No API keys configured (app.api-keys.<companyId>=<key>): every /api request will be rejected");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String companyId = lookup(request.getHeader(HEADER));
        if (companyId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Missing or invalid " + HEADER + " header\"}");
            return;
        }
        request.setAttribute(COMPANY_ATTRIBUTE, companyId);
        chain.doFilter(request, response);
    }

    private String lookup(String provided) {
        if (provided == null || provided.isEmpty()) {
            return null;
        }
        byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
        String match = null;
        // Constant-time comparison; don't stop at the first hit.
        for (Map.Entry<String, String> e : companyByKey.entrySet()) {
            if (MessageDigest.isEqual(providedBytes, e.getKey().getBytes(StandardCharsets.UTF_8))) {
                match = e.getValue();
            }
        }
        return match;
    }
}
