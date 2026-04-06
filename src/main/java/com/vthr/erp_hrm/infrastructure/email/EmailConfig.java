package com.vthr.erp_hrm.infrastructure.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Hàng đợi Redis + {@link EmailWorker} gửi mail qua {@link JavaMailSender}.
 * Cấu hình SMTP dùng properties chuẩn Spring Boot (giống spring-boot-starter-mail + Gmail/Mailtrap):
 * {@code spring.mail.host}, {@code spring.mail.port}, {@code spring.mail.username},
 * {@code spring.mail.password}, {@code spring.mail.properties.mail.smtp.auth},
 * {@code spring.mail.properties.mail.smtp.starttls.enable}, v.v.
 *
 * @see <a href="https://github.com/amaanalikhan3000/EmailOtp">EmailOtp (Gmail + OTP)</a>
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableAsync
public class EmailConfig {

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

    /**
     * Log cấu hình mail khi khởi động (không chặn start nếu chưa set SMTP — gửi OTP sẽ lỗi lúc gửi).
     */
    @Bean
    public ApplicationRunner mailStartupLogger(Environment env, JavaMailSender javaMailSender) {
        return args -> {
            String host = env.getProperty("spring.mail.host", "");
            String port = env.getProperty("spring.mail.port", "");
            String user = env.getProperty("spring.mail.username", "");
            String pass = env.getProperty("spring.mail.password", "");
            boolean passOk = pass != null && !pass.isBlank();

            log.info("JavaMailSender bean: {}", javaMailSender.getClass().getName());
            log.info(
                    "SMTP (OTP / xác thực email): host='{}' port={} username='{}' đã_có_mật_khẩu={}",
                    host, port, user, passOk);
            log.info(
                    "SMTP props: auth={} starttls.enable={} starttls.required={} ssl.trust={}",
                    env.getProperty("spring.mail.properties.mail.smtp.auth"),
                    env.getProperty("spring.mail.properties.mail.smtp.starttls.enable"),
                    env.getProperty("spring.mail.properties.mail.smtp.starttls.required"),
                    env.getProperty("spring.mail.properties.mail.smtp.ssl.trust"));

            if (!passOk || user == null || user.isBlank()) {
                log.warn(
                        "Chưa cấu hình đủ SMTP (MAIL_USER/MAIL_PASS hoặc SMTP_USER/SMTP_PASS). "
                                + "Đăng ký / quên mật khẩu sẽ enqueue email nhưng gửi thật sẽ thất bại. "
                                + "Gmail: bật 2FA + App Password; ví dụ host=smtp.gmail.com port=587 — tham khảo EmailOtp trên GitHub.");
            }
            if (host == null || host.isBlank() || "localhost".equalsIgnoreCase(host.trim())) {
                log.warn(
                        "spring.mail.host='{}' — thường không gửi được mail thật. Đặt SMTP_HOST=smtp.gmail.com (hoặc Mailtrap). "
                                + "Dev không SMTP: có thể bật auth.email-verification-demo-log-only=true để in OTP ra log.",
                        host);
            }
        };
    }
}
