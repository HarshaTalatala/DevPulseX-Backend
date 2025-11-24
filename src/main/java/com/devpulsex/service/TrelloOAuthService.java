package com.devpulsex.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.devpulsex.dto.trello.TrelloMemberProfile;

@Service
public class TrelloOAuthService {
    
    private static final Logger log = LoggerFactory.getLogger(TrelloOAuthService.class);
    
    private final WebClient webClient;
    private final String trelloApiKey;
    private final String trelloBaseUrl;
    
    public TrelloOAuthService(WebClient.Builder webClientBuilder,
                             @Value("${trello.api.key}") String trelloApiKey,
                             @Value("${trello.api.base-url}") String trelloBaseUrl) {
        this.webClient = webClientBuilder.build();
        this.trelloApiKey = trelloApiKey;
        this.trelloBaseUrl = trelloBaseUrl;
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
