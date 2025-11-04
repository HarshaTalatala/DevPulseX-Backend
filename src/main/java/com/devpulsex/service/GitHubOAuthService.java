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

import com.devpulsex.dto.github.GitHubTokenResponse;
import com.devpulsex.dto.github.GitHubUserProfile;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class GitHubOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GitHubOAuthService.class);

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    private final WebClient webClient;

    public GitHubOAuthService(
            @Value("${github.client-id}") String clientId,
            @Value("${github.client-secret}") String clientSecret,
            @Value("${github.redirect-uri}") String redirectUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.webClient = WebClient.builder().build();
    }

    public GitHubTokenResponse exchangeCodeForToken(String code) {
        try {
            return webClient.post()
                    .uri("https://github.com/login/oauth/access_token")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("code", code)
                            .with("redirect_uri", redirectUri))
            .retrieve()
            .onStatus(status -> status.isError(), resp -> resp.bodyToMono(String.class)
                            .map(body -> new RuntimeException("GitHub token error: " + body)))
                    .bodyToMono(GitHubTokenResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("GitHub token exchange failed: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("GitHub token exchange unexpected error", e);
            return null;
        }
    }

    public GitHubUserProfile fetchUserProfile(String accessToken) {
        try {
            return webClient.get()
                    .uri("https://api.github.com/user")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .onStatus(status -> status.isError(), resp -> resp.bodyToMono(String.class)
                            .map(body -> new RuntimeException("GitHub user error: " + body)))
                    .bodyToMono(GitHubUserProfile.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("GitHub user profile failed: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("GitHub user profile unexpected error", e);
            return null;
        }
    }

    public String fetchPrimaryEmail(String accessToken) {
        try {
            JsonNode emails = webClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .onStatus(status -> status.isError(), resp -> resp.bodyToMono(String.class)
                            .map(body -> new RuntimeException("GitHub emails error: " + body)))
                    .bodyToMono(JsonNode.class)
                    .block();
            if (emails != null && emails.isArray()) {
                for (JsonNode e : emails) {
                    boolean primary = e.path("primary").asBoolean(false);
                    boolean verified = e.path("verified").asBoolean(false);
                    if (primary && verified) {
                        return e.path("email").asText(null);
                    }
                }
                // fallback: first verified email
                for (JsonNode e : emails) {
                    if (e.path("verified").asBoolean(false)) {
                        return e.path("email").asText(null);
                    }
                }
            }
        } catch (WebClientResponseException e) {
            log.warn("GitHub email fetch failed: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("GitHub email fetch unexpected error", e);
        }
        return null;
    }
}
