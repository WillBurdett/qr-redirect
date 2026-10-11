package com.example.qrredirect.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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
 * Temporary guard for /api/**: a single operator key in the X-API-Key header. The public redirect
 * (/q/**) is intentionally not protected.
 *
 * The next step replaces this with a proper operator login (Spring Security), so don't deploy
 * this version anywhere public.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final byte[] expected;   // null when no key is configured

    public ApiKeyFilter(AppProperties props) {
        String key = props.apiKey();
        if (key == null || key.isBlank()) {
            log.warn("No operator API key configured (app.api-key): every /api request will be rejected");
            this.expected = null;
        } else {
            this.expected = key.getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        boolean ok = expected != null
                && provided != null
                && MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expected);
        if (!ok) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Missing or invalid " + HEADER + " header\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}