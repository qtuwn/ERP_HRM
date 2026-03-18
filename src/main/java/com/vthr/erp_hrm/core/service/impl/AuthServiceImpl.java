package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.AuthTokens;
import com.vthr.erp_hrm.core.model.RefreshToken;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.RefreshTokenRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.core.service.AuthService;
import com.vthr.erp_hrm.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    @Override
    public AuthTokens login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
                
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        if (!user.isActive()) {
            throw new RuntimeException("User is disabled");
        }

        String accessToken = jwtService.generateAccessToken(user.getId().toString(), user.getRole());
        String refreshTokenStr = jwtService.generateRefreshToken();
        String tokenHash = jwtService.hashToken(refreshTokenStr);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(ZonedDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS))
                .revoked(false)
                .build();
                
        refreshTokenRepository.save(refreshToken);
        
        return AuthTokens.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .user(user)
                .build();
    }

    @Override
    public AuthTokens refreshToken(String refreshTokenStr) {
        String tokenHash = jwtService.hashToken(refreshTokenStr);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
                
        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new RuntimeException("Invalid or expired refresh token");
        }
        
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        
        String newAccessToken = jwtService.generateAccessToken(user.getId().toString(), user.getRole());
        String newRefreshTokenStr = jwtService.generateRefreshToken();
        String newTokenHash = jwtService.hashToken(newRefreshTokenStr);
        
        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(newTokenHash)
                .expiresAt(ZonedDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS))
                .revoked(false)
                .build();
                
        refreshTokenRepository.save(newRefreshToken);
        
        return AuthTokens.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenStr)
                .user(user)
                .build();
    }

    @Override
    public void logout(String refreshTokenStr) {
        String tokenHash = jwtService.hashToken(refreshTokenStr);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    public User register(String email, String password, String fullName, String phone) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(com.vthr.erp_hrm.core.model.Role.CANDIDATE)
                .isActive(true)
                .mustChangePassword(false)
                .fullName(fullName)
                .phone(phone)
                .emailVerified(true) // Demo mode setup
                .build();
        return userRepository.save(user);
    }

    @Override
    public void changePassword(java.util.UUID userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
