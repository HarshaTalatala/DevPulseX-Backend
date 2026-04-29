package com.devpulsex.controller;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devpulsex.config.security.JwtUtil;
import com.devpulsex.dto.auth.AuthResponse;
import com.devpulsex.dto.google.GoogleAuthRequest;
import com.devpulsex.dto.google.GoogleTokenResponse;
import com.devpulsex.dto.google.GoogleUserProfile;
import com.devpulsex.integration.oauth.OAuthTokenEncryptor;
import com.devpulsex.model.Role;
import com.devpulsex.model.User;
import com.devpulsex.repository.UserRepository;
import com.devpulsex.service.GoogleOAuthService;
import com.devpulsex.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Google OAuth", description = "Authenticate via Google and receive app JWT")
public class GoogleAuthController {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthController.class);

    private final GoogleOAuthService oAuthService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final OAuthTokenEncryptor tokenEncryptor;

    public GoogleAuthController(GoogleOAuthService oAuthService,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               JwtUtil jwtUtil,
                               UserService userService,
                               OAuthTokenEncryptor tokenEncryptor) {
        this.oAuthService = oAuthService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.tokenEncryptor = tokenEncryptor;
    }    @PostMapping("/google")
    @Operation(summary = "Exchange Google code for JWT and user info")
    @SuppressWarnings("null")
    public ResponseEntity<AuthResponse> googleLogin(
            @Valid @RequestBody GoogleAuthRequest request,
            @CookieValue(value = "oauth_state_google", required = false) String expectedState,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            log.info("Google OAuth login attempt");

            if (expectedState == null || expectedState.isBlank() || !expectedState.equals(request.getState())) {
                log.warn("Google OAuth state validation failed");
                clearOauthStateCookie(httpRequest, httpResponse, "oauth_state_google");
                return ResponseEntity.badRequest().build();
            }
            
            // Exchange code for access token
            GoogleTokenResponse tokenResp = oAuthService.exchangeCodeForToken(request.getCode());
            if (tokenResp == null || tokenResp.getAccessToken() == null) {
                log.warn("Google token exchange returned null or no access token");
                clearOauthStateCookie(httpRequest, httpResponse, "oauth_state_google");
                return ResponseEntity.badRequest().build();
            }
            
            // Fetch user profile
            GoogleUserProfile profile = oAuthService.fetchUserProfile(tokenResp.getAccessToken());
            if (profile == null || profile.getId() == null) {
                log.warn("Google user profile fetch returned null or missing ID");
                clearOauthStateCookie(httpRequest, httpResponse, "oauth_state_google");
                return ResponseEntity.badRequest().build();
            }
            
            log.info("Google user profile retrieved");

            String email = profile.getEmail();
            String name = (profile.getName() != null && !profile.getName().isBlank()) 
                ? profile.getName() 
                : profile.getGivenName();

            // Try to find existing user by Google ID first, then by email
            User user = userRepository.findByGoogleId(profile.getId()).orElse(null);
            if (user == null) {
                user = userRepository.findByEmail(email).orElse(null);
            }

            if (user == null) {
                // Create new user with Google OAuth data
                user = User.builder()
                        .name(name)
                        .email(email)
                        .password(passwordEncoder.encode(randomPassword()))
                        .role(Role.DEVELOPER)
                        .googleId(profile.getId())
                        .googleEmail(profile.getEmail())
                        .googleName(profile.getName())
                        .googlePictureUrl(profile.getPicture())
                        .googleAccessToken(tokenEncryptor.encrypt(tokenResp.getAccessToken()))
                        .googleRefreshToken(tokenEncryptor.encrypt(tokenResp.getRefreshToken()))
                        .build();
                log.info("Google OAuth user created");
            } else {
                // Update existing user with Google OAuth data (account linking by email)
                user.setName(name);
                user.setEmail(email);
                user.setGoogleId(profile.getId());
                user.setGoogleEmail(profile.getEmail());
                user.setGoogleName(profile.getName());
                user.setGooglePictureUrl(profile.getPicture());
                user.setGoogleAccessToken(tokenEncryptor.encrypt(tokenResp.getAccessToken()));
                user.setGoogleRefreshToken(tokenEncryptor.encrypt(tokenResp.getRefreshToken()));
                log.info("Google OAuth account linked");
            }

            userRepository.save(user);

            // Generate JWT token
            String jwt = jwtUtil.generateToken(user.getEmail(), Map.of("role", user.getRole().name()));
            clearOauthStateCookie(httpRequest, httpResponse, "oauth_state_google");
            
            return ResponseEntity.ok(AuthResponse.builder()
                    .token(jwt)
                    .user(userService.toDto(user))
                    .build());
        } catch (Exception ex) {
            log.error("Google OAuth login failed");
            clearOauthStateCookie(httpRequest, httpResponse, "oauth_state_google");
            return ResponseEntity.status(502).build();
        }
    }

        private void clearOauthStateCookie(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull String cookieName) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(request.isSecure())
            .sameSite("None")
            .path("/api/auth")
            .maxAge(0)
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String randomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
