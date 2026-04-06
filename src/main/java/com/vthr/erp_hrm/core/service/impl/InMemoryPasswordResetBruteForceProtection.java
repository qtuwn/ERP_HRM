package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.service.PasswordResetBruteForceProtection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryPasswordResetBruteForceProtection implements PasswordResetBruteForceProtection {

    private final Clock clock;
    private final int requestIpMax;
    private final Duration requestIpWindow;
    private final int confirmIpMax;
    private final Duration confirmIpWindow;
    private final int maxFailuresPerEmail;
    private final Duration lockoutDuration;

    private final ConcurrentHashMap<String, Deque<Instant>> requestBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> confirmBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EmailResetState> emailStates = new ConcurrentHashMap<>();

    public InMemoryPasswordResetBruteForceProtection(
            Clock clock,
            @Value("${auth.password-reset-request-ip-max:5}") int requestIpMax,
            @Value("${auth.password-reset-request-ip-window-seconds:900}") long requestIpWindowSeconds,
            @Value("${auth.password-reset-confirm-ip-max:30}") int confirmIpMax,
            @Value("${auth.password-reset-confirm-ip-window-seconds:900}") long confirmIpWindowSeconds,
            @Value("${auth.password-reset-max-failed-confirm-per-email:5}") int maxFailuresPerEmail,
            @Value("${auth.password-reset-lockout-seconds:900}") long lockoutSeconds
    ) {
        this.clock = clock;
        this.requestIpMax = Math.max(1, requestIpMax);
        this.requestIpWindow = Duration.ofSeconds(Math.max(60, requestIpWindowSeconds));
        this.confirmIpMax = Math.max(1, confirmIpMax);
        this.confirmIpWindow = Duration.ofSeconds(Math.max(60, confirmIpWindowSeconds));
        this.maxFailuresPerEmail = Math.max(1, maxFailuresPerEmail);
        this.lockoutDuration = Duration.ofSeconds(Math.max(60, lockoutSeconds));
    }

    @Override
    public void assertForgotPasswordRequestAllowed(String clientIp) {
        assertBucket("req:" + normalizeIp(clientIp), requestBuckets, requestIpMax, requestIpWindow,
                "Rate limit exceeded (password reset request). Vui long thu lai sau.");
    }

    @Override
    public void assertForgotPasswordConfirmAllowed(String clientIp) {
        assertBucket("cfm:" + normalizeIp(clientIp), confirmBuckets, confirmIpMax, confirmIpWindow,
                "Rate limit exceeded (password reset confirm). Vui long thu lai sau.");
    }

    @Override
    public void assertEmailNotLockedForPasswordReset(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return;
        }
        String key = normalizedEmail.trim().toLowerCase();
        EmailResetState state = emailStates.get(key);
        if (state == null || state.lockoutUntil == null) {
            return;
        }
        Instant now = clock.instant();
        if (!now.isBefore(state.lockoutUntil)) {
            state.lockoutUntil = null;
            state.failures = 0;
            return;
        }
        long waitSec = Duration.between(now, state.lockoutUntil).getSeconds();
        throw new RuntimeException(
                "Dat lai mat khau tam thoi bi khoa do nhap sai nhieu lan. Vui long cho " + waitSec + " giay.");
    }

    @Override
    public void recordPasswordResetConfirmFailure(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return;
        }
        String key = normalizedEmail.trim().toLowerCase();
        emailStates.compute(key, (k, existing) -> {
            EmailResetState s = existing != null ? existing : new EmailResetState();
            Instant now = clock.instant();
            if (s.lockoutUntil != null && now.isBefore(s.lockoutUntil)) {
                return s;
            }
            if (s.lockoutUntil != null) {
                s.lockoutUntil = null;
                s.failures = 0;
            }
            s.failures++;
            if (s.failures >= maxFailuresPerEmail) {
                s.lockoutUntil = now.plus(lockoutDuration);
                s.failures = 0;
            }
            return s;
        });
    }

    @Override
    public void clearPasswordResetConfirmFailures(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return;
        }
        emailStates.remove(normalizedEmail.trim().toLowerCase());
    }

    private void assertBucket(String mapKey, ConcurrentHashMap<String, Deque<Instant>> map, int max, Duration window,
            String message) {
        Instant now = clock.instant();
        Deque<Instant> bucket = map.computeIfAbsent(mapKey, k -> new ArrayDeque<>());
        synchronized (bucket) {
            prune(bucket, now.minus(window));
            if (bucket.size() >= max) {
                throw new RuntimeException(message);
            }
            bucket.addLast(now);
        }
    }

    private static void prune(Deque<Instant> bucket, Instant cutoff) {
        while (!bucket.isEmpty()) {
            Instant first = bucket.peekFirst();
            if (first == null || first.isAfter(cutoff)) {
                break;
            }
            bucket.removeFirst();
        }
    }

    private static String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        return clientIp.trim();
    }

    private static final class EmailResetState {
        int failures;
        Instant lockoutUntil;
    }
}
