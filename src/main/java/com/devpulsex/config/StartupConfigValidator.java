package com.devpulsex.config;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Fail-fast validation for required production configuration.
 *
 * This prevents partially configured deployments from starting and failing at runtime.
 */
@Component
public class StartupConfigValidator implements ApplicationRunner {

    private final Environment environment;

    public StartupConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfileActive()) {
            return;
        }

        List<String> requiredKeys = List.of(
                "spring.datasource.url",
                "spring.datasource.username",
                "spring.datasource.password",
                "app.security.jwt.secret",
                "github.client-id",
                "github.client-secret",
                "github.redirect-uri",
                "google.client-id",
                "google.client-secret",
                "google.redirect-uri",
                "trello.encryption.secret"
        );

        List<String> missing = new ArrayList<>();
        for (String key : requiredKeys) {
            String value = environment.getProperty(Objects.requireNonNull(key));
            if (isBlank(value)) {
                missing.add(key);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required production configuration: " + String.join(", ", missing));
        }
    }

    private boolean isProdProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch("prod"::equalsIgnoreCase);
    }

    private boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
