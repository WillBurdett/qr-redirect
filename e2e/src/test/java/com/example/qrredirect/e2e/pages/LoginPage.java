package com.example.qrredirect.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class LoginPage {

    private final Page page;

    public LoginPage(Page page) {
        this.page = page;
    }

    public LoginPage open() {
        page.navigate("/");
        return this;
    }

    public Locator heading() {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Log in"));
    }

    public Locator errorMessage() {
        return page.locator("form p.text-red-600");
    }

    public void submit(String email, String password) {
        page.getByPlaceholder("Email").fill(email);
        page.getByPlaceholder("Password").fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
    }

    public DashboardPage loginAs(String email, String password) {
        submit(email, password);
        return new DashboardPage(page).waitUntilLoaded();
    }
}
