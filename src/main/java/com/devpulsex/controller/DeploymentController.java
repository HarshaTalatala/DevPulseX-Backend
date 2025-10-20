package com.devpulsex.controller;

import com.devpulsex.dto.deployment.DeploymentDto;
import com.devpulsex.model.DeploymentStatus;
import com.devpulsex.service.DeploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/deployments")
@Tag(name = "Deployments", description = "Manage deployments and track statuses")
public class DeploymentController {

    private static final Logger log = LoggerFactory.getLogger(DeploymentController.class);

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) { this.deploymentService = deploymentService; }

    @GetMapping
    @Operation(summary = "Get all deployments")
    public List<DeploymentDto> getAll() { return deploymentService.getAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "Get deployment by id")
    public DeploymentDto getById(@PathVariable Long id) { return deploymentService.getById(id); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create a deployment")
    public ResponseEntity<DeploymentDto> create(@Valid @RequestBody DeploymentDto dto) {
        DeploymentDto created = deploymentService.create(dto);
        log.info("Created deployment {} for project {}", created.getId(), created.getProjectId());
        return ResponseEntity.created(URI.create("/api/deployments/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update a deployment")
    public DeploymentDto update(@PathVariable Long id, @Valid @RequestBody DeploymentDto dto) { return deploymentService.update(id, dto); }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Transition deployment status following lifecycle rules")
    public DeploymentDto transitionStatus(@PathVariable Long id, @RequestParam DeploymentStatus status) {
        DeploymentDto result = deploymentService.transitionStatus(id, status);
        log.info("Deployment {} status changed to {}", id, status);
        return result;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a deployment")
    public ResponseEntity<Void> delete(@PathVariable Long id) { deploymentService.delete(id); return ResponseEntity.noContent().build(); }
}
