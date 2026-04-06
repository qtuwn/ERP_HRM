package com.vthr.erp_hrm.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DOTENV_LOCAL_FILENAME = ".env.local";
    private static final String DOTENV_FILENAME = ".env";
    private static final String[] ALLOWED_PREFIXES = new String[] {
            "SMTP_",
            "MAIL_",
            "GEMINI_"
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path target = resolveDotenvFile();
        if (target == null) {
            log.warn(
                    "Không tìm thấy {} / {} (đã tìm từ user.dir={} lên các thư mục cha). "
                            + "SMTP/MAIL trong .env sẽ không được nạp — đặt Working Directory = thư mục gốc dự án hoặc export biến môi trường.",
                    DOTENV_LOCAL_FILENAME,
                    DOTENV_FILENAME,
                    Path.of(System.getProperty("user.dir")).toAbsolutePath());
            return;
        }

        Map<String, Object> props = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int idx = trimmed.indexOf('=');
                if (idx <= 0) continue;

                String key = trimmed.substring(0, idx).trim();
                String rawVal = trimmed.substring(idx + 1).trim();
                String value = unquote(rawVal);

                if (!isAllowedKey(key)) {
                    continue;
                }

                // Only set if not already present from env/system props
                if (!environment.containsProperty(key)) {
                    props.put(key, value);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to load .env file: {}", target, e);
            return;
        }

        if (props.isEmpty()) {
            return;
        }

        // Highest precedence (but below system props/env vars if they already exist)
        environment.getPropertySources().addFirst(new MapPropertySource("dotenvFile", props));
        log.info("Loaded {} properties from {}", props.size(), target);
    }

    /**
     * Tìm .env.local / .env từ user.dir và đi lên tối đa vài cấp (IDE đôi khi chạy với cwd = frontend, module con…).
     */
    private Path resolveDotenvFile() {
        Path dir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (int depth = 0; depth < 10; depth++) {
            Path local = dir.resolve(DOTENV_LOCAL_FILENAME);
            if (Files.exists(local) && Files.isRegularFile(local)) {
                return local;
            }
            Path env = dir.resolve(DOTENV_FILENAME);
            if (Files.exists(env) && Files.isRegularFile(env)) {
                return env;
            }
            Path parent = dir.getParent();
            if (parent == null || parent.equals(dir)) {
                break;
            }
            dir = parent;
        }
        return null;
    }

    private boolean isAllowedKey(String key) {
        if (key == null || key.isBlank()) return false;
        for (String prefix : ALLOWED_PREFIXES) {
            if (key.startsWith(prefix)) return true;
        }
        return false;
    }

    private String unquote(String raw) {
        if (raw == null) return null;
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

