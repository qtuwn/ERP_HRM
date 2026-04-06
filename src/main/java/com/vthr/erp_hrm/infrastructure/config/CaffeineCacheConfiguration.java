package com.vthr.erp_hrm.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CaffeineCacheConfiguration {

    /** Cache cho tin tuyển dụng public (GET theo id, chỉ OPEN). */
    public static final String CACHE_PUBLIC_JOB_BY_ID = "publicJobs";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CACHE_PUBLIC_JOB_BY_ID);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterWrite(Duration.ofMinutes(5)));
        return manager;
    }
}
