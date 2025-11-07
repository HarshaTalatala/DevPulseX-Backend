package com.devpulsex.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.devpulsex.dto.github.GithubInsightsResponse;
import com.devpulsex.exception.GitHubRateLimitException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Wrapper service that provides resilient GitHub data fetching with cache fallback.
 * 
 * This service wraps the GitHubService and provides:
 * - Automatic fallback to cached data on API failures
 * - Rate limit detection and handling
 * - Graceful degradation
 */
@Service
public class ResilientGitHubService {
    
    private static final Logger log = LoggerFactory.getLogger(ResilientGitHubService.class);
    
    private final GitHubService gitHubService;
    private final CacheManager cacheManager;
    
    public ResilientGitHubService(GitHubService gitHubService, CacheManager cacheManager) {
        this.gitHubService = gitHubService;
        this.cacheManager = cacheManager;
    }
    
    /**
     * Fetch GitHub insights with automatic fallback to cache on failure.
     * 
     * Flow:
     * 1. Try to fetch fresh data from GitHub API
     * 2. If rate limited or API fails, return cached data
     * 3. If no cached data, return empty response
     * 
     * @param username GitHub username
     * @param accessToken GitHub access token
     * @return GitHub insights (fresh or cached)
     */
    public GithubInsightsResponse fetchInsightsWithFallback(String username, String accessToken) {
        try {
            // Try to fetch fresh data
            return gitHubService.fetchInsights(username, accessToken);
            
        } catch (GitHubRateLimitException e) {
            log.warn("GitHub rate limit exceeded for user: {}. Remaining: {}, Reset at: {}", 
                    username, e.getRemainingRequests(), e.getResetTimeEpoch());
            
            // Fallback to cached data
            return getCachedInsights(username)
                    .orElseGet(() -> createEmptyInsights(username, "Rate limit exceeded"));
                    
        } catch (Exception e) {
            log.error("Error fetching GitHub insights for user: {}. Attempting cache fallback.", username, e);
            
            // Fallback to cached data on any error
            return getCachedInsights(username)
                    .orElseGet(() -> createEmptyInsights(username, "API error: " + e.getMessage()));
        }
    }
    
    /**
     * Fetch GitHub repositories with automatic fallback to cache.
     * 
     * @param accessToken GitHub access token
     * @return Repositories (fresh or cached)
     */
    public JsonNode fetchRepositoriesWithFallback(String accessToken) {
        try {
            // Try to fetch fresh data
            return gitHubService.fetchRepositories(accessToken);
            
        } catch (GitHubRateLimitException e) {
            log.warn("GitHub rate limit exceeded when fetching repositories. Remaining: {}", 
                    e.getRemainingRequests());
            
            // Fallback to cached data
            return getCachedRepositories(accessToken)
                    .orElse(null);
                    
        } catch (Exception e) {
            log.error("Error fetching GitHub repositories. Attempting cache fallback.", e);
            
            // Fallback to cached data
            return getCachedRepositories(accessToken)
                    .orElse(null);
        }
    }
    
    /**
     * Get cached GitHub insights if available.
     * 
     * @param username GitHub username (cache key)
     * @return Optional containing cached insights
     */
    private Optional<GithubInsightsResponse> getCachedInsights(String username) {
        try {
            Cache cache = cacheManager.getCache("githubInsights");
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(username);
                if (wrapper != null) {
                    GithubInsightsResponse cached = (GithubInsightsResponse) wrapper.get();
                    log.info("Returning cached GitHub insights for user: {} (CACHE FALLBACK)", username);
                    return Optional.ofNullable(cached);
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving cached insights for user: {}", username, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Get cached repositories if available.
     * 
     * @param accessToken Access token (used for cache key hash)
     * @return Optional containing cached repositories
     */
    private Optional<JsonNode> getCachedRepositories(String accessToken) {
        try {
            Cache cache = cacheManager.getCache("githubRepositories");
            if (cache != null) {
                // Use same cache key strategy as @Cacheable
                Integer cacheKey = accessToken.hashCode();
                Cache.ValueWrapper wrapper = cache.get(cacheKey);
                if (wrapper != null) {
                    JsonNode cached = (JsonNode) wrapper.get();
                    log.info("Returning cached GitHub repositories (CACHE FALLBACK)");
                    return Optional.ofNullable(cached);
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving cached repositories", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Create empty insights response for fallback scenarios.
     * 
     * @param username GitHub username
     * @param reason Reason for empty response
     * @return Empty insights response
     */
    private GithubInsightsResponse createEmptyInsights(String username, String reason) {
        log.warn("Creating empty insights for user: {}. Reason: {}", username, reason);
        
        return GithubInsightsResponse.builder()
                .username(username)
                .repoCount(0)
                .totalPullRequests(0)
                .recentCommits(0)
                .totalIssues(0)
                .openIssues(0)
                .closedIssues(0)
                .totalStars(0)
                .followers(0)
                .following(0)
                .publicGists(0)
                .recentPRs(0)
                .recentIssues(0)
                .mostActiveRepo("")
                .profileUrl("https://github.com/" + username)
                .build();
    }
}
