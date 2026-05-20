package com.devpulsex.config.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class OAuthCookieSecurityResolver {

    private final Environment environment;
    private final Boolean forceSecureCookies;

    public OAuthCookieSecurityResolver(
            Environment environment,
            @Value("${app.cookies.force-secure:#{null}}") @Nullable Boolean forceSecureCookies) {
        this.environment = environment;
        this.forceSecureCookies = forceSecureCookies;
    }

    public boolean shouldUseSecureCookies(@NonNull HttpServletRequest request) {
        if (request.isSecure() || isForwardedHttps(request)) {
            return true;
        }

        if (forceSecureCookies != null) {
            return forceSecureCookies;
        }

        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch("prod"::equalsIgnoreCase);
    }

    private boolean isForwardedHttps(@NonNull HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto == null || forwardedProto.isBlank()) {
            return false;
        }

        return Arrays.stream(forwardedProto.split(","))
                .map(String::trim)
                .anyMatch("https"::equalsIgnoreCase);
    }
}
