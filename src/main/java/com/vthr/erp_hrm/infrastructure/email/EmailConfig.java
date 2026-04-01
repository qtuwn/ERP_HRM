package com.vthr.erp_hrm.infrastructure.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Properties;
import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableScheduling
@EnableAsync
public class EmailConfig {

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:0}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Bean(name = {"taskExecutor", "applicationTaskExecutor"})
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }

    @Bean
    public JavaMailSender javaMailSender(Environment env) {
        log.info("Configuring JavaMailSender: host='{}' port={} username='{}'", mailHost, mailPort, mailUsername);

        if (mailHost == null || mailHost.isBlank() || "localhost".equals(mailHost)) {
            log.warn("MAIL_HOST is '{}' — emails will likely fail in Docker! Set MAIL_HOST=smtp.gmail.com", mailHost);
        }
        if (mailPassword == null || mailPassword.isEmpty()) {
            throw new RuntimeException(
                    "Mail Password is MISSING! Set MAIL_PASS (or SMTP_PASS) environment variable. " +
                    "Current spring.mail.password resolved to empty.");
        }
        if (mailUsername == null || mailUsername.isEmpty()) {
            throw new RuntimeException(
                    "Mail Username is MISSING! Set MAIL_USER (or SMTP_USER) environment variable.");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailHost);
        sender.setPort(mailPort);
        sender.setUsername(mailUsername);
        sender.setPassword(mailPassword);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", env.getProperty("spring.mail.properties.mail.smtp.auth", "true"));
        props.put("mail.smtp.starttls.enable", env.getProperty("spring.mail.properties.mail.smtp.starttls.enable", "true"));
        props.put("mail.smtp.starttls.required", env.getProperty("spring.mail.properties.mail.smtp.starttls.required", "true"));
        String trust = env.getProperty("spring.mail.properties.mail.smtp.ssl.trust");
        if (trust != null && !trust.isBlank()) {
            props.put("mail.smtp.ssl.trust", trust);
        }
        props.put("mail.smtp.connectiontimeout", env.getProperty("spring.mail.properties.mail.smtp.connectiontimeout", "5000"));
        props.put("mail.smtp.timeout", env.getProperty("spring.mail.properties.mail.smtp.timeout", "5000"));
        props.put("mail.smtp.writetimeout", env.getProperty("spring.mail.properties.mail.smtp.writetimeout", "5000"));

        if (!"true".equalsIgnoreCase(String.valueOf(props.get("mail.smtp.auth")))) {
            throw new RuntimeException("SMTP auth must be true (spring.mail.properties.mail.smtp.auth=true)");
        }

        return sender;
    }

    @Bean
    public org.springframework.boot.ApplicationRunner mailStartupLogger(Environment env, JavaMailSender javaMailSender) {
        return args -> {
            // Log để xác nhận MailSender bean được init và config đã load từ env/properties
            String pass = env.getProperty("spring.mail.password", "");
            String maskedPass = (pass == null || pass.isBlank()) ? "" : "******";

            log.info("MailSender initialized: {}", javaMailSender.getClass().getName());
            log.info("SMTP config: host='{}' port={} username='{}' password='{}'", mailHost, mailPort, mailUsername, maskedPass);
            log.info("SMTP props: auth={} starttls.enable={} starttls.required={} ssl.trust={}",
                    env.getProperty("spring.mail.properties.mail.smtp.auth"),
                    env.getProperty("spring.mail.properties.mail.smtp.starttls.enable"),
                    env.getProperty("spring.mail.properties.mail.smtp.starttls.required"),
                    env.getProperty("spring.mail.properties.mail.smtp.ssl.trust"));
        };
    }
}
