package com.devpulsex.dto.dashboard;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMetricsDto {
    private Long userId;
    private String userName;
    private String userEmail;

    // Tasks assigned to user
    private long totalTasks;
    private Map<String, Long> tasksByStatus; // status -> count

    // Commits by user
    private long totalCommits;

    // Issues assigned to user
    private long totalIssuesAssigned;
    private Map<String, Long> issuesByStatus; // status -> count
}
