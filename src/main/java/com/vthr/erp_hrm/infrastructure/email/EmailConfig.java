package com.vthr.erp_hrm.infrastructure.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean smtpStartTlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
    private boolean smtpStartTlsRequired;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust:}")
    private String smtpSslTrust;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:5000}")
    private String smtpConnectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout:5000}")
    private String smtpTimeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:5000}")
    private String smtpWriteTimeout;

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
    public JavaMailSender javaMailSender() {
        log.info("Configuring JavaMailSender: host='{}' port={} username='{}'", mailHost, mailPort, mailUsername);

        validateMailConfig();

        if (mailHost.isBlank() || "localhost".equalsIgnoreCase(mailHost.trim())) {
            log.warn("MAIL_HOST is '{}' - emails will likely fail in Docker. Set MAIL_HOST to your real SMTP host.", mailHost);
        }

        if (!smtpAuth) {
            log.warn("SMTP auth is disabled (spring.mail.properties.mail.smtp.auth=false). Use only in trusted environments.");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailHost.trim());
        sender.setPort(mailPort);
        sender.setUsername(mailUsername.trim());
        sender.setPassword(mailPassword);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(smtpAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtpStartTlsEnable));
        props.put("mail.smtp.starttls.required", String.valueOf(smtpStartTlsRequired));
        if (smtpSslTrust != null && !smtpSslTrust.isBlank()) {
            props.put("mail.smtp.ssl.trust", smtpSslTrust);
        }
        props.put("mail.smtp.connectiontimeout", smtpConnectionTimeout);
        props.put("mail.smtp.timeout", smtpTimeout);
        props.put("mail.smtp.writetimeout", smtpWriteTimeout);

        return sender;
    }

    @Bean
    public org.springframework.boot.ApplicationRunner mailStartupLogger(JavaMailSender javaMailSender) {
        return args -> {
            String maskedPass = (mailPassword == null || mailPassword.isBlank()) ? "" : "******";

            log.info("MailSender initialized: {}", javaMailSender.getClass().getName());
            log.info("SMTP config: host='{}' port={} username='{}' password='{}'", mailHost, mailPort, mailUsername, maskedPass);
            log.info("SMTP props: auth={} starttls.enable={} starttls.required={} ssl.trust={}",
                    smtpAuth,
                    smtpStartTlsEnable,
                    smtpStartTlsRequired,
                    smtpSslTrust);
        };
    }

    private void validateMailConfig() {
        if (mailPort <= 0) {
            throw new IllegalStateException("Mail port is invalid. Set spring.mail.port (or MAIL_PORT/SMTP_PORT) to a positive number.");
        }

        if (mailHost == null || mailHost.isBlank()) {
            throw new IllegalStateException("Mail host is missing. Set spring.mail.host (or MAIL_HOST/SMTP_HOST).");
        }

        if (smtpAuth) {
            if (mailUsername == null || mailUsername.isBlank()) {
                throw new IllegalStateException("Mail username is missing. Set spring.mail.username (or MAIL_USER/SMTP_USER).");
            }

            if (mailPassword == null || mailPassword.isBlank()) {
                throw new IllegalStateException("Mail password is missing. Set spring.mail.password (or MAIL_PASS/SMTP_PASS).");
            }
        }
    }
}
