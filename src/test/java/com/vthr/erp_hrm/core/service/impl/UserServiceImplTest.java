package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.AccountStatus;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void updateProfile_trimsFullNameAndPhone() {
        UUID id = UUID.randomUUID();
        User existing = User.builder()
                .id(id)
                .email("a@b.com")
                .fullName("Old")
                .phone("090")
                .role(Role.CANDIDATE)
                .status(AccountStatus.ACTIVE)
                .isActive(true)
                .emailVerified(true)
                .build();
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User out = userService.updateProfile(id, "  New Name  ", " 0123 ");

        assertEquals("New Name", out.getFullName());
        assertEquals("0123", out.getPhone());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_blankFullNameAndPhone_keepsExisting() {
        UUID id = UUID.randomUUID();
        User existing = User.builder()
                .id(id)
                .email("a@b.com")
                .fullName("KeepMe")
                .phone("099")
                .role(Role.CANDIDATE)
                .status(AccountStatus.ACTIVE)
                .isActive(true)
                .emailVerified(true)
                .build();
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User out = userService.updateProfile(id, "   ", null);

        assertEquals("KeepMe", out.getFullName());
        assertEquals("099", out.getPhone());
    }

    @Test
    void getUserById_missing_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.getUserById(id));
        assertTrue(ex.getMessage().contains("User not found"));
    }
}
