package com.vthr.erp_hrm.infrastructure.bootstrap;

import com.vthr.erp_hrm.core.model.AccountStatus;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Locale;

/**
 * Bootstrap 1 tài khoản ADMIN cho môi trường dev khi DB còn trống (chưa seed).
 * Bật bằng: app.bootstrap.admin.enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.bootstrap.admin.enabled", havingValue = "true", matchIfMissing = false)
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.email:admin@vthr.com}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.full-name:System Admin}")
    private String adminFullName;

    @Override
    public void run(ApplicationArguments args) {
        String email = (adminEmail == null ? "" : adminEmail.trim().toLowerCase(Locale.ROOT));
        if (email.isBlank()) {
            log.warn("Admin bootstrap skipped: app.bootstrap.admin.email is blank");
            return;
        }

        // Ưu tiên đảm bảo 1 tài khoản login được theo email chỉ định.
        // (Tránh trường hợp DB đã có record admin@vthr.com nhưng password/emailVerified/status sai do migration cũ.)
        userRepository.findByEmail(email).ifPresentOrElse(existing -> {
            ZonedDateTime now = ZonedDateTime.now();
            existing.setRole(Role.ADMIN);
            existing.setPasswordHash(passwordEncoder.encode(adminPassword != null ? adminPassword : ""));
            existing.setStatus(AccountStatus.ACTIVE);
            existing.setActive(true);
            existing.setEmailVerified(true);
            existing.setVerifiedAt(existing.getVerifiedAt() != null ? existing.getVerifiedAt() : now);
            existing.setMustChangePassword(false);
            if (existing.getFullName() == null || existing.getFullName().isBlank()) {
                existing.setFullName((adminFullName == null || adminFullName.isBlank()) ? "System Admin" : adminFullName.trim());
            }
            existing.setUpdatedAt(now);
            userRepository.save(existing);
            log.info("Admin bootstrap updated existing ADMIN user: email={}", email);
        }, () -> {
            ZonedDateTime now = ZonedDateTime.now();
            User admin = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(adminPassword != null ? adminPassword : ""))
                    .role(Role.ADMIN)
                    .status(AccountStatus.ACTIVE)
                    .isActive(true)
                    .mustChangePassword(false)
                    .fullName((adminFullName == null || adminFullName.isBlank()) ? "System Admin" : adminFullName.trim())
                    .emailVerified(true)
                    .verifiedAt(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            userRepository.save(admin);
            log.info("Admin bootstrap created default ADMIN user: email={} (password from properties)", email);
        });
    }
}

