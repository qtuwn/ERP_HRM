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
        Path workDir = Path.of(System.getProperty("user.dir"));
        Path dotenvLocalPath = workDir.resolve(DOTENV_LOCAL_FILENAME);
        Path dotenvPath = workDir.resolve(DOTENV_FILENAME);

        // Priority: .env.local (dev) > .env
        Path target = Files.exists(dotenvLocalPath) && Files.isRegularFile(dotenvLocalPath)
                ? dotenvLocalPath
                : dotenvPath;

        if (!Files.exists(target) || !Files.isRegularFile(target)) {
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
            log.warn("Failed to load .env file: {}", dotenvPath, e);
            return;
        }

        if (props.isEmpty()) {
            return;
        }

        // Highest precedence (but below system props/env vars if they already exist)
        environment.getPropertySources().addFirst(new MapPropertySource("dotenvFile", props));
        log.info("Loaded {} properties from {}", props.size(), target);
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

