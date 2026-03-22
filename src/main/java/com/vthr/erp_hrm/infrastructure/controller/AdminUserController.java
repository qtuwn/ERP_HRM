package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.service.UserService;
import com.vthr.erp_hrm.infrastructure.controller.request.UpdateUserRoleRequest;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(required = false) Role role,
            Pageable pageable) {
        Page<UserResponse> users = (role == null
                ? userService.getAllUsers(pageable)
                : userService.getUsersByRole(role, pageable))
                .map(UserResponse::fromDomain);
        return ResponseEntity.ok(ApiResponse.success(users, "Fetched users successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse user = UserResponse.fromDomain(userService.getUserById(id));
        return ResponseEntity.ok(ApiResponse.success(user, "Fetched user successfully"));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Principal principal) {
        preventSelfRoleChange(id, principal);
        UserResponse user = UserResponse.fromDomain(userService.updateUserRole(id, request.getRole()));
        return ResponseEntity.ok(ApiResponse.success(user, "Updated user role successfully"));
    }

    @PatchMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<UserResponse>> lockUser(
            @PathVariable UUID id,
            Principal principal) {
        preventSelfAction(id, principal, "You cannot lock your own account");
        UserResponse user = UserResponse.fromDomain(userService.setUserActive(id, false));
        return ResponseEntity.ok(ApiResponse.success(user, "Locked user successfully"));
    }

    @PatchMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(@PathVariable UUID id) {
        UserResponse user = UserResponse.fromDomain(userService.setUserActive(id, true));
        return ResponseEntity.ok(ApiResponse.success(user, "Unlocked user successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID id,
            Principal principal) {
        preventSelfAction(id, principal, "You cannot delete your own account");
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted user successfully"));
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
}
