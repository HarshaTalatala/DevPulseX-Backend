package com.devpulsex.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devpulsex.config.security.JwtUtil;
import com.devpulsex.dto.auth.AuthResponse;
import com.devpulsex.dto.trello.TrelloAuthRequest;
import com.devpulsex.dto.trello.TrelloMemberProfile;
import com.devpulsex.model.User;
import com.devpulsex.repository.UserRepository;
import com.devpulsex.service.TrelloOAuthService;
import com.devpulsex.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Trello OAuth", description = "Link Trello account to DevPulseX user")
public class TrelloAuthController {

    private static final Logger log = LoggerFactory.getLogger(TrelloAuthController.class);

    private final TrelloOAuthService oAuthService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public TrelloAuthController(TrelloOAuthService oAuthService,
                                UserRepository userRepository,
                                JwtUtil jwtUtil,
                                UserService userService) {
        this.oAuthService = oAuthService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/trello/callback")
    @Operation(summary = "Handle Trello OAuth callback")
    public ResponseEntity<AuthResponse> trelloCallback(
            @RequestParam("oauth_token") String oauthToken,
            @RequestParam("oauth_verifier") String oauthVerifier,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                log.error("Unauthorized: Authentication is null or not authenticated");
                return ResponseEntity.status(401).build();
            }

            String userEmail = authentication.getName();
            log.info("Trello OAuth callback for user: {}", userEmail);

            // Exchange OAuth token and verifier for access token
            String accessToken = oAuthService.exchangeCodeForToken(oauthToken, oauthVerifier);
            
            if (accessToken == null) {
                log.error("Failed to exchange Trello OAuth token");
                return ResponseEntity.badRequest().build();
            }

            // Fetch Trello member profile
            TrelloMemberProfile profile = oAuthService.getMemberProfile(accessToken);
            
            if (profile == null || profile.getId() == null) {
                log.error("Failed to fetch Trello profile");
                return ResponseEntity.badRequest().build();
            }

            // Find the authenticated user
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

            // Update user with Trello OAuth data
            user.setTrelloId(profile.getId());
            user.setTrelloUsername(profile.getUsername());
            user.setTrelloAccessToken(accessToken);
            
            log.info("Linked Trello account to user: email={}, trelloId={}, trelloUsername={}", 
                    userEmail, profile.getId(), profile.getUsername());

            userRepository.save(user);

            // Generate new JWT token with updated user info
            String jwt = jwtUtil.generateToken(user.getEmail(), Map.of("role", user.getRole().name()));

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(jwt)
                    .user(userService.toDto(user))
                    .build());

        } catch (Exception e) {
            log.error("Trello OAuth callback failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(AuthResponse.builder()
                    .token(null)
                    .user(null)
                    .build());
        }
    }

    @PostMapping("/trello/link")
    @Operation(summary = "Link Trello account to authenticated user (manual token)")
    public ResponseEntity<AuthResponse> linkTrelloAccount(
            @Valid @RequestBody TrelloAuthRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String userEmail = authentication.getName();
            log.info("Trello account linking attempt for user: {}", userEmail);

            // Fetch Trello member profile
            TrelloMemberProfile profile = oAuthService.getMemberProfile(request.getToken());
            
            if (profile == null || profile.getId() == null) {
                log.error("Failed to fetch Trello profile");
                return ResponseEntity.badRequest().build();
            }

            // Find the authenticated user
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

            // Update user with Trello OAuth data
            user.setTrelloId(profile.getId());
            user.setTrelloUsername(profile.getUsername());
            user.setTrelloAccessToken(request.getToken());
            
            log.info("Linked Trello account to user: email={}, trelloId={}, trelloUsername={}", 
                    userEmail, profile.getId(), profile.getUsername());

            userRepository.save(user);

            // Generate new JWT token with updated user info
            String jwt = jwtUtil.generateToken(user.getEmail(), Map.of("role", user.getRole().name()));

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(jwt)
                    .user(userService.toDto(user))
                    .build());

        } catch (Exception e) {
            log.error("Trello account linking failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(AuthResponse.builder()
                    .token(null)
                    .user(null)
                    .build());
        }
    }
}
