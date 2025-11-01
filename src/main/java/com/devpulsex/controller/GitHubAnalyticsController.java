package com.devpulsex.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devpulsex.dto.github.GithubInsightsResponse;
import com.devpulsex.model.User;
import com.devpulsex.repository.UserRepository;
import com.devpulsex.service.GitHubService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/github")
@Tag(name = "GitHub Analytics", description = "Developer productivity metrics from GitHub")
@SecurityRequirement(name = "Bearer Authentication")
public class GitHubAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(GitHubAnalyticsController.class);

    private final GitHubService gitHubService;
    private final UserRepository userRepository;

    public GitHubAnalyticsController(GitHubService gitHubService, UserRepository userRepository) {
        this.gitHubService = gitHubService;
        this.userRepository = userRepository;
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
            log.debug("Fetching GitHub insights for user: {}", userEmail);

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

            if (user.getGithubAccessToken() == null || user.getGithubAccessToken().isBlank()) {
                log.warn("User {} has no GitHub access token", userEmail);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            if (user.getGithubUsername() == null || user.getGithubUsername().isBlank()) {
                log.warn("User {} has no GitHub username", userEmail);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            GithubInsightsResponse insights = gitHubService.fetchInsights(
                    user.getGithubUsername(),
                    user.getGithubAccessToken()
            );
            
            // Add avatar URL from user profile
            if (user.getGithubAvatarUrl() != null) {
                insights.setAvatarUrl(user.getGithubAvatarUrl());
            }

            log.info("Successfully fetched GitHub insights for user: {}", userEmail);
            return ResponseEntity.ok(insights);

        } catch (Exception e) {
            log.error("Error fetching GitHub insights", e);
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
            log.debug("Fetching GitHub repositories for user: {}", userEmail);

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

            if (user.getGithubAccessToken() == null || user.getGithubAccessToken().isBlank()) {
                log.warn("User {} has no GitHub access token", userEmail);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("GitHub not connected. Please connect your GitHub account.");
            }

            var repos = gitHubService.fetchRepositories(user.getGithubAccessToken());
            
            if (repos == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to fetch repositories from GitHub");
            }

            log.info("Successfully fetched {} repositories for user: {}", repos.size(), userEmail);
            return ResponseEntity.ok(repos);

        } catch (Exception e) {
            log.error("Error fetching GitHub repositories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}
