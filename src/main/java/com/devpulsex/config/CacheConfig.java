package com.devpulsex.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Cache configuration for high-performance caching using Caffeine.
 * 
 * Caching Strategy:
 * - GitHub API responses are expensive (rate-limited, network latency)
 * - Cache for 5 minutes to balance freshness and performance
 * - Maximum 1000 entries to prevent memory issues
 * - Automatic eviction based on size and time
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Primary cache manager for GitHub data.
     * 
     * Cache Names:
     * - githubInsights: User's GitHub analytics (repos, PRs, issues, stars) - 5 min TTL
     * - githubRepositories: List of user's GitHub repositories - 5 min TTL
     * - githubUserProfile: GitHub user profile data - 5 min TTL
     * - githubRateLimit: Rate limit tracking - 1 min TTL (handled separately)
     * 
     * @return Configured CacheManager
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "githubInsights",
            "githubRepositories", 
            "githubUserProfile",
            "githubMetrics",
            "githubRateLimit"
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            // Cache expires after 5 minutes of creation
            .expireAfterWrite(5, TimeUnit.MINUTES)
            // Maximum cache size to prevent memory issues
            .maximumSize(1000)
            // Record cache statistics for monitoring
            .recordStats()
            // Initial cache capacity
            .initialCapacity(50)
        );
        
        return cacheManager;
    }
}
