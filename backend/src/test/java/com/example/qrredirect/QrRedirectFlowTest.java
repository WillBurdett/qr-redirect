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
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "app.base-url=https://qr.example.com",
        "app.api-key=test-key"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class QrRedirectFlowTest {

    private static final String KEY = "X-API-Key";
    private static final String API_KEY = "test-key";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    /** Creates a company with a unique name (the database is shared by every test in this class). */
    private long createCompany(String namePrefix) throws Exception {
        String body = mvc.perform(post("/api/companies")
                        .header(KEY, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + namePrefix + "-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private JsonNode createCode(long companyId, String targetUrl) throws Exception {
        String body = mvc.perform(post("/api/codes")
                        .header(KEY, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"targetUrl\":\"" + targetUrl + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value(startsWith("https://qr.example.com/q/")))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    @Test
    void createScanReassignAndDeactivate() throws Exception {
        long companyId = createCompany("flow");
        JsonNode created = createCode(companyId, "https://example.com/a");
        long id = created.get("id").asLong();
        String code = created.get("shortCode").asString();

        assertThat(created.get("companyId").asLong()).isEqualTo(companyId);

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
                        .header(KEY, API_KEY)
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
                        .header(KEY, API_KEY)
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
                        .header(KEY, API_KEY)
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
    void apiRequiresTheOperatorKeyButRedirectsArePublic() throws Exception {
        mvc.perform(get("/api/codes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/codes").header(KEY, "wrong")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/companies")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/codes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":1,\"targetUrl\":\"https://example.com\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/codes").header(KEY, API_KEY)).andExpect(status().isOk());
        mvc.perform(get("/api/companies").header(KEY, API_KEY)).andExpect(status().isOk());
    }

    @Test
    void companyRulesAreEnforced() throws Exception {
        String name = "Rules-" + UUID.randomUUID();
        String body = mvc.perform(post("/api/companies").header(KEY, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  " + name + "  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))   // trimmed
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).get("id").asLong();

        // Same name again, in a different case: conflict
        mvc.perform(post("/api/companies").header(KEY, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name.toUpperCase() + "\"}"))
                .andExpect(status().isConflict());

        // Blank or missing name: bad request
        mvc.perform(post("/api/companies").header(KEY, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/companies").header(KEY, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        // Readable by id and in the list
        mvc.perform(get("/api/companies/" + id).header(KEY, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));
        String list = mvc.perform(get("/api/companies").header(KEY, API_KEY))
                .andReturn().getResponse().getContentAsString();
        assertThat(list).contains(name);

        // Unknown company: 404 for reads, and codes can't be created for it
        mvc.perform(get("/api/companies/999999999").header(KEY, API_KEY)).andExpect(status().isNotFound());
        mvc.perform(post("/api/codes").header(KEY, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":999999999,\"targetUrl\":\"https://example.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRejectsUnsafeOrMissingInput() throws Exception {
        long companyId = createCompany("validation");
        for (String body : new String[] {
                "{\"companyId\":" + companyId + ",\"targetUrl\":\"http://example.com\"}",
                "{\"companyId\":" + companyId + ",\"targetUrl\":\"javascript:alert(1)\"}",
                "{\"companyId\":" + companyId + ",\"targetUrl\":\"https://user:pw@example.com\"}",
                "{\"companyId\":" + companyId + ",\"targetUrl\":\"not a url\"}",
                "{\"companyId\":" + companyId + ",\"targetUrl\":\"\"}",
                "{\"companyId\":" + companyId + "}",
                "{\"targetUrl\":\"https://example.com\"}",
                "{}" }) {
            mvc.perform(post("/api/codes").header(KEY, API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void updateRejectsUnsafeTarget() throws Exception {
        long id = createCode(createCompany("update"), "https://example.com/ok").get("id").asLong();
        mvc.perform(put("/api/codes/" + id).header(KEY, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUrl\":\"http://insecure.example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void codesBelongToACompanyAndCanBeFiltered() throws Exception {
        long acmeId = createCompany("acme");
        long globexId = createCompany("globex");
        JsonNode acmeCode = createCode(acmeId, "https://acme.example.com");
        JsonNode globexCode = createCode(globexId, "https://globex.example.com");
        String acme = acmeCode.get("shortCode").asString();
        String globex = globexCode.get("shortCode").asString();

        // The response says which company owns the code
        assertThat(acmeCode.get("companyId").asLong()).isEqualTo(acmeId);
        assertThat(acmeCode.get("companyName").asString()).startsWith("acme-");

        // Filtered list: only that company's codes
        String acmeList = mvc.perform(get("/api/codes?companyId=" + acmeId).header(KEY, API_KEY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(acmeList).contains(acme).doesNotContain(globex);

        // Unfiltered list: everything
        String all = mvc.perform(get("/api/codes").header(KEY, API_KEY))
                .andReturn().getResponse().getContentAsString();
        assertThat(all).contains(acme).contains(globex);

        // Both still redirect to their own targets
        mvc.perform(get("/q/" + acme)).andExpect(header().string("Location", "https://acme.example.com"));
        mvc.perform(get("/q/" + globex)).andExpect(header().string("Location", "https://globex.example.com"));
    }

    @Test
    void pngImageDecodesBackToTheShortUrl() throws Exception {
        JsonNode created = createCode(createCompany("png"), "https://example.com/png");
        long id = created.get("id").asLong();

        byte[] png = mvc.perform(get("/api/codes/" + id + "/image?format=png&size=400").header(KEY, API_KEY))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn().getResponse().getContentAsByteArray();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image.getWidth()).isEqualTo(400);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        String decoded = new QRCodeReader().decode(bitmap).getText();

        // The code encodes the permanent short URL, not the target
        assertThat(decoded).isEqualTo(created.get("shortUrl").asString());
    }

    @Test
    void svgImageIsServedAndBadImageParamsAreRejected() throws Exception {
        long id = createCode(createCompany("svg"), "https://example.com/svg").get("id").asLong();

        mvc.perform(get("/api/codes/" + id + "/image?format=svg&ec=Q").header(KEY, API_KEY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(content().string(startsWith("<svg")));

        mvc.perform(get("/api/codes/" + id + "/image?format=gif").header(KEY, API_KEY)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/codes/" + id + "/image?size=10").header(KEY, API_KEY)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/codes/" + id + "/image?ec=Z").header(KEY, API_KEY)).andExpect(status().isBadRequest());
    }
}