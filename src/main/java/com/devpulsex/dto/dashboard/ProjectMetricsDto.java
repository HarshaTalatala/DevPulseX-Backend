package com.devpulsex.dto.dashboard;

import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMetricsDto {
    private Long projectId;
    private String projectName;

    // Tasks
    private long totalTasks;
    private Map<String, Long> tasksByStatus; // status name -> count

    // Commits
    private long totalCommits;
    private Map<LocalDate, Long> commitsOverTime; // date -> count
    private Map<Long, Long> commitsPerUser; // userId -> count

    // Issues
    private long totalIssues;
    private Map<String, Long> issuesByStatus; // status name -> count
    private Map<Long, Long> issuesAssignedPerUser; // userId -> count

    // Deployments
    private long totalDeployments;
    private Map<String, Long> deploymentsByStatus; // status name -> count
    private String lastDeploymentStatus; // nullable
    private String lastDeploymentTimestamp; // ISO-8601 string, nullable
}
