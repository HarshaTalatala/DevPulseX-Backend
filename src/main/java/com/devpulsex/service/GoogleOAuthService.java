package com.devpulsex.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.devpulsex.dto.google.GoogleTokenResponse;
import com.devpulsex.dto.google.GoogleUserProfile;

@Service
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    private final WebClient webClient;

    public GoogleOAuthService(
            @Value("${google.client-id}") String clientId,
            @Value("${google.client-secret}") String clientSecret,
            @Value("${google.redirect-uri}") String redirectUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.webClient = WebClient.builder().build();
    }

    public GoogleTokenResponse exchangeCodeForToken(String code) {
        try {
            return webClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("code", code)
                            .with("redirect_uri", redirectUri)
                            .with("grant_type", "authorization_code"))
                    .retrieve()
                    .onStatus(status -> status.isError(), resp -> resp.bodyToMono(String.class)
                            .map(body -> new RuntimeException("Google token error: " + body)))
                    .bodyToMono(GoogleTokenResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Google token exchange failed: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Google token exchange unexpected error", e);
            return null;
        }
    }

    public GoogleUserProfile fetchUserProfile(String accessToken) {
        try {
            return webClient.get()
                    .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status.isError(), resp -> resp.bodyToMono(String.class)
                            .map(body -> new RuntimeException("Google user error: " + body)))
                    .bodyToMono(GoogleUserProfile.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Google user profile failed: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Google user profile unexpected error", e);
            return null;
        }
    }
}
