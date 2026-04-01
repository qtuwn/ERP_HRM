package com.vthr.erp_hrm.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.service.AuditLogService;
import com.vthr.erp_hrm.core.service.UserService;
import com.vthr.erp_hrm.infrastructure.controller.request.UpdateUserRoleRequest;
import com.vthr.erp_hrm.infrastructure.persistence.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

        @Mock
        private UserService userService;

        @Mock
        private AuditLogService auditLogService;

        @Mock
        private CompanyRepository companyRepository;

        @InjectMocks
        private AdminUserController adminUserController;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;
        private UUID adminId;

        @BeforeEach
        void setup() {
                this.mockMvc = MockMvcBuilders.standaloneSetup(adminUserController)
                                .setControllerAdvice(new com.vthr.erp_hrm.infrastructure.config.GlobalExceptionHandler())
                                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                                .build();
                this.objectMapper = new ObjectMapper();
                this.adminId = UUID.randomUUID();
        }

        private void stubAdminUser() {
                when(userService.getUserById(adminId))
                                .thenReturn(user(adminId, "admin@vthr.com", Role.ADMIN, true, "Admin"));
        }

        @Test
        void getUsers_shouldReturnPagedUsers() throws Exception {
                User admin = user(UUID.randomUUID(), "admin2@vthr.com", Role.ADMIN, true, "Admin User");
                User hr = user(UUID.randomUUID(), "hr@vthr.com", Role.HR, true, "HR User");

                when(userService.getAllUsers(any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(admin, hr), PageRequest.of(0, 2), 2));
                when(companyRepository.findAllById(any(Iterable.class))).thenReturn(List.of());

                mockMvc.perform(get("/api/admin/users?page=0&size=2")
                                .principal(() -> adminId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        void getUsers_withRoleFilter_shouldReturnFilteredPagedUsers() throws Exception {
                User hr = user(UUID.randomUUID(), "hr@vthr.com", Role.HR, true, "HR User");

                when(userService.getUsersByRole(eq(Role.HR), any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(hr), PageRequest.of(0, 20), 1));
                when(companyRepository.findAllById(any(Iterable.class))).thenReturn(List.of());

                mockMvc.perform(get("/api/admin/users?role=HR")
                                .principal(() -> adminId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.content.length()").value(1))
                                .andExpect(jsonPath("$.data.content[0].role").value("HR"));

                verify(userService).getUsersByRole(eq(Role.HR), any(Pageable.class));
        }

        @Test
        void getUserById_shouldReturnUserDetails() throws Exception {
                UUID userId = UUID.randomUUID();
                when(userService.getUserById(userId))
                                .thenReturn(user(userId, "candidate@vthr.com", Role.CANDIDATE, true, "Candidate User"));

                mockMvc.perform(get("/api/admin/users/{id}", userId)
                                .principal(() -> adminId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                                .andExpect(jsonPath("$.data.email").value("candidate@vthr.com"));
        }

        @Test
        void updateRole_shouldUpdateUserRole() throws Exception {
                UUID targetId = UUID.randomUUID();

                UpdateUserRoleRequest request = new UpdateUserRoleRequest();
                request.setRole(Role.HR);

                when(userService.getUserById(targetId))
                                .thenReturn(user(targetId, "user@vthr.com", Role.CANDIDATE, true, "Updated User"));
                when(userService.updateUserRole(targetId, Role.HR))
                                .thenReturn(user(targetId, "user@vthr.com", Role.HR, true, "Updated User"));

                mockMvc.perform(patch("/api/admin/users/{id}/role", targetId)
                                .principal(() -> adminId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.role").value("HR"));

                verify(auditLogService).logAction(eq(adminId), eq("UPDATE_USER_ROLE"), eq("User"), eq(targetId), any(String.class));
        }

        @Test
        void lockAndUnlock_shouldToggleActiveState() throws Exception {
                UUID targetId = UUID.randomUUID();

                when(userService.getUserById(targetId))
                                .thenReturn(user(targetId, "user@vthr.com", Role.CANDIDATE, true, "Lockable User"));
                when(userService.setUserActive(targetId, false))
                                .thenReturn(user(targetId, "user@vthr.com", Role.CANDIDATE, false, "Locked User"));
                when(userService.setUserActive(targetId, true))
                                .thenReturn(user(targetId, "user@vthr.com", Role.CANDIDATE, true, "Unlocked User"));

                mockMvc.perform(patch("/api/admin/users/{id}/lock", targetId)
                                .principal(() -> adminId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.active").value(false));

                mockMvc.perform(patch("/api/admin/users/{id}/unlock", targetId)
                                .principal(() -> adminId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.active").value(true));

                verify(auditLogService).logAction(eq(adminId), eq("LOCK_USER"), eq("User"), eq(targetId), any(String.class));
                verify(auditLogService).logAction(eq(adminId), eq("UNLOCK_USER"), eq("User"), eq(targetId), any(String.class));
        }

        @Test
        void deleteUser_shouldReturnSuccess() throws Exception {
                UUID targetId = UUID.randomUUID();

                when(userService.getUserById(targetId))
                                .thenReturn(user(targetId, "user@vthr.com", Role.CANDIDATE, true, "Deletable User"));
                doNothing().when(userService).deleteUser(targetId);

                mockMvc.perform(delete("/api/admin/users/{id}", targetId)
                                .principal(() -> adminId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                verify(userService).deleteUser(targetId);
                verify(auditLogService).logAction(eq(adminId), eq("DELETE_USER"), eq("User"), eq(targetId), any(String.class));
        }

        @Test
        void lockUser_shouldRejectSelfAction() throws Exception {
                mockMvc.perform(patch("/api/admin/users/{id}/lock", adminId)
                                .principal(() -> adminId.toString()))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void lockUser_shouldRejectLockingLastAdmin() throws Exception {
                UUID targetId = UUID.randomUUID();
                when(userService.getUserById(targetId))
                                .thenReturn(user(targetId, "admin2@vthr.com", Role.ADMIN, true, "Admin2"));
                when(userService.countUsersByRole(Role.ADMIN)).thenReturn(1L);

                mockMvc.perform(patch("/api/admin/users/{id}/lock", targetId)
                                .principal(() -> adminId.toString()))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void deleteUser_shouldRejectDeletingLastAdmin() throws Exception {
                UUID targetId = UUID.randomUUID();
                when(userService.getUserById(targetId))
                                .thenReturn(user(targetId, "admin2@vthr.com", Role.ADMIN, true, "Admin2"));
                when(userService.countUsersByRole(Role.ADMIN)).thenReturn(1L);

                mockMvc.perform(delete("/api/admin/users/{id}", targetId)
                                .principal(() -> adminId.toString()))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void updateRole_shouldRejectDemotingLastAdmin() throws Exception {
                UUID targetId = UUID.randomUUID();
                UpdateUserRoleRequest request = new UpdateUserRoleRequest();
                request.setRole(Role.HR);

                when(userService.getUserById(targetId))
                                .thenReturn(user(targetId, "admin2@vthr.com", Role.ADMIN, true, "Admin2"));
                when(userService.countUsersByRole(Role.ADMIN)).thenReturn(1L);

                mockMvc.perform(patch("/api/admin/users/{id}/role", targetId)
                                .principal(() -> adminId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false));
        }

        private User user(UUID id, String email, Role role, boolean active, String fullName) {
                return User.builder()
                                .id(id)
                                .email(email)
                                .role(role)
                                .isActive(active)
                                .fullName(fullName)
                                .emailVerified(true)
                                .build();
        }
}
