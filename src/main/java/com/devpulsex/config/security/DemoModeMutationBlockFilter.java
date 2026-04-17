package com.devpulsex.config.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class DemoModeMutationBlockFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATION_METHODS = Set.of(
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name()
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
        protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (isBlockedDemoMutation(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(new DemoModeError(
                    Instant.now().toString(),
                    403,
                    "Forbidden",
                    "Demo mode is read-only. Mutation requests are not allowed.",
                    request.getRequestURI()
            )));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlockedDemoMutation(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/") || path.startsWith("/api/auth/")) {
            return false;
        }
        if (!MUTATION_METHODS.contains(request.getMethod())) {
            return false;
        }
        return "1".equals(request.getHeader("X-Demo-Mode"));
    }

    private record DemoModeError(
            String timestamp,
            int status,
            String error,
            String message,
            String path
    ) {}
}
