package com.example.qrredirect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under the "app." prefix.
 *
 * @param baseUrl   public base URL encoded into every QR code (no trailing slash)
 * @param allowHttp whether http:// redirect targets are accepted (https only by default)
 * @param apiKey    the operator's key for /api/** (temporary: replaced by operator login in the next step)
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String baseUrl, boolean allowHttp, String apiKey) {

    public AppProperties {
        baseUrl = (baseUrl == null || baseUrl.isBlank())
                ? "http://localhost:8080"
                : baseUrl.trim().replaceAll("/+$", "");
    }
}