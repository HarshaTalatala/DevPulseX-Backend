package com.devpulsex.integration.trello;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Low-level Trello API client with simple rate limit handling.
 */
@Component
public class TrelloClient {

    private static final Logger log = LoggerFactory.getLogger(TrelloClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${trello.api.base-url}")
    private String baseUrl;
    @Value("${trello.api.key}")
    private String apiKey;
    @Value("${trello.api.token}")
    private String apiToken;

    @Value("${trello.rate.limit.requests}")
    private int softLimitRequests;
    @Value("${trello.rate.limit.window-seconds}")
    private int softLimitWindowSeconds;

    private int windowCount = 0;
    private Instant windowStart = Instant.now();

    public TrelloClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode getBoards(String memberId) { return performGet("/members/" + memberId + "/boards"); }
    public JsonNode getLists(String boardId) { return performGet("/boards/" + boardId + "/lists"); }
    public JsonNode getCards(String listId) { return performGet("/lists/" + listId + "/cards"); }

    private void sleepForRetry(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void applySoftWindowLimit() {
        long sleepMs = calculateSleepTime();
        if (sleepMs > 0) {
            log.debug("Soft window limit reached; sleeping {} ms", sleepMs);
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized long calculateSleepTime() {
        Instant now = Instant.now();
        if (windowStart.plusSeconds(softLimitWindowSeconds).isBefore(now)) {
            windowStart = now;
            windowCount = 0;
        }
        windowCount++;
        if (windowCount >= softLimitRequests) {
            long sleepMs = Duration.between(now, windowStart.plusSeconds(softLimitWindowSeconds)).toMillis();
            windowStart = Instant.now();
            windowCount = 0;
            return sleepMs;
        }
        return 0;
    }

    private JsonNode performGet(String path) {
        applySoftWindowLimit();
        String url = UriComponentsBuilder.fromUriString(baseUrl + path)
                .queryParam("key", apiKey)
                .queryParam("token", apiToken)
                .build().toUriString();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        int attempt = 0;
        while (attempt < 3) {
            attempt++;
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                handleRateLimitHeaders(response.getHeaders());
                return objectMapper.readTree(response.getBody());
            } catch (RestClientException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("429") && attempt < 3) {
                    log.warn("Trello 429 encountered; backing off (attempt {})", attempt);
                    sleepForRetry(1500L * attempt);
                    continue;
                }
                throw ex;
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RestClientException("Failed to parse Trello response", e);
            }
        }
        throw new RestClientException("Failed to call Trello API after retries: " + url);
    }

    private void handleRateLimitHeaders(HttpHeaders headers) {
        String remaining = headers.getFirst("x-ratelimit-remaining");
        String reset = headers.getFirst("x-ratelimit-reset");
        if (remaining != null && reset != null) {
            try {
                int rem = Integer.parseInt(remaining);
                long resetEpoch = Long.parseLong(reset);
                if (rem <= 0) {
                    long sleepMs = (resetEpoch * 1000L) - System.currentTimeMillis();
                    if (sleepMs > 0) {
                        log.info("Trello hard limit reached. Sleeping {} ms until reset.", sleepMs);
                        try { Thread.sleep(sleepMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed headers
            }
        }
    }
}
