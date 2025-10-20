package com.devpulsex.service;

import com.devpulsex.dto.dashboard.DashboardDto;
import com.devpulsex.dto.dashboard.ProjectMetricsDto;
import com.devpulsex.dto.dashboard.UserMetricsDto;
import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.model.*;
import com.devpulsex.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final CommitRepository commitRepository;
    private final IssueRepository issueRepository;
    private final DeploymentRepository deploymentRepository;

    public DashboardService(ProjectRepository projectRepository,
                             UserRepository userRepository,
                             TaskRepository taskRepository,
                             CommitRepository commitRepository,
                             IssueRepository issueRepository,
                             DeploymentRepository deploymentRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.commitRepository = commitRepository;
        this.issueRepository = issueRepository;
        this.deploymentRepository = deploymentRepository;
    }

    public List<ProjectMetricsDto> getAllProjectMetrics() {
        List<Project> projects = projectRepository.findAll();
        if (projects.isEmpty()) {
            throw new ResourceNotFoundException("No projects found");
        }

        // Preload enums for iteration
        TaskStatus[] taskStatuses = TaskStatus.values();
        IssueStatus[] issueStatuses = IssueStatus.values();
        DeploymentStatus[] deploymentStatuses = DeploymentStatus.values();

        Instant now = Instant.now();
        LocalDate today = LocalDate.now();
        Instant from = today.minusDays(29).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<ProjectMetricsDto> result = new ArrayList<>();
        for (Project p : projects) {
            Long projectId = p.getId();

            long totalTasks = taskRepository.countByProject_Id(projectId);
            Map<String, Long> tasksByStatus = new LinkedHashMap<>();
            for (TaskStatus ts : taskStatuses) {
                tasksByStatus.put(ts.name(), taskRepository.countByProject_IdAndStatus(projectId, ts));
            }

            long totalCommits = commitRepository.countByProject_Id(projectId);
            Map<LocalDate, Long> commitsOverTime = bucketCommitsOverTime(projectId, from, now);

            // commits per user within project: derive from commits list
            Map<Long, Long> commitsPerUser = new HashMap<>();
            // Load all commits for project once for per-user aggregation (time-bounded map already computed separately)
            List<Commit> allProjectCommits = commitRepository.findByProject_Id(projectId);
            for (Commit c : allProjectCommits) {
                if (c.getUser() == null) continue;
                Long uid = c.getUser().getId();
                commitsPerUser.merge(uid, 1L, Long::sum);
            }

            long totalIssues = issueRepository.countByProject_Id(projectId);
            Map<String, Long> issuesByStatus = new LinkedHashMap<>();
            for (IssueStatus is : issueStatuses) {
                issuesByStatus.put(is.name(), issueRepository.countByProject_IdAndStatus(projectId, is));
            }
            // issues assigned per user in project
            Map<Long, Long> issuesAssignedPerUser = new HashMap<>();
            List<Issue> projectIssues = issueRepository.findByProject_Id(projectId);
            for (Issue i : projectIssues) {
                if (i.getUser() == null) continue;
                Long uid = i.getUser().getId();
                issuesAssignedPerUser.merge(uid, 1L, Long::sum);
            }

            long totalDeployments = deploymentRepository.countByProject_Id(projectId);
            Map<String, Long> deploymentsByStatus = new LinkedHashMap<>();
            for (DeploymentStatus ds : deploymentStatuses) {
                deploymentsByStatus.put(ds.name(), deploymentRepository.countByProject_IdAndStatus(projectId, ds));
            }
            Optional<Deployment> last = deploymentRepository.findFirstByProject_IdOrderByTimestampDesc(projectId);

            result.add(ProjectMetricsDto.builder()
                    .projectId(projectId)
                    .projectName(p.getName())
                    .totalTasks(totalTasks)
                    .tasksByStatus(tasksByStatus)
                    .totalCommits(totalCommits)
                    .commitsOverTime(commitsOverTime)
                    .commitsPerUser(commitsPerUser)
                    .totalIssues(totalIssues)
                    .issuesByStatus(issuesByStatus)
                    .issuesAssignedPerUser(issuesAssignedPerUser)
                    .totalDeployments(totalDeployments)
                    .deploymentsByStatus(deploymentsByStatus)
                    .lastDeploymentStatus(last.map(ld -> ld.getStatus().name()).orElse(null))
                    .lastDeploymentTimestamp(last.map(ld -> ld.getTimestamp().toString()).orElse(null))
                    .build());
        }
        return result;
    }

    private Map<LocalDate, Long> bucketCommitsOverTime(Long projectId, Instant from, Instant to) {
        List<Commit> commits = commitRepository.findByProject_IdAndTimestampBetween(projectId, from, to);
        Map<LocalDate, Long> buckets = new LinkedHashMap<>();
        LocalDate start = from.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = to.atZone(ZoneId.systemDefault()).toLocalDate();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            buckets.put(d, 0L);
        }
        for (Commit c : commits) {
            LocalDate d = c.getTimestamp().atZone(ZoneId.systemDefault()).toLocalDate();
            buckets.merge(d, 1L, Long::sum);
        }
        return buckets;
    }

    public List<UserMetricsDto> getAllUserMetrics() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new ResourceNotFoundException("No users found");
        }
        TaskStatus[] taskStatuses = TaskStatus.values();
        IssueStatus[] issueStatuses = IssueStatus.values();

        List<UserMetricsDto> result = new ArrayList<>();
        for (User u : users) {
            Long userId = u.getId();
            long totalTasks = taskRepository.countByAssignedUser_Id(userId);
            Map<String, Long> tasksByStatus = new LinkedHashMap<>();
            for (TaskStatus ts : taskStatuses) {
                tasksByStatus.put(ts.name(), taskRepository.countByAssignedUser_IdAndStatus(userId, ts));
            }

            long totalCommits = commitRepository.countByUser_Id(userId);

            long totalIssuesAssigned = issueRepository.countByUser_Id(userId);
            Map<String, Long> issuesByStatus = new LinkedHashMap<>();
            for (IssueStatus is : issueStatuses) {
                issuesByStatus.put(is.name(), issueRepository.countByUser_IdAndStatus(userId, is));
            }

            result.add(UserMetricsDto.builder()
                    .userId(userId)
                    .userName(u.getName())
                    .userEmail(u.getEmail())
                    .totalTasks(totalTasks)
                    .tasksByStatus(tasksByStatus)
                    .totalCommits(totalCommits)
                    .totalIssuesAssigned(totalIssuesAssigned)
                    .issuesByStatus(issuesByStatus)
                    .build());
        }
        return result;
    }

    public DashboardDto getDashboardSummary() {
        List<ProjectMetricsDto> projectMetrics = getAllProjectMetrics();
        List<UserMetricsDto> userMetrics = getAllUserMetrics();

        long totalProjects = projectMetrics.size();
        long totalUsers = userMetrics.size();

        // Aggregate totals and status maps
        long totalTasks = 0;
        Map<String, Long> tasksByStatus = new LinkedHashMap<>();
        for (TaskStatus ts : TaskStatus.values()) tasksByStatus.put(ts.name(), 0L);

        long totalCommits = 0;

        long totalIssues = 0;
        Map<String, Long> issuesByStatus = new LinkedHashMap<>();
        for (IssueStatus is : IssueStatus.values()) issuesByStatus.put(is.name(), 0L);

        long totalDeployments = 0;
        Map<String, Long> deploymentsByStatus = new LinkedHashMap<>();
        for (DeploymentStatus ds : DeploymentStatus.values()) deploymentsByStatus.put(ds.name(), 0L);

        for (ProjectMetricsDto pm : projectMetrics) {
            totalTasks += pm.getTotalTasks();
            pm.getTasksByStatus().forEach((k, v) -> tasksByStatus.merge(k, v, Long::sum));

            totalCommits += pm.getTotalCommits();

            totalIssues += pm.getTotalIssues();
            pm.getIssuesByStatus().forEach((k, v) -> issuesByStatus.merge(k, v, Long::sum));

            totalDeployments += pm.getTotalDeployments();
            pm.getDeploymentsByStatus().forEach((k, v) -> deploymentsByStatus.merge(k, v, Long::sum));
        }

        return DashboardDto.builder()
                .totalProjects(totalProjects)
                .totalUsers(totalUsers)
                .totalTasks(totalTasks)
                .tasksByStatus(tasksByStatus)
                .totalCommits(totalCommits)
                .totalIssues(totalIssues)
                .issuesByStatus(issuesByStatus)
                .totalDeployments(totalDeployments)
                .deploymentsByStatus(deploymentsByStatus)
                .projects(projectMetrics)
                .users(userMetrics)
                .build();
    }
}
