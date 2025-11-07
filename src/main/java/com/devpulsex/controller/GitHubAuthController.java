package com.devpulsex.controller;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devpulsex.config.security.JwtUtil;
import com.devpulsex.dto.auth.AuthResponse;
import com.devpulsex.dto.github.GitHubAuthRequest;
import com.devpulsex.dto.github.GitHubTokenResponse;
import com.devpulsex.dto.github.GitHubUserProfile;
import com.devpulsex.dto.user.UserDto;
import com.devpulsex.model.Role;
import com.devpulsex.model.User;
import com.devpulsex.repository.UserRepository;
import com.devpulsex.service.GitHubOAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "GitHub OAuth", description = "Authenticate via GitHub and receive app JWT")
public class GitHubAuthController {

    private static final Logger log = LoggerFactory.getLogger(GitHubAuthController.class);

    private final GitHubOAuthService oAuthService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public GitHubAuthController(GitHubOAuthService oAuthService,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                JwtUtil jwtUtil) {
        this.oAuthService = oAuthService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/github")
    @Operation(summary = "Exchange GitHub code for JWT and user info")
    public ResponseEntity<AuthResponse> githubLogin(@Valid @RequestBody GitHubAuthRequest request) {
        try {
            log.info("Received GitHub OAuth request: {}", request);
            log.info("GitHub OAuth login attempt with code={}", request.getCode());
            GitHubTokenResponse tokenResp = oAuthService.exchangeCodeForToken(request.getCode());
            if (tokenResp == null || tokenResp.getAccessToken() == null) {
                log.warn("GitHub token exchange returned null or no access token");
                return ResponseEntity.badRequest().build();
            }
            GitHubUserProfile profile = oAuthService.fetchUserProfile(tokenResp.getAccessToken());
            if (profile == null || profile.getId() == null) {
                log.warn("GitHub user profile fetch returned null or missing ID");
                return ResponseEntity.badRequest().build();
            }
            log.info("GitHub user profile retrieved: id={}, login={}", profile.getId(), profile.getLogin());

            String email = profile.getEmail();
            if (email == null || email.isBlank()) {
                // Try fetching primary email via /user/emails (requires user:email or repo scope)
                String fetched = oAuthService.fetchPrimaryEmail(tokenResp.getAccessToken());
                email = (fetched != null && !fetched.isBlank()) ? fetched : profile.getLogin() + "@users.noreply.github.com";
            }
            String name = (profile.getName() != null && !profile.getName().isBlank()) ? profile.getName() : profile.getLogin();

            // Find existing user by GitHub ID or by email
            User user = userRepository.findByGithubId(profile.getId()).orElse(null);
            if (user == null) {
                user = userRepository.findByEmail(email).orElse(null);
            }

            if (user == null) {
                user = User.builder()
                        .name(name)
                        .email(email)
                        .password(passwordEncoder.encode(randomPassword()))
                        .role(Role.DEVELOPER)
                        .githubId(profile.getId())
                        .githubUsername(profile.getLogin())
                        .githubAvatarUrl(profile.getAvatarUrl())
                        .githubAccessToken(tokenResp.getAccessToken())
                        .build();
                log.info("Created new user from GitHub OAuth: email={}", email);
            } else {
                // Update existing user with GitHub OAuth data (preserving Google data if exists)
                user.setName(name);
                user.setEmail(email);
                user.setGithubId(profile.getId());
                user.setGithubUsername(profile.getLogin());
                user.setGithubAvatarUrl(profile.getAvatarUrl());
                user.setGithubAccessToken(tokenResp.getAccessToken());
                log.info("Linked GitHub account to existing user: email={}, hasGoogle={}", 
                    email, user.getGoogleId() != null);
            }

            userRepository.save(user);

            String jwt = jwtUtil.generateToken(user.getEmail(), Map.of("role", user.getRole().name()));
            return ResponseEntity.ok(AuthResponse.builder()
                    .token(jwt)
                    .user(UserDto.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .build())
                    .build());
        } catch (Exception ex) {
            log.error("GitHub OAuth login failed", ex);
            // Avoid leaking secrets; provide a generic response
            return ResponseEntity.status(502).build();
        }
    }

    private String randomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
