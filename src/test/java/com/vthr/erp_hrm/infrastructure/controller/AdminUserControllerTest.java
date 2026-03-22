package com.vthr.erp_hrm.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.service.UserService;
import com.vthr.erp_hrm.infrastructure.controller.request.UpdateUserRoleRequest;
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

    @InjectMocks
    private AdminUserController adminUserController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(adminUserController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Test
    void getUsers_shouldReturnPagedUsers() throws Exception {
        User admin = user(UUID.randomUUID(), "admin@vthr.com", Role.ADMIN, true, "Admin User");
        User hr = user(UUID.randomUUID(), "hr@vthr.com", Role.HR, true, "HR User");

        when(userService.getAllUsers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(admin, hr), PageRequest.of(0, 2), 2));

        mockMvc.perform(get("/api/admin/users?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].email").value("admin@vthr.com"));
    }

    @Test
    void getUsers_withRoleFilter_shouldReturnFilteredPagedUsers() throws Exception {
        User hr = user(UUID.randomUUID(), "hr@vthr.com", Role.HR, true, "HR User");

        when(userService.getUsersByRole(eq(Role.HR), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(hr), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/users?role=HR"))
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

        mockMvc.perform(get("/api/admin/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("candidate@vthr.com"));
    }

    @Test
    void updateRole_shouldUpdateUserRole() throws Exception {
        UUID targetId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.HR);

        when(userService.updateUserRole(targetId, Role.HR))
                .thenReturn(user(targetId, "user@vthr.com", Role.HR, true, "Updated User"));

        mockMvc.perform(patch("/api/admin/users/{id}/role", targetId)
                .principal(() -> adminId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("HR"));
    }

    @Test
    void lockAndUnlock_shouldToggleActiveState() throws Exception {
        UUID targetId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        when(userService.setUserActive(targetId, false))
                .thenReturn(user(targetId, "user@vthr.com", Role.CANDIDATE, false, "Locked User"));
        when(userService.setUserActive(targetId, true))
                .thenReturn(user(targetId, "user@vthr.com", Role.CANDIDATE, true, "Unlocked User"));

        mockMvc.perform(patch("/api/admin/users/{id}/lock", targetId)
                .principal(() -> adminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(patch("/api/admin/users/{id}/unlock", targetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void deleteUser_shouldReturnSuccess() throws Exception {
        UUID targetId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        doNothing().when(userService).deleteUser(targetId);

        mockMvc.perform(delete("/api/admin/users/{id}", targetId)
                .principal(() -> adminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).deleteUser(targetId);
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
