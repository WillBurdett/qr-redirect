package com.example.qrredirect.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;


import com.example.qrredirect.model.Company;
import com.example.qrredirect.model.PlanType;
import com.example.qrredirect.repo.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class CompanyControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired MockMvc mockMvc;
    @Autowired CompanyRepository companies;

    MockHttpSession session;

    @BeforeEach
    void logIn() throws Exception {
        var result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "admin@qr.com", "password": "password123" }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        session = (MockHttpSession) result.getRequest().getSession(false);
    }

    @Nested
    class create {

        @Test
        void rejectsLoginForUnknownEmail() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "nobody@qr.com", "password": "password123" }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void rejectsLoginWithWrongPassword() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "admin@qr.com", "password": "wrong-password" }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());
        }

        @Test
        void createsCompanyOnPilotPlanWithThirtyDayExpiry() throws Exception {
            mockMvc.perform(post("/api/companies")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Miles & Barr", "planType": "PILOT" }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Miles & Barr"))
                    .andExpect(jsonPath("$.planType").value("PILOT"));

            Company saved = companies.findAll().stream()
                    .filter(c -> c.getName().equals("Miles & Barr"))
                    .findFirst().orElseThrow();

            assertThat(saved.getPlanType()).isEqualTo(PlanType.PILOT);
            assertThat(saved.getPlanStartedAt()).isNotNull();
            assertThat(Duration.between(saved.getPlanStartedAt(), saved.getPlanExpiresAt()))
                    .isEqualTo(Duration.ofDays(30));
        }

        @Test
        void createsCompanyOnNonPilotPlanWithoutThirtyDayExpiry() throws Exception {
            mockMvc.perform(post("/api/companies")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Miles & Barr", "planType": "AGENCY" }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Miles & Barr"))
                    .andExpect(jsonPath("$.planType").value("AGENCY"));

            Company saved = companies.findAll().stream()
                    .filter(c -> c.getName().equals("Miles & Barr"))
                    .findFirst().orElseThrow();

            assertThat(saved.getPlanType()).isEqualTo(PlanType.AGENCY);
            assertThat(saved.getPlanStartedAt()).isNotNull();
            assertThat(saved.getPlanExpiresAt()).isNull();
        }

        @Test
        void rejectsCompanyWithEmptyName() throws Exception {
            mockMvc.perform(post("/api/companies")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "", "planType": "AGENCY" }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejectsDuplicateNameIgnoringCase() throws Exception {
            Company existing = new Company();
            existing.setName("Miles & Barr");
            existing.setPlanType(PlanType.PILOT);
            existing.setPlanStartedAt(Instant.now());
            companies.saveAndFlush(existing);

            mockMvc.perform(post("/api/companies")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "miles & barr", "planType": "PILOT" }
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        void rejectsCompanyWithUnknownPlan() throws Exception {
            mockMvc.perform(post("/api/companies")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "name": "Miles & Barr", "planType": "PLATINUM" }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }
}