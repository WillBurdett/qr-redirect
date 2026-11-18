package com.example.qrredirect.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.example.qrredirect.e2e.pages.DashboardPage;
import com.example.qrredirect.e2e.pages.LoginPage;
import com.example.qrredirect.e2e.support.E2ETestBase;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

@Tag("REQ-AUTH-01")
@Tag("REQ-AUTH-02")
class AuthE2ETest extends E2ETestBase {

    @Test
    @Tag("smoke")
    @DisplayName("REQ-AUTH-01: valid admin credentials open the dashboard")
    void validLoginShowsDashboard() {
        DashboardPage dashboard = new LoginPage(page).open().loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);

        PlaywrightAssertions.assertThat(dashboard.logoutButton()).isVisible();
        PlaywrightAssertions.assertThat(page.getByText(ADMIN_EMAIL)).isVisible();
    }

    @Test
    @DisplayName("REQ-AUTH-01: a wrong password shows an error and stays on the login page")
    void wrongPasswordIsRejected() {
        LoginPage login = new LoginPage(page).open();
        login.submit(ADMIN_EMAIL, "definitely-wrong");

        PlaywrightAssertions.assertThat(login.errorMessage()).isVisible();
        PlaywrightAssertions.assertThat(login.heading()).isVisible();
    }

    @Test
    @DisplayName("REQ-AUTH-01: logging out returns to the login page and ends the session")
    void logoutEndsSession() {
        DashboardPage dashboard = new LoginPage(page).open().loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);
        dashboard.logoutButton().click();

        PlaywrightAssertions.assertThat(new LoginPage(page).heading()).isVisible();
        page.reload();
        PlaywrightAssertions.assertThat(new LoginPage(page).heading()).isVisible();
    }

    @Test
    @Tag("smoke")
    @DisplayName("REQ-AUTH-02: the management API rejects anonymous callers")
    void managementApiRequiresLogin() {
        APIRequestContext anonymous = newAnonymousClient();

        assertThat(anonymous.get("/api/qrCodes").status()).isEqualTo(401);
        assertThat(anonymous.get("/api/companies").status()).isEqualTo(401);
    }
}
