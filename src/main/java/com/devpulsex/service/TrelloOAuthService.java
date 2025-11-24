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

import com.devpulsex.dto.trello.TrelloMemberProfile;
import com.devpulsex.dto.trello.TrelloTokenResponse;

@Service
public class TrelloOAuthService {
    
    private static final Logger log = LoggerFactory.getLogger(TrelloOAuthService.class);
    
    private final WebClient webClient;
    private final String trelloApiKey;
    private final String trelloApiSecret;
    private final String trelloBaseUrl;
    private final String trelloRedirectUri;
    
    public TrelloOAuthService(WebClient.Builder webClientBuilder,
                             @Value("${trello.api.key}") String trelloApiKey,
                             @Value("${trello.api.secret}") String trelloApiSecret,
                             @Value("${trello.api.base-url}") String trelloBaseUrl,
                             @Value("${trello.redirect-uri}") String trelloRedirectUri) {
        this.webClient = webClientBuilder.build();
        this.trelloApiKey = trelloApiKey;
        this.trelloApiSecret = trelloApiSecret;
        this.trelloBaseUrl = trelloBaseUrl;
        this.trelloRedirectUri = trelloRedirectUri;
    }
    
    /**
     * Exchange authorization code for access token
     * Note: Trello uses OAuth 1.0a, so we need to get the token from the callback URL
     * @param oauthToken The OAuth token from the callback
     * @param oauthVerifier The OAuth verifier from the callback
     * @return Access token string
     */
    public String exchangeCodeForToken(String oauthToken, String oauthVerifier) {
        try {
            log.info("Exchanging Trello OAuth token for access token");
            
            TrelloTokenResponse response = webClient.post()
                    .uri("https://trello.com/1/OAuthGetAccessToken")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("key", trelloApiKey)
                            .with("secret", trelloApiSecret)
                            .with("token", oauthToken)
                            .with("verifier", oauthVerifier))
                    .retrieve()
                    .onStatus(status -> status.isError(), resp -> resp.bodyToMono(String.class)
                            .map(body -> new RuntimeException("Trello token error: " + body)))
                    .bodyToMono(TrelloTokenResponse.class)
                    .block();
            
            if (response == null || response.getToken() == null) {
                log.error("Failed to exchange Trello token: null response");
                throw new RuntimeException("Failed to exchange Trello token");
            }
            
            log.info("Successfully exchanged Trello token");
            return response.getToken();
        } catch (WebClientResponseException e) {
            log.error("Trello token exchange failed: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to exchange Trello token: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Trello token exchange unexpected error", e);
            throw new RuntimeException("Failed to exchange Trello token: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fetch member profile using Trello API key and token
     * @param token User's Trello access token
     * @return TrelloMemberProfile with user details
     */
    public TrelloMemberProfile getMemberProfile(String token) {
        try {
            String url = trelloBaseUrl + "/members/me?key=" + trelloApiKey + "&token=" + token;
            
            log.info("Fetching Trello member profile");
            
            TrelloMemberProfile profile = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(TrelloMemberProfile.class)
                    .block();
            
            if (profile == null) {
                log.error("Failed to fetch Trello member profile: null response");
                throw new RuntimeException("Failed to fetch Trello member profile");
            }
            
            log.info("Successfully fetched Trello member profile: id={}, username={}", 
                    profile.getId(), profile.getUsername());
            
            return profile;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching Trello member profile: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch Trello member profile: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate that a token is valid by attempting to fetch member info
     * @param token User's Trello access token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            getMemberProfile(token);
            return true;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
