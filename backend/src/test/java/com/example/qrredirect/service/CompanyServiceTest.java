package com.example.qrredirect.service;

import com.example.qrredirect.model.Company;
import com.example.qrredirect.model.Plan;
import com.example.qrredirect.model.PlanType;
import com.example.qrredirect.repo.CompanyRepository;
import com.example.qrredirect.repo.PlanRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    CompanyRepository repo;
    @Mock
    PlanRepository planRepo;
    @InjectMocks
    CompanyService service;

    @Nested
    class create {
        @Test
        void createPilotCompanyGetsExpiryFromPlanDuration() {
            stubPlan("PILOT", 30);

            Company result = service.create("Miles & Barr", "PILOT");

            assertThat(result.getPlanType()).isEqualTo(PlanType.PILOT);
            assertThat(Duration.between(result.getPlanStartedAt(), result.getPlanExpiresAt()))
                    .isEqualTo(Duration.ofDays(30));
        }

        @Test
        void createNonPilotCompanyWithoutExpiryFromPlanDuration() {
            stubPlan("BRANCH", null);

            Company result = service.create("Miles & Barr", "BRANCH");

            assertThat(result.getPlanType()).isEqualTo(PlanType.BRANCH);
            assertThat(result.getPlanStartedAt()).isNotNull();
            assertThat(result.getPlanExpiresAt()).isNull();
        }

        @Test
        void rejectCompanyEmptyName() {
            assertThatThrownBy(() -> service.create("", "BRANCH"))
                    .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(ex.getReason()).isEqualTo("name is required");
                    });

            verify(repo, never()).saveAndFlush(any());
        }

        @Test
        void rejectsCompanyNameExceedingMaxLength() {
            assertThatThrownBy(() -> service.create("thisIsAnExtremelyLongNameDesignedToExceedTheMaximumAllowedNameLengthOfOneHundredAndTwentyEightCharactersForTestingValidationAndEnsuringThatTheApplicationCorrectlyRejectsNamesThatAreTooLong", ""))
                    .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(ex.getReason()).isEqualTo("name must be at most 128 characters");
                    });

            verify(repo, never()).saveAndFlush(any());
        }

        @Test
        void rejectsCompanyDuplicateName() {
            when(repo.existsByNameIgnoreCase("Miles & Barr")).thenReturn(true);

            assertThatThrownBy(() -> service.create("Miles & Barr", "BRANCH"))
                    .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

            verify(repo, never()).saveAndFlush(any());
        }


        @Test
        void rejectsCompanyUnknownPlan() {
            assertThatThrownBy(() -> service.create("Miles & Barr", "PREMIUM"))
                    .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(ex.getReason()).isEqualTo("Unknown plan: PREMIUM");
                    });

            verify(repo, never()).saveAndFlush(any());
        }
    }

    private void stubPlan(String planType, Integer durationDays) {
        Plan plan = mock(Plan.class); when(plan.getDurationDays()).thenReturn(durationDays);
        when(planRepo.findByCode(planType)).thenReturn(Optional.of(plan));
        when(repo.existsByNameIgnoreCase("Miles & Barr")).thenReturn(false);
        when(repo.saveAndFlush(any(Company.class))).thenAnswer(i -> i.getArgument(0));
    }
}

