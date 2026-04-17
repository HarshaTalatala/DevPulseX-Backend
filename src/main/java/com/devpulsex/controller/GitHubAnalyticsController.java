package com.devpulsex.controller;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devpulsex.dto.github.GithubInsightsResponse;
import com.devpulsex.integration.oauth.OAuthTokenEncryptor;
import com.devpulsex.model.User;
import com.devpulsex.repository.UserRepository;
import com.devpulsex.service.ResilientGitHubService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/github")
@Tag(name = "GitHub Analytics", description = "Developer productivity metrics from GitHub")
@SecurityRequirement(name = "Bearer Authentication")
public class GitHubAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(GitHubAnalyticsController.class);

    private final ResilientGitHubService resilientGitHubService;
    private final UserRepository userRepository;
    private final OAuthTokenEncryptor tokenEncryptor;

    public GitHubAnalyticsController(ResilientGitHubService resilientGitHubService,
                                     UserRepository userRepository,
                                     OAuthTokenEncryptor tokenEncryptor) {
        this.resilientGitHubService = resilientGitHubService;
        this.userRepository = userRepository;
        this.tokenEncryptor = tokenEncryptor;
    }

    @GetMapping("/insights")
    @Operation(summary = "Get GitHub analytics for authenticated user", 
               description = "Fetches developer productivity metrics including repos, commits, and PRs")
    public ResponseEntity<GithubInsightsResponse> getInsights(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                log.warn("Unauthorized access attempt to /api/github/insights");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

            if (user.getGithubAccessToken() == null || user.getGithubAccessToken().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            if (user.getGithubUsername() == null || user.getGithubUsername().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            String githubToken = tokenEncryptor.decryptLenient(user.getGithubAccessToken());
            if (githubToken == null || githubToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Use resilient service with automatic cache fallback
            GithubInsightsResponse insights = resilientGitHubService.fetchInsightsWithFallback(
                    user.getGithubUsername(),
                    githubToken
            );
            
            // Add avatar URL from user profile
            if (user.getGithubAvatarUrl() != null) {
                insights.setAvatarUrl(user.getGithubAvatarUrl());
            }

            log.info("GitHub insights fetch succeeded");
            
            // Add cache-control headers for browser caching (5 minutes)
            // Also enable compression via Accept-Encoding header
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES)
                            .cachePrivate() // Private cache (user-specific)
                            .mustRevalidate())
                    .header(HttpHeaders.VARY, "Accept-Encoding") // Ensure proper cache with compression
                    .body(insights);

        } catch (Exception e) {
            log.error("GitHub insights fetch failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/repositories")
    @Operation(summary = "Get GitHub repositories for authenticated user", 
               description = "Fetches all repositories from the authenticated user's GitHub account")
    public ResponseEntity<?> getRepositories(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                log.warn("Unauthorized access attempt to /api/github/repositories");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

            if (user.getGithubAccessToken() == null || user.getGithubAccessToken().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Authentication failed");
            }

            String githubToken = tokenEncryptor.decryptLenient(user.getGithubAccessToken());
            if (githubToken == null || githubToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Authentication failed");
            }

            // Use resilient service with automatic cache fallback
            var repos = resilientGitHubService.fetchRepositoriesWithFallback(githubToken);
            
            if (repos == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("External integration failed");
            }

            log.info("GitHub repositories fetch succeeded");
            
            // Add cache-control headers (10 minutes - repositories change less frequently)
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES)
                            .cachePrivate()
                            .mustRevalidate())
                    .header(HttpHeaders.VARY, "Accept-Encoding") // Ensure proper cache with compression
                    .body(repos);

        } catch (Exception e) {
            log.error("GitHub repositories fetch failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("External integration failed");
        }
    }
}
