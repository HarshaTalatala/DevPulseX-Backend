package com.devpulsex.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.devpulsex.exception.GitHubRateLimitException;

import reactor.core.publisher.Mono;

/**
 * Configuration for GitHub API WebClient with rate limit handling.
 */
@Configuration
public class GitHubWebClientConfig {
    
    private static final Logger log = LoggerFactory.getLogger(GitHubWebClientConfig.class);
    
    /**
     * Create a WebClient bean configured for GitHub API calls.
     * Includes rate limit detection and logging.
     * 
     * @return Configured WebClient
     */
    @Bean(name = "githubWebClient")
    public WebClient githubWebClient() {
        return WebClient.builder()
                .filter(rateLimitFilter())
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }
    
    /**
     * Filter to detect and handle GitHub API rate limits.
     * 
     * GitHub returns HTTP 403 with specific headers when rate limited:
     * - X-RateLimit-Remaining: Number of requests remaining
     * - X-RateLimit-Reset: Unix timestamp when limit resets
     */
    private ExchangeFilterFunction rateLimitFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            // Check for rate limit status
            if (response.statusCode() == HttpStatus.FORBIDDEN) {
                return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            // Extract rate limit headers
                            int remaining = parseIntHeader(response, "X-RateLimit-Remaining", 0);
                            long resetTime = parseLongHeader(response, "X-RateLimit-Reset", 0L);
                            
                            // Check if it's a rate limit error
                            if (remaining == 0 || body.contains("rate limit exceeded")) {
                                log.error("GitHub API rate limit exceeded. Reset at: {}", resetTime);
                                
                                return Mono.error(new GitHubRateLimitException(
                                        "GitHub API rate limit exceeded",
                                        remaining,
                                        resetTime
                                ));
                            }
                            
                            // Not a rate limit error, return original response
                            return Mono.just(response);
                        });
            }
            
            // Log rate limit info for successful requests
            int remaining = parseIntHeader(response, "X-RateLimit-Remaining", -1);
            if (remaining >= 0 && remaining < 100) {
                log.warn("GitHub API rate limit low: {} requests remaining", remaining);
            }
            
            return Mono.just(response);
        });
    }
    
    /**
     * Log outgoing requests (debug level).
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            if (log.isDebugEnabled()) {
                log.debug("GitHub API Request: {} {}", request.method(), request.url());
            }
            return Mono.just(request);
        });
    }
    
    /**
     * Log incoming responses (debug level).
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (log.isDebugEnabled()) {
                log.debug("GitHub API Response: {} - {}", 
                        response.statusCode().value(),
                        response.headers().asHttpHeaders().getFirst("X-RateLimit-Remaining"));
            }
            return Mono.just(response);
        });
    }
    
    /**
     * Parse integer header safely.
     */
    private int parseIntHeader(ClientResponse response, String headerName, int defaultValue) {
        try {
            String value = response.headers().asHttpHeaders().getFirst(headerName);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Parse long header safely.
     */
    private long parseLongHeader(ClientResponse response, String headerName, long defaultValue) {
        try {
            String value = response.headers().asHttpHeaders().getFirst(headerName);
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
