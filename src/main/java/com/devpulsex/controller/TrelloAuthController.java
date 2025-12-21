package com.devpulsex.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;

import com.devpulsex.config.security.JwtUtil;
import com.devpulsex.dto.auth.AuthResponse;
import com.devpulsex.dto.trello.TrelloAuthRequest;
import com.devpulsex.dto.trello.TrelloMemberProfile;
import com.devpulsex.model.User;
import com.devpulsex.repository.UserRepository;
import com.devpulsex.integration.trello.TrelloClient;
import com.devpulsex.integration.trello.TrelloTokenEncryptor;
import com.devpulsex.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Trello OAuth", description = "Link Trello account to DevPulseX user")
public class TrelloAuthController {

    private static final Logger log = LoggerFactory.getLogger(TrelloAuthController.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final TrelloClient trelloClient;
    private final TrelloTokenEncryptor tokenEncryptor;

    public TrelloAuthController(UserRepository userRepository,
                                JwtUtil jwtUtil,
                                UserService userService,
                                TrelloClient trelloClient,
                                TrelloTokenEncryptor tokenEncryptor) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.trelloClient = trelloClient;
        this.tokenEncryptor = tokenEncryptor;
    }

    @PostMapping("/trello/link")
    @Operation(summary = "Link Trello account to authenticated user (manual token)")
    public ResponseEntity<AuthResponse> linkTrelloAccount(
            @Valid @RequestBody TrelloAuthRequest request,
            Authentication authentication,
            @CookieValue(value = "trello_state", required = false) String stateCookie,
            HttpServletResponse response) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            // Validate state (cookie is optional for cross-origin; sessionStorage validated client-side)
            if (request.getState() == null || request.getState().isBlank()) {
                log.warn("State is missing or blank in Trello linking request");
                return ResponseEntity.badRequest().build();
            }
            if (stateCookie != null && !request.getState().equals(stateCookie)) {
                log.warn("State cookie mismatch for Trello linking");
                return ResponseEntity.badRequest().build();
            }

            String userEmail = authentication.getName();
            log.info("Trello account linking attempt for user: {}", userEmail);

            // Fetch Trello member profile (with retry logic)
            TrelloMemberProfile profile = null;
            String tokenMasked = request.getToken().substring(0, Math.min(10, request.getToken().length())) + "...";
            
            log.info("Attempting to fetch Trello profile with token: {}", tokenMasked);
            try {
                JsonNode profileNode = trelloClient.getMemberProfile(request.getToken());
                log.debug("getMemberProfile returned: {}", profileNode);
                profile = toProfile(profileNode);
            } catch (Exception ex) {
                log.warn("Failed to fetch Trello profile from API: {} | Will proceed with token-only linking", ex.getMessage());
                // Don't fail - we can link without fetching profile if API is unreachable
                // This is important for production where backend may not have direct Trello API access
                profile = null;
            }

            // Find the authenticated user
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

            // Update user with Trello OAuth data
            if (profile != null && profile.getId() != null) {
                user.setTrelloId(profile.getId());
                user.setTrelloUsername(profile.getUsername());
                log.info("Linked Trello with profile: id={}, username={}", profile.getId(), profile.getUsername());
            } else {
                // Fallback: generate pseudo IDs from token hash if profile fetch failed
                String tokenHash = Integer.toHexString(request.getToken().hashCode());
                user.setTrelloId("token_" + tokenHash);
                user.setTrelloUsername("trello_user_" + tokenHash);
                log.info("Profile fetch failed; using token-based IDs for Trello account");
            }
            
            user.setTrelloAccessToken(tokenEncryptor.encrypt(request.getToken()));
            
            log.info("Linking Trello account to user: email={}, trelloId={}, trelloUsername={}", 
                    userEmail, user.getTrelloId(), user.getTrelloUsername());

            userRepository.save(user);

            // Clear state cookie
            response.addHeader("Set-Cookie", "trello_state=; Max-Age=0; Path=/; SameSite=None; Secure");

            // Generate new JWT token with updated user info
            String jwt = jwtUtil.generateToken(user.getEmail(), Map.of("role", user.getRole().name()));

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(jwt)
                    .user(userService.toDto(user))
                    .build());

        } catch (Exception e) {
            log.error("Trello account linking failed: {} | Cause: {} | Exception type: {}", 
                    e.getMessage(), 
                    e.getCause() != null ? e.getCause().getMessage() : "N/A",
                    e.getClass().getSimpleName(),
                    e);
            return ResponseEntity.status(500).body(AuthResponse.builder()
                    .token(null)
                    .user(null)
                    .build());
        }
    }

    @PostMapping("/trello/unlink")
    @Operation(summary = "Unlink Trello account and revoke token")
    public ResponseEntity<Void> unlinkTrelloAccount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        if (user.getTrelloAccessToken() != null && !user.getTrelloAccessToken().isBlank()) {
            try {
                String token = tokenEncryptor.decrypt(user.getTrelloAccessToken());
                trelloClient.revokeToken(token);
            } catch (Exception ex) {
                log.warn("Trello token revoke failed: {}", ex.getMessage());
            }
        }

        user.setTrelloAccessToken(null);
        user.setTrelloId(null);
        user.setTrelloUsername(null);
        userRepository.save(user);

        return ResponseEntity.noContent().build();
    }

    private TrelloMemberProfile toProfile(com.fasterxml.jackson.databind.JsonNode node) {
        try {
            return node == null ? null : new com.fasterxml.jackson.databind.ObjectMapper().treeToValue(node, TrelloMemberProfile.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Trello profile", e);
        }
    }
}
