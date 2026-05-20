package com.devpulsex.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class CookieSecuritySupport {

    private final boolean forceSecureCookies;

    public CookieSecuritySupport(@Value("${app.cookies.force-secure:false}") boolean forceSecureCookies) {
        this.forceSecureCookies = forceSecureCookies;
    }

    public boolean isSecure(HttpServletRequest request) {
        return forceSecureCookies
                || request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }
}