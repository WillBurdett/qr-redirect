package com.example.qrredirect.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.example.qrredirect.e2e.support.E2ETestBase;
import com.example.qrredirect.e2e.support.Phone;
import com.microsoft.playwright.APIResponse;

/** The public side: what a person sees when they scan a printed code. No login involved. */
@Tag("REQ-RED-01")
@Tag("REQ-RED-02")
class PublicRedirectE2ETest extends E2ETestBase {

    @Test
    @Tag("smoke")
    @DisplayName("Health endpoint reports UP")
    void healthIsUp() {
        APIResponse res = newAnonymousClient().get("/actuator/health");
        assertThat(res.status()).isEqualTo(200);
        assertThat(res.text()).contains("UP");
    }

    @Test
    @Tag("smoke")
    @DisplayName("REQ-RED-01: scanning an active code returns an uncached 302 to the target")
    void activeCodeRedirects() {
        loginViaApi();
        String target = uniqueUrl();
        var created = api().createQrCode(api().createCompany(unique("redir"), "PILOT"), target);

        APIResponse res = new Phone(newAnonymousClient()).scan(created.code());

        assertThat(res.status()).isEqualTo(302);
        assertThat(res.headers().get("location")).isEqualTo(target);
        assertThat(res.headers().get("cache-control")).contains("no-store");
    }

    @Test
    @DisplayName("REQ-RED-02: unknown and malformed codes get the friendly 404 page")
    void unknownCodesGetFriendly404() {
        Phone phone = new Phone(newAnonymousClient());

        for (String code : new String[] {"doesNotExist", "ab", "bad%20code!"}) {
            APIResponse res = phone.scan(code);
            assertThat(res.status()).as("status for %s", code).isEqualTo(404);
            assertThat(res.headers().get("content-type")).contains("text/html");
            assertThat(res.text()).contains("isn't active");
        }
    }
}
