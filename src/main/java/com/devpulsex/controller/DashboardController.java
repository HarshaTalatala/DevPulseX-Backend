package com.devpulsex.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.devpulsex.dto.dashboard.DashboardDto;
import com.devpulsex.dto.dashboard.ProjectMetricsDto;
import com.devpulsex.dto.dashboard.UserMetricsDto;
import com.devpulsex.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Project and user metrics and summaries")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/projects")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('DEVELOPER')")
    @Operation(summary = "Get project-level metrics")
    public List<ProjectMetricsDto> getProjectMetrics(Authentication authentication) {
        try {
            List<ProjectMetricsDto> metrics = dashboardService.getAllProjectMetrics();
            log.info("Project metrics fetch succeeded");
            return metrics;
        } catch (Exception e) {
            log.error("Project metrics fetch failed");
            throw e;
        }
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('DEVELOPER')")
    @Operation(summary = "Get user-level metrics")
    public List<UserMetricsDto> getUserMetrics(Authentication authentication) {
        try {
            List<UserMetricsDto> metrics = dashboardService.getAllUserMetrics();
            log.info("User metrics fetch succeeded");
            return metrics;
        } catch (Exception e) {
            log.error("User metrics fetch failed");
            throw e;
        }
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('DEVELOPER')")
    @Operation(summary = "Get dashboard summary")
    public DashboardDto getSummary(Authentication authentication) {
        try {
            DashboardDto summary = dashboardService.getDashboardSummary();
            log.info("Dashboard summary fetch succeeded");
            return summary;
        } catch (Exception e) {
            log.error("Dashboard summary fetch failed");
            throw e;
        }
    }

    @GetMapping("/trello/{projectId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('DEVELOPER')")
    @Operation(summary = "Get Trello aggregate for a project")
    public Object getTrelloForProject(@PathVariable Long projectId, Authentication authentication) {
        return dashboardService.getTrelloDashboardForProject(projectId, authentication);
    }
}
