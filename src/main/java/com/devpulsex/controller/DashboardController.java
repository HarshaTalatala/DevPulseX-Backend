package com.devpulsex.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public List<ProjectMetricsDto> getProjectMetrics() {
        log.debug("Fetching project metrics");
        return dashboardService.getAllProjectMetrics();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('DEVELOPER')")
    @Operation(summary = "Get user-level metrics")
    public List<UserMetricsDto> getUserMetrics() {
        log.debug("Fetching user metrics");
        return dashboardService.getAllUserMetrics();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('DEVELOPER')")
    @Operation(summary = "Get dashboard summary")
    public DashboardDto getSummary() {
        log.debug("Fetching dashboard summary");
        return dashboardService.getDashboardSummary();
    }

    @GetMapping("/trello/{projectId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('DEVELOPER')")
    @Operation(summary = "Get Trello aggregate for a project")
    public Object getTrelloForProject(@PathVariable Long projectId) {
        log.debug("Fetching Trello dashboard for project {}", projectId);
        return dashboardService.getTrelloDashboardForProject(projectId);
    }
}
