package com.example.qrredirect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.example.qrredirect.config.AppProperties;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

class UnitTests {

    private static final AppProperties PROPS = new AppProperties("https://qr.example.com/", false, null);
    @Test
    void shortCodesAreRightLengthBase62AndDoNotRepeat() {
        ShortCodeGenerator generator = new ShortCodeGenerator();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            String code = generator.next();
            assertThat(code).hasSize(ShortCodeGenerator.LENGTH).matches("[0-9A-Za-z]+");
            seen.add(code);
        }
        assertThat(seen).hasSize(20_000);
    }

    @Test
    void urlPolicyAcceptsNormalHttpsUrls() {
        TargetUrlPolicy policy = new TargetUrlPolicy(PROPS);
        assertThat(policy.validate("  https://example.com/path?x=1#frag ")).isEqualTo("https://example.com/path?x=1#frag");
    }

    @Test
    void urlPolicyRejectsRiskyOrMalformedUrls() {
        TargetUrlPolicy policy = new TargetUrlPolicy(PROPS);
        for (String bad : new String[] {
                null, "", "   ", "not a url", "http://example.com", "javascript:alert(1)",
                "ftp://example.com/file", "https://", "https://user:pw@example.com",
                "https://trusted.com@evil.com", "https://exa mple.com",
                "https://example.com/" + "a".repeat(2100) }) {
            assertThatThrownBy(() -> policy.validate(bad))
                    .as("should reject %s", bad)
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test
    void urlPolicyAllowsHttpOnlyWhenConfigured() {
        TargetUrlPolicy policy = new TargetUrlPolicy(new AppProperties(null, true, null));
        assertThat(policy.validate("http://example.com")).isEqualTo("http://example.com");
    }

    @Test
    void baseUrlTrailingSlashIsTrimmedAndShortUrlBuilt() {
        assertThat(new QrImageService(PROPS).shortUrl("Ab3xK9z")).isEqualTo("https://qr.example.com/q/Ab3xK9z");
    }

    /** Rebuilds the module grid from the SVG path and compares it to what ZXing produced. */
    @Test
    void svgPathMatchesTheRealQrMatrix() throws Exception {
        String content = "https://qr.example.com/q/Ab3xK9z";
        String svg = new QrImageService(PROPS).svg(content, ErrorCorrectionLevel.M);

        BitMatrix expected = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0,
                Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 4));
        int w = expected.getWidth();
        int h = expected.getHeight();

        assertThat(svg).startsWith("<svg").contains("viewBox=\"0 0 " + w + " " + h + "\"");

        boolean[][] actual = new boolean[h][w];
        Matcher m = Pattern.compile("M(\\d+) (\\d+)h(\\d+)v1h-(\\d+)z").matcher(svg);
        int runs = 0;
        while (m.find()) {
            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));
            int run = Integer.parseInt(m.group(3));
            assertThat(m.group(4)).isEqualTo(m.group(3));
            for (int i = 0; i < run; i++) {
                actual[y][x + i] = true;
            }
            runs++;
        }
        assertThat(runs).isPositive();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertThat(actual[y][x]).as("module (%d,%d)", x, y).isEqualTo(expected.get(x, y));
            }
        }
    }
}
