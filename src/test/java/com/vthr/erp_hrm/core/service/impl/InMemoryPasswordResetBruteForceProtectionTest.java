package com.vthr.erp_hrm.core.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPasswordResetBruteForceProtectionTest {

    private Clock clock;
    private InMemoryPasswordResetBruteForceProtection protection;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-04-02T12:00:00Z"), ZoneOffset.UTC);
        protection = new InMemoryPasswordResetBruteForceProtection(
                clock,
                3,
                3600,
                5,
                3600,
                2,
                600
        );
    }

    @Test
    void requestIp_thirdOk_fourthBlocked() {
        protection.assertForgotPasswordRequestAllowed("1.1.1.1");
        protection.assertForgotPasswordRequestAllowed("1.1.1.1");
        protection.assertForgotPasswordRequestAllowed("1.1.1.1");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> protection.assertForgotPasswordRequestAllowed("1.1.1.1"));
        assertTrue(ex.getMessage().contains("Rate limit"));
    }

    @Test
    void emailLockout_afterTwoFailures() {
        protection.assertEmailNotLockedForPasswordReset("u@x.com");
        protection.recordPasswordResetConfirmFailure("u@x.com");
        protection.assertEmailNotLockedForPasswordReset("u@x.com");
        protection.recordPasswordResetConfirmFailure("u@x.com");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> protection.assertEmailNotLockedForPasswordReset("u@x.com"));
        assertTrue(ex.getMessage().contains("tam thoi bi khoa"));
    }

    @Test
    void clearFailures_removesLockState() {
        protection.recordPasswordResetConfirmFailure("a@b.c");
        protection.recordPasswordResetConfirmFailure("a@b.c");
        assertThrows(RuntimeException.class, () -> protection.assertEmailNotLockedForPasswordReset("a@b.c"));
        protection.clearPasswordResetConfirmFailures("a@b.c");
        protection.assertEmailNotLockedForPasswordReset("a@b.c");
    }
}
