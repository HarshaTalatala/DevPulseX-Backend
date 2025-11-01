package com.devpulsex.dto.github;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GithubInsightsResponse {
    private String username;
    private int repoCount;
    private int totalPullRequests;
    private int recentCommits; // last 7 days
    
    // Additional metrics
    private int totalIssues;
    private int openIssues;
    private int closedIssues;
    private int totalStars;
    private int followers;
    private int following;
    private int publicGists;
    
    // Recent activity
    private int recentPRs; // last 7 days
    private int recentIssues; // last 7 days
    private String mostActiveRepo;
    
    @Builder.Default
    private Instant fetchedAt = Instant.now();
    
    // Additional metadata
    private String avatarUrl;
    private String profileUrl;
}
