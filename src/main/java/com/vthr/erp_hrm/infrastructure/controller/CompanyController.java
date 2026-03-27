package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.CompanyMember;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.service.AuditLogService;
import com.vthr.erp_hrm.core.service.CompanyService;
import com.vthr.erp_hrm.core.service.UserService;
import com.vthr.erp_hrm.infrastructure.controller.dto.AddHrMemberRequest;
import com.vthr.erp_hrm.infrastructure.controller.dto.UpdateHrMemberRoleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    private boolean isAuthorized(UUID currentUserId, UUID targetCompanyId) {
        User currentUser = userService.getUserById(currentUserId);
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }
        if (currentUser.getRole() == Role.HR_MANAGER && targetCompanyId.equals(currentUser.getCompanyId())) {
            return true;
        }
        return false;
    }

    @GetMapping("/{companyId}/hr-members")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<List<CompanyMember>> getCompanyMembers(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        if (!isAuthorized(currentUserId, companyId)) {
            return ResponseEntity.status(403).build();
        }

        List<CompanyMember> members = companyService.getCompanyMembers(companyId);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{companyId}/hr-members")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<CompanyMember> addHrMember(
            @PathVariable UUID companyId,
            @Valid @RequestBody AddHrMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        if (!isAuthorized(currentUserId, companyId)) {
            return ResponseEntity.status(403).build();
        }

        CompanyMember member = companyService.addHrMember(companyId, request.getUserId(), request.getMemberRole());
        
        auditLogService.logAction(
                currentUserId,
                "ADD_HR_MEMBER",
                "CompanyMember",
                member.getId(),
                "Added user " + request.getUserId() + " to company " + companyId + " as " + request.getMemberRole()
        );

        return ResponseEntity.ok(member);
    }

    @PutMapping("/{companyId}/hr-members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Void> updateHrMemberRole(
            @PathVariable UUID companyId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateHrMemberRoleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        if (!isAuthorized(currentUserId, companyId)) {
            return ResponseEntity.status(403).build();
        }

        companyService.updateMemberRole(companyId, userId, request.getMemberRole());
        
        auditLogService.logAction(
                currentUserId,
                "UPDATE_HR_MEMBER_ROLE",
                "CompanyMember",
                userId,
                "Updated role to " + request.getMemberRole() + " for user " + userId + " in company " + companyId
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{companyId}/hr-members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Void> removeHrMember(
            @PathVariable UUID companyId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        if (!isAuthorized(currentUserId, companyId)) {
            return ResponseEntity.status(403).build();
        }

        companyService.removeMember(companyId, userId);
        
        auditLogService.logAction(
                currentUserId,
                "REMOVE_HR_MEMBER",
                "CompanyMember",
                userId,
                "Removed user " + userId + " from company " + companyId
        );

        return ResponseEntity.noContent().build();
    }
}
