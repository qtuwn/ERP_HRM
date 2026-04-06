package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.ApplicationService;
import com.vthr.erp_hrm.infrastructure.storage.FileStorageService;
import com.vthr.erp_hrm.infrastructure.storage.SignedUrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerCompanyScopeTest {

    @Mock
    private ApplicationService applicationService;

    @Mock
    private com.vthr.erp_hrm.core.service.JobService jobService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private SignedUrlService signedUrlService;

    @Mock
    private com.vthr.erp_hrm.core.repository.AIEvaluationRepository aiEvaluationRepository;

    @Mock
    private ApplicationAccessService applicationAccessService;

    @InjectMocks
    private ApplicationController controller;

    @Test
    void getApplicationsForJob_shouldReturn403WhenOutOfCompanyScope() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doThrow(new RuntimeException("Access denied"))
                .when(applicationAccessService)
                .requireRecruiterForJobTopic(eq(userId), eq(Role.HR), eq(jobId));

        Authentication auth = auth(userId, "ROLE_HR");

        assertThrows(RuntimeException.class, () ->
                controller.getApplicationsForJob(jobId, Pageable.ofSize(20), auth)
        );

        verify(applicationService, never()).getApplicationsByJobId(any(), any());
    }

    @Test
    void getApplicationsForJob_shouldCallAccessGuardThenReturn200() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(applicationService.getApplicationsByJobId(eq(jobId), any()))
                .thenReturn(new PageImpl<>(List.of()));

        Authentication auth = auth(userId, "ROLE_HR");

        ResponseEntity<com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse<org.springframework.data.domain.Page<com.vthr.erp_hrm.infrastructure.controller.response.ApplicationResponse>>> res =
                controller.getApplicationsForJob(jobId, Pageable.ofSize(20), auth);

        assertEquals(200, res.getStatusCode().value());

        verify(applicationAccessService).requireRecruiterForJobTopic(eq(userId), eq(Role.HR), eq(jobId));
    }

    @Test
    void getAiEvaluation_shouldRequireParticipant() {
        UUID appId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Authentication auth = auth(userId, "ROLE_HR");

        doThrow(new RuntimeException("Access denied"))
                .when(applicationAccessService)
                .requireParticipantForMessaging(eq(userId), eq(Role.HR), eq(appId));

        assertThrows(RuntimeException.class, () -> controller.getAiEvaluation(appId, auth));
    }

    @Test
    void getAiEvaluation_shouldReturn200WhenAllowed() {
        UUID appId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Authentication auth = auth(userId, "ROLE_HR");

        when(aiEvaluationRepository.findByApplicationId(appId)).thenReturn(Optional.empty());

        ResponseEntity<com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse<com.vthr.erp_hrm.core.model.AIEvaluation>> res =
                controller.getAiEvaluation(appId, auth);

        assertEquals(200, res.getStatusCode().value());
        verify(applicationAccessService).requireParticipantForMessaging(eq(userId), eq(Role.HR), eq(appId));
    }

    private Authentication auth(UUID userId, String role) {
        return new Authentication() {
            @Override
            public List<? extends GrantedAuthority> getAuthorities() {
                return List.of((GrantedAuthority) () -> role);
            }

            @Override
            public Object getCredentials() {
                return "n/a";
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return userId.toString();
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) {
                // ignore
            }

            @Override
            public String getName() {
                return userId.toString();
            }
        };
    }
}

