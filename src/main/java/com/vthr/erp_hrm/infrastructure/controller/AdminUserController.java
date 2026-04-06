package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.service.UserService;
import com.vthr.erp_hrm.infrastructure.controller.request.UpdateUserRoleRequest;
import com.vthr.erp_hrm.core.model.AuditLog;
import com.vthr.erp_hrm.core.service.AuditLogService;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.UserResponse;
import com.vthr.erp_hrm.infrastructure.persistence.repository.CompanyRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final AuditLogService auditLogService;
    private final CompanyRepository companyRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID departmentId,
            Pageable pageable,
            Principal principal) {
        Role normalizedRole = role == null || role.isBlank() ? null : Role.fromString(role);

        Page<User> usersDomain = resolveUsersForAdmin(normalizedRole, companyId, departmentId, pageable);

        var companyIds = usersDomain.getContent().stream()
                .map(User::getCompanyId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        var companyNameMap = companyRepository.findAllById(companyIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.vthr.erp_hrm.infrastructure.persistence.entity.CompanyEntity::getId,
                        com.vthr.erp_hrm.infrastructure.persistence.entity.CompanyEntity::getName
                ));

        Page<UserResponse> users = usersDomain.map(u -> UserResponse.fromDomain(u, companyNameMap.get(u.getCompanyId())));
        return ResponseEntity.ok(ApiResponse.success(users, "Fetched users successfully"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs() {
        List<AuditLog> logs = auditLogService.getRecentLogs();
        return ResponseEntity.ok(ApiResponse.success(logs, "Fetched recent audit logs"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id, Principal principal) {
        User u = userService.getUserById(id);
        String companyName = null;
        if (u.getCompanyId() != null) {
            companyName = companyRepository.findById(u.getCompanyId()).map(com.vthr.erp_hrm.infrastructure.persistence.entity.CompanyEntity::getName).orElse(null);
        }
        UserResponse user = UserResponse.fromDomain(u, companyName);
        return ResponseEntity.ok(ApiResponse.success(user, "Fetched user successfully"));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Principal principal) {
        preventSelfRoleChange(id, principal);
        preventDemotingLastAdmin(id, request.getRole());
        UserResponse user = UserResponse.fromDomain(userService.updateUserRole(id, request.getRole()));
        
        UUID currentUserId = UUID.fromString(principal.getName());
        auditLogService.logAction(currentUserId, "UPDATE_USER_ROLE", "User", id, "Changed role to " + request.getRole());
        
        return ResponseEntity.ok(ApiResponse.success(user, "Updated user role successfully"));
    }

    @PatchMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<UserResponse>> lockUser(
            @PathVariable UUID id,
            Principal principal) {
        preventSelfAction(id, principal, "You cannot lock your own account");
        preventLockingLastAdmin(id);
        UserResponse user = UserResponse.fromDomain(userService.setUserActive(id, false));
        
        UUID currentUserId = UUID.fromString(principal.getName());
        auditLogService.logAction(currentUserId, "LOCK_USER", "User", id, "Locked user account");
        
        return ResponseEntity.ok(ApiResponse.success(user, "Locked user successfully"));
    }

    @PatchMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(
            @PathVariable UUID id,
            Principal principal) {
        UserResponse user = UserResponse.fromDomain(userService.setUserActive(id, true));

        UUID currentUserId = UUID.fromString(principal.getName());
        auditLogService.logAction(currentUserId, "UNLOCK_USER", "User", id, "Unlocked user account");

        return ResponseEntity.ok(ApiResponse.success(user, "Unlocked user successfully"));
    }

    @PatchMapping("/{id}/department")
    public ResponseEntity<ApiResponse<UserResponse>> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody com.vthr.erp_hrm.infrastructure.controller.request.UpdateUserDepartmentRequest request,
            Principal principal) {
        UserResponse user = UserResponse.fromDomain(userService.updateDepartment(id, request.getDepartment()));

        UUID currentUserId = UUID.fromString(principal.getName());
        auditLogService.logAction(currentUserId, "UPDATE_USER_DEPARTMENT", "User", id, "Updated user department to " + request.getDepartment());

        return ResponseEntity.ok(ApiResponse.success(user, "Updated user department successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID id,
            Principal principal) {
        preventSelfAction(id, principal, "You cannot delete your own account");
        preventDeletingLastAdmin(id);
        userService.deleteUser(id);

        UUID currentUserId = UUID.fromString(principal.getName());
        auditLogService.logAction(currentUserId, "DELETE_USER", "User", id, "Deleted user account");

        return ResponseEntity.ok(ApiResponse.success(null, "Deleted user successfully"));
    }

    private void preventDemotingLastAdmin(UUID targetUserId, Role newRole) {
        if (newRole == Role.ADMIN) {
            return;
        }

        UserResponse target = UserResponse.fromDomain(userService.getUserById(targetUserId));
        if (target.getRole() == Role.ADMIN && userService.countUsersByRole(Role.ADMIN) <= 1) {
            throw new RuntimeException("Cannot change role of the last ADMIN account");
        }
    }

    private void preventLockingLastAdmin(UUID targetUserId) {
        UserResponse target = UserResponse.fromDomain(userService.getUserById(targetUserId));
        if (target.getRole() == Role.ADMIN && userService.countUsersByRole(Role.ADMIN) <= 1) {
            throw new RuntimeException("Cannot lock the last ADMIN account");
        }
    }

    private void preventDeletingLastAdmin(UUID targetUserId) {
        UserResponse target = UserResponse.fromDomain(userService.getUserById(targetUserId));
        if (target.getRole() == Role.ADMIN && userService.countUsersByRole(Role.ADMIN) <= 1) {
            throw new RuntimeException("Cannot delete the last ADMIN account");
        }
    }

    private void preventSelfRoleChange(UUID targetUserId, Principal principal) {
        preventSelfAction(targetUserId, principal, "You cannot change your own role");
    }

    private void preventSelfAction(UUID targetUserId, Principal principal, String message) {
        if (principal == null || principal.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }

        UUID currentUserId = UUID.fromString(principal.getName());
        if (currentUserId.equals(targetUserId)) {
            throw new RuntimeException(message);
        }
    }

    private Page<User> resolveUsersForAdmin(Role role, UUID companyId, UUID departmentId, Pageable pageable) {
        if (companyId != null && departmentId != null && role != null) {
            // Không có method combo 3 điều kiện trong service hiện tại => chặn để tránh trả sai dữ liệu.
            throw new AccessDeniedException("Unsupported filter combination. Use companyId+departmentId or companyId+role or role only.");
        }
        if (companyId != null && departmentId != null) {
            return userService.getUsersByCompanyIdAndDepartmentId(companyId, departmentId, pageable);
        }
        if (companyId != null && role != null) {
            return userService.getUsersByCompanyIdAndRole(companyId, role, pageable);
        }
        if (companyId != null) {
            return userService.getUsersByCompanyId(companyId, pageable);
        }
        if (role != null) {
            return userService.getUsersByRole(role, pageable);
        }
        return userService.getAllUsers(pageable);
    }
}
