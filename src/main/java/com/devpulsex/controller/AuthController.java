package com.devpulsex.controller;

import java.util.Map;
import java.util.Set;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devpulsex.config.security.JwtUtil;
import com.devpulsex.config.security.OAuthCookieSecurityResolver;
import com.devpulsex.dto.auth.AuthResponse;
import com.devpulsex.dto.auth.LoginRequest;
import com.devpulsex.dto.auth.OAuthStatePrepareRequest;
import com.devpulsex.dto.auth.RegisterRequest;
import com.devpulsex.model.Role;
import com.devpulsex.model.User;
import com.devpulsex.repository.UserRepository;
import com.devpulsex.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Register and login using JWT tokens")
public class AuthController {

    private static final Set<String> SUPPORTED_OAUTH_PROVIDERS = Set.of("github", "google");

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final OAuthCookieSecurityResolver oauthCookieSecurityResolver;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          UserService userService,
                          OAuthCookieSecurityResolver oauthCookieSecurityResolver) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.oauthCookieSecurityResolver = oauthCookieSecurityResolver;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user and return JWT token")
    @SuppressWarnings("null")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "message", "An account with this email already exists. Please sign in instead.",
                "path", "/api/auth/register"
            ));
        }

        if (request.getRole() != null && request.getRole() != Role.DEVELOPER) {
            log.warn("Ignoring non-developer role during public registration");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.DEVELOPER)
                .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), Map.of("role", user.getRole().name()));
        log.info("User registration succeeded");
        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .user(userService.toDto(user))
                .build());
    }

    @PostMapping("/login")
    @Operation(summary = "Login and return JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String token = jwtUtil.generateToken(user.getEmail(), Map.of("role", user.getRole().name()));
        log.info("User login succeeded");
        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .user(userService.toDto(user))
                .build());
    }

    @PostMapping("/oauth/state/{provider}")
    @Operation(summary = "Prepare OAuth state for provider")
    public ResponseEntity<Map<String, String>> prepareOAuthState(
            @PathVariable String provider,
            @Valid @RequestBody OAuthStatePrepareRequest request,
            @NonNull HttpServletRequest httpRequest,
            @NonNull HttpServletResponse httpResponse) {
        String normalizedProvider = provider == null ? "" : provider.trim().toLowerCase();
        if (!SUPPORTED_OAUTH_PROVIDERS.contains(normalizedProvider)) {
            return ResponseEntity.badRequest().build();
        }

        String state = request.getState() == null ? "" : request.getState().trim();
        if (state.length() < 24 || state.length() > 256) {
            return ResponseEntity.badRequest().build();
        }

        boolean isSecure = oauthCookieSecurityResolver.shouldUseSecureCookies(httpRequest);
        String originHeader = httpRequest.getHeader("Origin");
        log.info("Preparing OAuth state cookie for provider={} origin={} x-forwarded-proto={} isSecure={}",
            normalizedProvider, originHeader, httpRequest.getHeader("X-Forwarded-Proto"), isSecure);

        ResponseCookie stateCookie = ResponseCookie.from("oauth_state_" + normalizedProvider, state)
            .httpOnly(true)
            .secure(isSecure)
            .sameSite("None")
            .path("/")
            .maxAge(300)
            .build();
        httpResponse.addHeader("Set-Cookie", stateCookie.toString());

        return ResponseEntity.ok(Map.of("state", state));
    }
}
