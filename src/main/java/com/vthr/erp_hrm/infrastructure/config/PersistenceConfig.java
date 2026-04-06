package com.vthr.erp_hrm.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.vthr.erp_hrm.infrastructure.persistence.repository")
public class PersistenceConfig {
}

