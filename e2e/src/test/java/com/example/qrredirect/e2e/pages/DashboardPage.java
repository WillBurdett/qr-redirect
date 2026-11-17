package com.example.qrredirect.e2e.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;

/** Page object for the admin dashboard (companies, QR code cards, filters, pagination). */
public class DashboardPage {

    private final Page page;

    public DashboardPage(Page page) {
        this.page = page;
    }

    public DashboardPage open() {
        page.navigate("/");
        return waitUntilLoaded();
    }

    public DashboardPage waitUntilLoaded() {
        PlaywrightAssertions.assertThat(logoutButton()).isVisible();
        return this;
    }

    public Locator logoutButton() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log out"));
    }

    public Locator errorBanner() {
        return page.getByRole(AriaRole.ALERT);
    }

    public Locator allCards() {
        return page.locator("article");
    }

    // ---- companies -----------------------------------------------------------------------

    private Locator addCompanyForm() {
        return page.locator("section").filter(new Locator.FilterOptions()
                .setHas(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Add company"))));
    }

    private Locator newQrForm() {
        return page.locator("section").filter(new Locator.FilterOptions()
                .setHas(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("New QR code"))));
    }

    public void addCompany(String name, String planType) {
        Locator form = addCompanyForm();
        form.getByPlaceholder("New company name").fill(name);
        form.locator("select").selectOption(planType);
        form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add").setExact(true)).click();
        // The new company shows up as an option in the "New QR code" form
        PlaywrightAssertions.assertThat(
                newQrForm().locator("option").filter(new Locator.FilterOptions().setHasText(name))).hasCount(1);
    }

    // ---- QR codes ------------------------------------------------------------------------

    /** Creates a code through the form and returns the card, located by its short code. */
    public QrCard createQrCode(String companyName, String targetUrl) {
        Locator form = newQrForm();
        form.locator("select").selectOption(new SelectOption().setLabel(companyName));
        form.locator("input[type=url]").fill(targetUrl);
        form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Create QR code")).click();

        Locator card = cardWithText(targetUrl);
        PlaywrightAssertions.assertThat(card).isVisible();
        String shortUrl = card.locator("p.font-mono").innerText();
        return card(shortUrl.substring(shortUrl.lastIndexOf('/') + 1));
    }

    /** Submits the form without waiting for a card (used for rejected input). */
    public void submitQrCode(String companyName, String targetUrl) {
        Locator form = newQrForm();
        form.locator("select").selectOption(new SelectOption().setLabel(companyName));
        form.locator("input[type=url]").fill(targetUrl);
        form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Create QR code")).click();
    }

    public Locator cardWithText(String text) {
        return allCards().filter(new Locator.FilterOptions().setHasText(text));
    }

    /** A card found by its short code (stable even when the target URL is edited). */
    public QrCard card(String shortCode) {
        return new QrCard(page, shortCode);
    }

    // ---- filters and pagination ------------------------------------------------------------

    public void filterByCompany(String companyName) {
        page.locator("select").filter(new Locator.FilterOptions().setHasText("All companies"))
                .selectOption(new SelectOption().setLabel(companyName));
    }

    public void filterByStatus(String label) {   // "Active only" | "Inactive only" | "Active + inactive"
        page.locator("select").filter(new Locator.FilterOptions().setHasText("Active only"))
                .selectOption(new SelectOption().setLabel(label));
    }

    public Locator pageIndicator(int page1Based, int total) {
        return page.getByText("Page " + page1Based + " of " + total);
    }

    public void nextPage() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();
    }

    /** One QR code card. */
    public static class QrCard {
        private final Page page;
        private final String code;
        private final Locator root;

        QrCard(Page page, String code) {
            this.page = page;
            this.code = code;
            this.root = page.locator("article").filter(new Locator.FilterOptions().setHas(page.getByAltText(code)));
        }

        public String code() { return code; }
        public Locator root() { return root; }

        public Locator image() { return root.locator("img"); }

        public Locator hits() {
            return root.locator("p").filter(new Locator.FilterOptions().setHasText(Pattern.compile("hits?$")));
        }

        public void assertHits(int expected) {
            Pattern p = Pattern.compile("^" + expected + (expected == 1 ? " hit$" : " hits$"));
            PlaywrightAssertions.assertThat(hits()).hasText(p);
        }

        public Locator statusLabel(String label) {   // "Active" | "Inactive"
            return root.getByText(label, new Locator.GetByTextOptions().setExact(true));
        }

        public void toggle() {
            root.getByRole(AriaRole.SWITCH).click();
        }

        public Locator switchControl() {
            return root.getByRole(AriaRole.SWITCH);
        }

        public void editTarget(String newUrl) {
            root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Edit").setExact(true)).click();
            Locator dialog = page.getByRole(AriaRole.DIALOG);
            dialog.getByLabel("Destination URL").fill(newUrl);
            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save")).click();
            PlaywrightAssertions.assertThat(dialog).isHidden();
        }

        public void clickDelete() {
            root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete").setExact(true)).click();
        }

        public Locator deleteDialog() {
            return page.getByRole(AriaRole.DIALOG);
        }

        public void confirmDelete() {
            deleteDialog().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete").setExact(true)).click();
        }

        public void cancelDelete() {
            deleteDialog().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel")).click();
        }

        public com.microsoft.playwright.Download clickDownload() {
            return page.waitForDownload(() ->
                    root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Download").setExact(true)).click());
        }
    }
}
