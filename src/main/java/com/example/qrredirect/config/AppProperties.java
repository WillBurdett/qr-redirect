package com.example.qrredirect.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under the "app." prefix.
 *
 * @param baseUrl   public base URL encoded into every QR code (no trailing slash)
 * @param allowHttp whether http:// redirect targets are accepted (https only by default)
 * @param apiKeys   companyId -> API key. Each company's key scopes the management API to its own codes.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String baseUrl, boolean allowHttp, Map<String, String> apiKeys) {

    public AppProperties {
        baseUrl = (baseUrl == null || baseUrl.isBlank())
                ? "http://localhost:8080"
                : baseUrl.trim().replaceAll("/+$", "");
        apiKeys = apiKeys == null ? Map.of() : Map.copyOf(apiKeys);
    }
}
