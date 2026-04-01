package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.service.ChatRateLimitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryChatRateLimitService implements ChatRateLimitService {

    private final Clock clock;
    private final int messageMax;
    private final Duration messageWindow;
    private final Duration typingCooldown;

    private final ConcurrentHashMap<String, Deque<Instant>> messageBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> typingLastSeen = new ConcurrentHashMap<>();

    public InMemoryChatRateLimitService(
            Clock clock,
            @Value("${app.chat.rate-limit.messages.max:8}") int messageMax,
            @Value("${app.chat.rate-limit.messages.window-seconds:10}") long messageWindowSeconds,
            @Value("${app.chat.rate-limit.typing.cooldown-ms:1000}") long typingCooldownMs
    ) {
        this.clock = clock;
        this.messageMax = Math.max(1, messageMax);
        this.messageWindow = Duration.ofSeconds(Math.max(1, messageWindowSeconds));
        this.typingCooldown = Duration.ofMillis(Math.max(100, typingCooldownMs));
    }

    @Override
    public void assertCanSendMessage(UUID applicationId, UUID senderId) {
        if (applicationId == null || senderId == null) {
            throw new RuntimeException("Rate limit key invalid");
        }

        Instant now = clock.instant();
        String key = msgKey(applicationId, senderId);
        Deque<Instant> bucket = messageBuckets.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (bucket) {
            prune(bucket, now.minus(messageWindow));
            if (bucket.size() >= messageMax) {
                throw new RuntimeException("Rate limit exceeded");
            }
            bucket.addLast(now);
        }
    }

    @Override
    public boolean shouldAllowTyping(UUID applicationId, UUID senderId) {
        if (applicationId == null || senderId == null) {
            return false;
        }
        Instant now = clock.instant();
        String key = typingKey(applicationId, senderId);
        Instant last = typingLastSeen.get(key);
        if (last != null && Duration.between(last, now).compareTo(typingCooldown) < 0) {
            return false;
        }
        typingLastSeen.put(key, now);
        return true;
    }

    private void prune(Deque<Instant> bucket, Instant cutoff) {
        while (!bucket.isEmpty()) {
            Instant first = bucket.peekFirst();
            if (first == null || first.isAfter(cutoff)) {
                break;
            }
            bucket.removeFirst();
        }
    }

    private String msgKey(UUID applicationId, UUID senderId) {
        return "m:" + applicationId + ":" + senderId;
    }

    private String typingKey(UUID applicationId, UUID senderId) {
        return "t:" + applicationId + ":" + senderId;
    }
}

