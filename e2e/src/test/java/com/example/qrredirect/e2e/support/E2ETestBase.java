package com.example.qrredirect.e2e.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.RequestOptions;

/**
 * Shared set-up for every E2E test.
 *
 * - One browser per test class, a fresh isolated BrowserContext (cookies, storage) per test.
 * - A Playwright trace + screenshot is saved to target/playwright when a test fails.
 * - Tests talk to an already-running app (see docker-compose.e2e.yml); nothing is started here.
 */
public abstract class E2ETestBase {

    public static final String BASE_URL =
            System.getProperty("e2e.baseUrl", System.getenv().getOrDefault("E2E_BASE_URL", "http://localhost:8080"));
    public static final String ADMIN_EMAIL = System.getenv().getOrDefault("E2E_ADMIN_EMAIL", "e2e@qr.test");
    public static final String ADMIN_PASSWORD = System.getenv().getOrDefault("E2E_ADMIN_PASSWORD", "e2e-password");

    private static Playwright playwright;
    private static Browser browser;

    protected BrowserContext context;
    protected Page page;

    @RegisterExtension
    final FailureTracker failures = new FailureTracker();

    @BeforeAll
    static void launchBrowser() {
        PlaywrightAssertions.setDefaultAssertionTimeout(10_000);
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(!Boolean.getBoolean("e2e.headed")));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void openContext() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(BASE_URL)
                .setViewportSize(1280, 900)
                .setAcceptDownloads(true));
        context.setDefaultTimeout(10_000);
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
        page = context.newPage();
    }

    @AfterEach
    void closeContext(TestInfo info) throws IOException {
        if (failures.failed) {
            Path dir = Path.of("target", "playwright");
            Files.createDirectories(dir);
            String name = info.getTestClass().map(Class::getSimpleName).orElse("test")
                    + "-" + info.getTestMethod().map(m -> m.getName()).orElse("unknown");
            page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(name + ".png")).setFullPage(true));
            context.tracing().stop(new Tracing.StopOptions().setPath(dir.resolve(name + "-trace.zip")));
        } else {
            context.tracing().stop();
        }
        context.close();
    }

    // ---- helpers -------------------------------------------------------------------------

    /** A name that is unique per run, so tests never collide on the shared database. */
    protected static String unique(String prefix) {
        return prefix + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /** A unique https target, so a QR card can be found on the dashboard by its URL. */
    protected static String uniqueUrl() {
        return "https://example.com/" + java.util.UUID.randomUUID();
    }

    /**
     * Logs the browser in without going through the login form. Used to arrange a test quickly;
     * the login form itself is covered by AuthE2ETest.
     */
    protected void loginViaApi() {
        APIResponse res = context.request().post("/auth/login",
                RequestOptions.create().setData(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD)));
        if (!res.ok()) {
            throw new IllegalStateException("API login failed (" + res.status() + "): " + res.text()
                    + " - is the app running at " + BASE_URL + " with the e2e admin user?");
        }
    }

    /** API helper bound to the browser's session (arrange test data quickly, then assert through the UI). */
    protected ApiClient api() {
        return new ApiClient(context.request());
    }

    /** A cookie-less client: behaves like a phone that scans a printed QR code. */
    protected APIRequestContext newAnonymousClient() {
        return playwright.request().newContext(new APIRequest.NewContextOptions().setBaseURL(BASE_URL));
    }

    /** Marks the current test as failed (read in @AfterEach to decide whether to keep the trace). */
    static final class FailureTracker implements AfterTestExecutionCallback {
        volatile boolean failed;

        @Override
        public void afterTestExecution(ExtensionContext ctx) {
            failed = ctx.getExecutionException().isPresent();
        }
    }
}
