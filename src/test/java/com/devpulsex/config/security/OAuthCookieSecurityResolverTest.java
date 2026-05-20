package com.devpulsex.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthCookieSecurityResolverTest {

    @Test
    void shouldUseSecureCookies_shouldReturnTrue_whenForwardedProtoIsHttps() {
        OAuthCookieSecurityResolver resolver = new OAuthCookieSecurityResolver(new MockEnvironment(), false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");

        assertTrue(resolver.shouldUseSecureCookies(request));
    }

    @Test
    void shouldUseSecureCookies_shouldReturnTrueByDefault_whenProdProfileActive() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        OAuthCookieSecurityResolver resolver = new OAuthCookieSecurityResolver(environment, null);

        assertTrue(resolver.shouldUseSecureCookies(new MockHttpServletRequest()));
    }

    @Test
    void shouldUseSecureCookies_shouldHonorFalseOverride_whenNotHttpsAndNotProd() {
        OAuthCookieSecurityResolver resolver = new OAuthCookieSecurityResolver(new MockEnvironment(), false);

        assertFalse(resolver.shouldUseSecureCookies(new MockHttpServletRequest()));
    }
}
