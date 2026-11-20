package com.example.qrredirect.e2e.support;

import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;

/** Thin wrapper over the management API, used only to arrange test data (not to assert). */
public class ApiClient {

    private final APIRequestContext request;

    public ApiClient(APIRequestContext request) {
        this.request = request;
    }

    public long createCompany(String name, String planType) {
        JsonObject body = json(request.post("/api/companies",
                RequestOptions.create().setData(Map.of("name", name, "planType", planType))), 201);
        return body.get("id").getAsLong();
    }

    /** Creates a QR code and returns its id and short code. */
    public Created createQrCode(long companyId, String targetUrl) {
        JsonObject body = json(request.post("/api/qrCodes",
                RequestOptions.create().setData(Map.of("companyId", companyId, "targetUrl", targetUrl))), 201);
        return new Created(body.get("id").getAsLong(), body.get("shortQrId").getAsString());
    }

    public void setActive(long qrId, boolean active) {
        json(request.put("/api/qrCodes/" + qrId, RequestOptions.create().setData(Map.of("active", active))), 200);
    }

    public record Created(long id, String code) { }

    private static JsonObject json(APIResponse res, int expectedStatus) {
        if (res.status() != expectedStatus) {
            throw new IllegalStateException("Expected HTTP " + expectedStatus + " but got " + res.status()
                    + " from " + res.url() + ": " + res.text());
        }
        return JsonParser.parseString(res.text()).getAsJsonObject();
    }
}
