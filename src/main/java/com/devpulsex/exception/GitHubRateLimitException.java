package com.devpulsex.exception;

/**
 * Exception thrown when GitHub API rate limit is exceeded.
 * This triggers fallback to cached data if available.
 */
public class GitHubRateLimitException extends RuntimeException {
    
    private final int remainingRequests;
    private final long resetTimeEpoch;
    
    public GitHubRateLimitException(String message, int remainingRequests, long resetTimeEpoch) {
        super(message);
        this.remainingRequests = remainingRequests;
        this.resetTimeEpoch = resetTimeEpoch;
    }
    
    public int getRemainingRequests() {
        return remainingRequests;
    }
    
    public long getResetTimeEpoch() {
        return resetTimeEpoch;
    }
}
