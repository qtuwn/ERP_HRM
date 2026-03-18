package com.vthr.erp_hrm.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiQueueService {

    private static final String QUEUE_KEY = "erp:ai_screening_queue";
    private final StringRedisTemplate redisTemplate;

    public void enqueueApplication(UUID applicationId) {
        redisTemplate.opsForList().rightPush(QUEUE_KEY, applicationId.toString());
    }

    public String popApplication() {
        return redisTemplate.opsForList().leftPop(QUEUE_KEY);
    }
}
