package com.example.qrredirect.e2e.support;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;

/** Simulates a phone scanning a printed QR code: an anonymous GET of /q/{code}, redirects not followed. */
public class Phone {

    public static final String HUMAN_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) E2E";
    public static final String BOT_UA = "Googlebot/2.1 (+http://www.google.com/bot.html)";

    private final APIRequestContext client;

    public Phone(APIRequestContext client) {
        this.client = client;
    }

    public APIResponse scan(String code) {
        return scan(code, HUMAN_UA);
    }

    public APIResponse scan(String code, String userAgent) {
        return client.get("/q/" + code,
                RequestOptions.create().setMaxRedirects(0).setHeader("User-Agent", userAgent));
    }
}
