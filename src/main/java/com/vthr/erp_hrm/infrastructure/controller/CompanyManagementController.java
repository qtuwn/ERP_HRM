package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Department;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.service.CompanyService;
import com.vthr.erp_hrm.core.service.UserService;
import com.vthr.erp_hrm.infrastructure.controller.dto.CreateDepartmentRequest;
import com.vthr.erp_hrm.infrastructure.controller.dto.CreateHrAccountRequest;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.DepartmentResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY')")
public class CompanyManagementController {

    private final UserService userService;
    private final CompanyService companyService;
    private final CompanyRepository companyRepository;

    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getStaff(
            @RequestParam(required = false) String role,
            Pageable pageable,
            Principal principal
    ) {
        User me = requireCompanyUser(principal);

        Role filterRole = role == null || role.isBlank() ? null : Role.fromString(role);
        Page<User> usersDomain = (filterRole == null
                ? userService.getUsersByCompanyId(me.getCompanyId(), pageable)
                : userService.getUsersByCompanyIdAndRole(me.getCompanyId(), filterRole, pageable))
                ;

        String myCompanyName = companyRepository.findById(me.getCompanyId())
                .map(com.vthr.erp_hrm.infrastructure.persistence.entity.CompanyEntity::getName)
                .orElse(null);

        Page<UserResponse> users = usersDomain.map(u -> UserResponse.fromDomain(u, myCompanyName));

        return ResponseEntity.ok(ApiResponse.success(users, "Fetched company staff successfully"));
    }

    @PatchMapping("/staff/{id}/lock")
    public ResponseEntity<ApiResponse<UserResponse>> lockStaff(@PathVariable UUID id, Principal principal) {
        User me = requireCompanyUser(principal);
        ensureSameCompany(me, id);
        UserResponse user = UserResponse.fromDomain(userService.setUserActive(id, false));
        return ResponseEntity.ok(ApiResponse.success(user, "Locked user successfully"));
    }

    @PatchMapping("/staff/{id}/unlock")
    public ResponseEntity<ApiResponse<UserResponse>> unlockStaff(@PathVariable UUID id, Principal principal) {
        User me = requireCompanyUser(principal);
        ensureSameCompany(me, id);
        UserResponse user = UserResponse.fromDomain(userService.setUserActive(id, true));
        return ResponseEntity.ok(ApiResponse.success(user, "Unlocked user successfully"));
    }

    @PatchMapping("/staff/{id}/department")
    public ResponseEntity<ApiResponse<UserResponse>> updateStaffDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody com.vthr.erp_hrm.infrastructure.controller.request.UpdateUserDepartmentRequest request,
            Principal principal
    ) {
        User me = requireCompanyUser(principal);
        ensureSameCompany(me, id);
        UserResponse user = UserResponse.fromDomain(userService.updateDepartment(id, request.getDepartment()));
        return ResponseEntity.ok(ApiResponse.success(user, "Updated user department successfully"));
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable UUID id, Principal principal) {
        User me = requireCompanyUser(principal);
        ensureSameCompany(me, id);
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted user successfully"));
    }

    @PostMapping("/staff/hr")
    public ResponseEntity<ApiResponse<UserResponse>> createHrAccount(
            @Valid @RequestBody CreateHrAccountRequest request,
            Principal principal
    ) {
        User me = requireCompanyUser(principal);
        User created = companyService.createHrAccount(
                me.getCompanyId(),
                request.getEmail(),
                request.getPassword(),
                request.getFullName(),
                request.getDepartmentId()
        );
        return ResponseEntity.ok(ApiResponse.success(UserResponse.fromDomain(created), "Created HR account successfully"));
    }

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getDepartments(Principal principal) {
        User me = requireCompanyUser(principal);
        List<DepartmentResponse> data = companyService.getDepartments(me.getCompanyId()).stream()
                .map(DepartmentResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data, "Fetched departments successfully"));
    }

    @PostMapping("/departments")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request,
            Principal principal
    ) {
        User me = requireCompanyUser(principal);
        Department dept = companyService.createDepartment(me.getCompanyId(), request.getName());
        return ResponseEntity.ok(ApiResponse.success(DepartmentResponse.fromDomain(dept), "Created department successfully"));
    }

    private User requireCompanyUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new AccessDeniedException("Unauthorized");
        }
        UUID userId = UUID.fromString(principal.getName());
        User me = userService.getUserById(userId);
        if (me.getRole() != Role.COMPANY) {
            throw new AccessDeniedException("Access denied");
        }
        if (me.getCompanyId() == null) {
            // Đây là case hay gặp sau khi đổi role nhưng chưa gán company_id hoặc token cũ.
            throw new AccessDeniedException("COMPANY account is missing companyId. Please re-login and ensure company_id is set in database.");
        }
        return me;
    }

    private void ensureSameCompany(User me, UUID targetUserId) {
        if (me.getId().equals(targetUserId)) {
            // không cho tự khóa/xóa chính mình qua UI staff
            throw new AccessDeniedException("You cannot modify your own account here");
        }
        User target = userService.getUserById(targetUserId);
        if (target.getCompanyId() == null || !target.getCompanyId().equals(me.getCompanyId())) {
            throw new AccessDeniedException("Access denied: user does not belong to your company");
        }
    }
}

