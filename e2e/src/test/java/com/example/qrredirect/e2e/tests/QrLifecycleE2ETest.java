package com.example.qrredirect.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.example.qrredirect.e2e.pages.DashboardPage;
import com.example.qrredirect.e2e.pages.DashboardPage.QrCard;
import com.example.qrredirect.e2e.support.E2ETestBase;
import com.example.qrredirect.e2e.support.Phone;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * The admin journey through the dashboard, checked against what the public actually experiences.
 * Pattern: arrange data through the API (fast), act through the UI, assert in the UI and on /q/{code}.
 */
class QrLifecycleE2ETest extends E2ETestBase {

    private Phone phone;

    @BeforeEach
    void signIn() {
        loginViaApi();
        phone = new Phone(newAnonymousClient());
    }

    @Test
    @Tag("smoke")
    @Tag("REQ-CO-01")
    @Tag("REQ-QR-01")
    @DisplayName("REQ-CO-01/QR-01: create a company and a QR code through the UI; the card renders with a working image")
    void createCompanyAndQrCode() {
        DashboardPage dashboard = new DashboardPage(page).open();
        String company = unique("Acme");
        String target = uniqueUrl();

        dashboard.addCompany(company, "PILOT");
        QrCard card = dashboard.createQrCode(company, target);

        PlaywrightAssertions.assertThat(card.root()).containsText(company);
        PlaywrightAssertions.assertThat(card.root()).containsText(target);
        PlaywrightAssertions.assertThat(card.root()).containsText("/q/" + card.code());
        PlaywrightAssertions.assertThat(card.statusLabel("Active")).isVisible();
        card.assertHits(0);

        // The QR image must actually load (not just have an <img> tag)
        page.waitForFunction(
                "code => { const i = document.querySelector('img[alt=\"' + code + '\"]');"
                        + " return !!i && i.complete && i.naturalWidth > 0; }",
                card.code());
    }

    @Test
    @Tag("REQ-QR-01")
    @DisplayName("REQ-QR-01: an insecure http:// target is rejected with an error and no card is created")
    void insecureUrlIsRejected() {
        String company = unique("Insecure");
        api().createCompany(company, "PILOT");
        DashboardPage dashboard = new DashboardPage(page).open();
        String bad = "http://insecure.example.com/" + java.util.UUID.randomUUID();

        dashboard.submitQrCode(company, bad);

        PlaywrightAssertions.assertThat(dashboard.errorBanner()).isVisible();
        PlaywrightAssertions.assertThat(dashboard.cardWithText(bad)).hasCount(0);
    }

    @Test
    @Tag("smoke")
    @Tag("REQ-SCAN-01")
    @Tag("REQ-RED-01")
    @DisplayName("REQ-SCAN-01: a human scan is counted on the dashboard; a bot scan is not")
    void scansAreCountedButBotsAreNot() {
        var created = api().createQrCode(api().createCompany(unique("Scan"), "PILOT"), uniqueUrl());
        DashboardPage dashboard = new DashboardPage(page).open();
        QrCard card = dashboard.card(created.code());
        card.assertHits(0);

        assertThat(phone.scan(created.code()).status()).isEqualTo(302);
        assertThat(phone.scan(created.code(), Phone.BOT_UA).status()).isEqualTo(302);   // still redirects

        page.reload();
        card.assertHits(1);   // only the human scan counted
    }

    @Test
    @Tag("REQ-QR-02")
    @Tag("REQ-SCAN-01")
    @DisplayName("REQ-QR-02: editing the destination takes effect on the very next scan and resets the hit count")
    void editDestinationRedirectsToNewTargetAndResetsHits() {
        String first = uniqueUrl();
        String second = uniqueUrl();
        var created = api().createQrCode(api().createCompany(unique("Edit"), "PILOT"), first);
        DashboardPage dashboard = new DashboardPage(page).open();
        QrCard card = dashboard.card(created.code());

        assertThat(phone.scan(created.code()).headers().get("location")).isEqualTo(first);
        page.reload();
        card.assertHits(1);

        card.editTarget(second);

        PlaywrightAssertions.assertThat(card.root()).containsText(second);
        card.assertHits(0);
        assertThat(phone.scan(created.code()).headers().get("location")).isEqualTo(second);
    }

