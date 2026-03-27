package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.AccountStatus;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.service.AuditLogService;
import com.vthr.erp_hrm.core.service.CompanyService;
import com.vthr.erp_hrm.core.service.UserService;
import com.vthr.erp_hrm.infrastructure.controller.dto.UpdateHrMemberRoleRequest;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompanyControllerTest {

    @Mock
    private CompanyService companyService;

    @Mock
    private UserService userService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CompanyController companyController;

    @Test
    void updateHrMemberRole_Success() {
        UUID companyId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID hrManagerId = UUID.randomUUID();

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(hrManagerId.toString());

        User hrManager = User.builder()
                .id(hrManagerId)
                .role(Role.HR_MANAGER)
                .companyId(companyId)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userService.getUserById(hrManagerId)).thenReturn(hrManager);
        doNothing().when(companyService).updateMemberRole(companyId, memberId, "HR");

        UpdateHrMemberRoleRequest req = new UpdateHrMemberRoleRequest();
        req.setMemberRole("HR");

        ResponseEntity<Void> response = companyController.updateHrMemberRole(companyId, memberId, req, userDetails);

        assertEquals(204, response.getStatusCode().value());

        verify(auditLogService).logAction(eq(hrManagerId), eq("UPDATE_HR_MEMBER_ROLE"), eq("CompanyMember"), eq(memberId), anyString());
    }

    @Test
    void updateHrMemberRole_Unauthorized_DifferentCompany() {
        UUID requestCompanyId = UUID.randomUUID();
        UUID managerCompanyId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID hrManagerId = UUID.randomUUID();

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(hrManagerId.toString());

        User hrManager = User.builder()
                .id(hrManagerId)
                .role(Role.HR_MANAGER)
                .companyId(managerCompanyId)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userService.getUserById(hrManagerId)).thenReturn(hrManager);

        UpdateHrMemberRoleRequest req = new UpdateHrMemberRoleRequest();
        req.setMemberRole("HR");

        ResponseEntity<Void> response = companyController.updateHrMemberRole(requestCompanyId, memberId, req, userDetails);

        assertEquals(403, response.getStatusCode().value());
        verify(companyService, never()).updateMemberRole(any(), any(), any());
    }
}
