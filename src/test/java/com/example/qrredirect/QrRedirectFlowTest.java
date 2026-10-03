package com.example.qrredirect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:qrtest;DB_CLOSE_DELAY=-1",
        "app.base-url=https://qr.example.com",
        "app.api-keys.acme=acme-key",
        "app.api-keys.globex=globex-key"
})
@AutoConfigureMockMvc
class QrRedirectFlowTest {

    private static final String KEY = "X-API-Key";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private JsonNode createCode(String apiKey, String targetUrl) throws Exception {
        String body = mvc.perform(post("/api/codes")
                        .header(KEY, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUrl\":\"" + targetUrl + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value(startsWith("https://qr.example.com/q/")))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    @Test
    void createScanReassignAndDeactivate() throws Exception {
        JsonNode created = createCode("acme-key", "https://example.com/a");
        long id = created.get("id").asLong();
        String code = created.get("shortCode").asText();

        // Scan: 302 to the first target, never cached
        mvc.perform(get("/q/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/a"))
                .andExpect(header().string("Cache-Control", "no-store"));
        // Second scan comes from the in-memory cache
        mvc.perform(get("/q/" + code))
                .andExpect(header().string("Location", "https://example.com/a"));

        // Reassign: the very next scan must see the new target (proves cache eviction)
        mvc.perform(put("/api/codes/" + id)
                        .header(KEY, "acme-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUrl\":\"https://example.com/b\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetUrl").value("https://example.com/b"))
                .andExpect(jsonPath("$.shortCode").value(code));
        mvc.perform(get("/q/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/b"));

        // Deactivate: scans now get the friendly 404 page
        mvc.perform(put("/api/codes/" + id)
                        .header(KEY, "acme-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.targetUrl").value("https://example.com/b"));
        mvc.perform(get("/q/" + code))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

        // Reactivate
        mvc.perform(put("/api/codes/" + id)
                        .header(KEY, "acme-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk());
        mvc.perform(get("/q/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/b"));
    }

    @Test
    void unknownAndMalformedCodesGetThe404Page() throws Exception {
        mvc.perform(get("/q/doesNotExist")).andExpect(status().isNotFound());
        mvc.perform(get("/q/ab")).andExpect(status().isNotFound());
        mvc.perform(get("/q/bad%20code!")).andExpect(status().isNotFound());
    }

    @Test
    void apiRequiresAValidKeyButRedirectsArePublic() throws Exception {
        mvc.perform(get("/api/codes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/codes").header(KEY, "wrong")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/codes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUrl\":\"https://example.com\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/codes").header(KEY, "acme-key")).andExpect(status().isOk());
    }

    @Test
    void createRejectsUnsafeOrMissingTargets() throws Exception {
        for (String body : new String[] {
                "{\"targetUrl\":\"http://example.com\"}",
                "{\"targetUrl\":\"javascript:alert(1)\"}",
                "{\"targetUrl\":\"https://user:pw@example.com\"}",
                "{\"targetUrl\":\"not a url\"}",
                "{\"targetUrl\":\"\"}",
                "{}" }) {
            mvc.perform(post("/api/codes").header(KEY, "acme-key")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void updateRejectsUnsafeTarget() throws Exception {
        long id = createCode("acme-key", "https://example.com/ok").get("id").asLong();
        mvc.perform(put("/api/codes/" + id).header(KEY, "acme-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUrl\":\"http://insecure.example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void companiesCannotSeeOrChangeEachOthersCodes() throws Exception {
        JsonNode acme = createCode("acme-key", "https://acme.example.com");
        long id = acme.get("id").asLong();

        mvc.perform(get("/api/codes/" + id).header(KEY, "globex-key")).andExpect(status().isNotFound());
        mvc.perform(get("/api/codes/" + id + "/image").header(KEY, "globex-key")).andExpect(status().isNotFound());
        mvc.perform(put("/api/codes/" + id).header(KEY, "globex-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUrl\":\"https://evil.example.com\"}"))
                .andExpect(status().isNotFound());

        String globexList = mvc.perform(get("/api/codes").header(KEY, "globex-key"))
                .andReturn().getResponse().getContentAsString();
        assertThat(globexList).doesNotContain(acme.get("shortCode").asText());

        // And acme's code still points where acme said
        mvc.perform(get("/q/" + acme.get("shortCode").asText()))
                .andExpect(header().string("Location", "https://acme.example.com"));
    }

    @Test
    void pngImageDecodesBackToTheShortUrl() throws Exception {
        JsonNode created = createCode("acme-key", "https://example.com/png");
        long id = created.get("id").asLong();

        byte[] png = mvc.perform(get("/api/codes/" + id + "/image?format=png&size=400").header(KEY, "acme-key"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn().getResponse().getContentAsByteArray();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image.getWidth()).isEqualTo(400);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        String decoded = new QRCodeReader().decode(bitmap).getText();

        // The code encodes the permanent short URL, not the target
        assertThat(decoded).isEqualTo(created.get("shortUrl").asText());
    }

    @Test
    void svgImageIsServedAndBadImageParamsAreRejected() throws Exception {
        long id = createCode("acme-key", "https://example.com/svg").get("id").asLong();

        mvc.perform(get("/api/codes/" + id + "/image?format=svg&ec=Q").header(KEY, "acme-key"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(content().string(startsWith("<svg")));

        mvc.perform(get("/api/codes/" + id + "/image?format=gif").header(KEY, "acme-key")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/codes/" + id + "/image?size=10").header(KEY, "acme-key")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/codes/" + id + "/image?ec=Z").header(KEY, "acme-key")).andExpect(status().isBadRequest());
    }
}