    @Test
    @Tag("REQ-QR-03")
    @DisplayName("REQ-QR-03: deactivating a code shows the friendly 404 to scanners and is not counted; reactivating restores it")
    void deactivateAndReactivate() {
        String target = uniqueUrl();
        var created = api().createQrCode(api().createCompany(unique("Toggle"), "PILOT"), target);
        DashboardPage dashboard = new DashboardPage(page).open();
        QrCard card = dashboard.card(created.code());
        PlaywrightAssertions.assertThat(card.statusLabel("Active")).isVisible();

        card.toggle();
        PlaywrightAssertions.assertThat(card.statusLabel("Inactive")).isVisible();
        PlaywrightAssertions.assertThat(card.switchControl()).hasAttribute("aria-checked", "false");

        APIResponse off = phone.scan(created.code());
        assertThat(off.status()).isEqualTo(404);
        assertThat(off.text()).contains("isn't active");
        page.reload();
        card.assertHits(0);

        card.toggle();
        PlaywrightAssertions.assertThat(card.statusLabel("Active")).isVisible();
        assertThat(phone.scan(created.code()).headers().get("location")).isEqualTo(target);
    }

    @Test
    @Tag("REQ-QR-04")
    @DisplayName("REQ-QR-04: delete asks for confirmation; cancel keeps the code, confirm removes it for good")
    void deleteNeedsConfirmation() {
        var created = api().createQrCode(api().createCompany(unique("Delete"), "PILOT"), uniqueUrl());
        DashboardPage dashboard = new DashboardPage(page).open();
        QrCard card = dashboard.card(created.code());

        card.clickDelete();
        PlaywrightAssertions.assertThat(card.deleteDialog()).containsText("Delete this QR code?");
        card.cancelDelete();
        PlaywrightAssertions.assertThat(card.root()).isVisible();
        assertThat(phone.scan(created.code()).status()).isEqualTo(302);

        card.clickDelete();
        card.confirmDelete();
        PlaywrightAssertions.assertThat(card.root()).hasCount(0);
        assertThat(phone.scan(created.code()).status()).isEqualTo(404);
    }

    @Test
    @Tag("REQ-QR-05")
    @DisplayName("REQ-QR-05: Download gives one zip containing the PNG and the SVG")
    void downloadZipContainsPngAndSvg() throws IOException {
        var created = api().createQrCode(api().createCompany(unique("Download"), "PILOT"), uniqueUrl());
        QrCard card = new DashboardPage(page).open().card(created.code());

        Download download = card.clickDownload();

        assertThat(download.suggestedFilename()).isEqualTo("qr-" + created.code() + ".zip");
        List<String> entries = new ArrayList<>();
        try (ZipFile zip = new ZipFile(download.path().toFile())) {
            zip.stream().map(ZipEntry::getName).forEach(entries::add);
        }
        assertThat(entries).containsExactlyInAnyOrder(created.code() + ".png", created.code() + ".svg");
    }

    @Test
    @Tag("REQ-QR-06")
    @DisplayName("REQ-QR-06: filters narrow the list by company and by status, and can be cleared")
    void filtersByCompanyAndStatus() {
        String companyA = unique("FilterA");
        String companyB = unique("FilterB");
        String urlA = uniqueUrl();
        String urlB = uniqueUrl();
        var a = api().createQrCode(api().createCompany(companyA, "PILOT"), urlA);
        api().createQrCode(api().createCompany(companyB, "PILOT"), urlB);
        api().setActive(a.id(), false);
        DashboardPage dashboard = new DashboardPage(page).open();

        dashboard.filterByCompany(companyA);
        PlaywrightAssertions.assertThat(dashboard.allCards()).hasCount(1);
        PlaywrightAssertions.assertThat(dashboard.cardWithText(urlA)).hasCount(1);

        dashboard.filterByStatus("Active only");   // company A's only code is inactive
        PlaywrightAssertions.assertThat(dashboard.allCards()).hasCount(0);

        dashboard.filterByStatus("Inactive only");
        PlaywrightAssertions.assertThat(dashboard.cardWithText(urlA)).hasCount(1);

        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName("Clear filters")).click();
        PlaywrightAssertions.assertThat(dashboard.cardWithText(urlB)).hasCount(1);
    }

    @Test
    @Tag("REQ-QR-06")
    @DisplayName("REQ-QR-06: more than 10 codes are paginated")
    void paginationSplitsLongLists() {
        String company = unique("Paged");
        long companyId = api().createCompany(company, "PILOT");
        for (int i = 0; i < 11; i++) {
            api().createQrCode(companyId, uniqueUrl());
        }
        DashboardPage dashboard = new DashboardPage(page).open();

        dashboard.filterByCompany(company);
        PlaywrightAssertions.assertThat(dashboard.pageIndicator(1, 2)).isVisible();
        PlaywrightAssertions.assertThat(dashboard.allCards()).hasCount(10);

        dashboard.nextPage();
        PlaywrightAssertions.assertThat(dashboard.pageIndicator(2, 2)).isVisible();
        PlaywrightAssertions.assertThat(dashboard.allCards()).hasCount(1);
    }
}
