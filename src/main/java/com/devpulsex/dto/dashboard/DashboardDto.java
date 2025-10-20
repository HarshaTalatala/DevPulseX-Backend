package com.devpulsex.dto.dashboard;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDto {
    // Aggregated counts across all projects/users
    private long totalProjects;
    private long totalUsers;

    private long totalTasks;
    private Map<String, Long> tasksByStatus; // status -> count

    private long totalCommits;

    private long totalIssues;
    private Map<String, Long> issuesByStatus; // status -> count

    private long totalDeployments;
    private Map<String, Long> deploymentsByStatus; // status -> count

    // Detailed lists
    private List<ProjectMetricsDto> projects;
    private List<UserMetricsDto> users;
}
