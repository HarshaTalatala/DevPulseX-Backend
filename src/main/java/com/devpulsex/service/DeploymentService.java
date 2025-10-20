package com.devpulsex.service;

import com.devpulsex.dto.deployment.DeploymentDto;
import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.model.Deployment;
import com.devpulsex.model.DeploymentStatus;
import com.devpulsex.model.Project;
import com.devpulsex.repository.DeploymentRepository;
import com.devpulsex.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DeploymentService {
    private static final Logger log = LoggerFactory.getLogger(DeploymentService.class);

    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;

    public DeploymentService(DeploymentRepository deploymentRepository, ProjectRepository projectRepository) {
        this.deploymentRepository = deploymentRepository;
        this.projectRepository = projectRepository;
    }

    public List<DeploymentDto> getAll() { return deploymentRepository.findAll().stream().map(this::toDto).toList(); }

    public DeploymentDto getById(Long id) { return deploymentRepository.findById(id).map(this::toDto).orElseThrow(() -> new ResourceNotFoundException("Deployment not found: " + id)); }

    public DeploymentDto create(DeploymentDto dto) {
        Deployment d = new Deployment();
        apply(dto, d);
        if (d.getTimestamp() == null) d.setTimestamp(Instant.now());
        return toDto(deploymentRepository.save(d));
    }

    public DeploymentDto update(Long id, DeploymentDto dto) {
        Deployment d = deploymentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Deployment not found: " + id));
        apply(dto, d);
        return toDto(deploymentRepository.save(d));
    }

    public void delete(Long id) {
        if (!deploymentRepository.existsById(id)) throw new ResourceNotFoundException("Deployment not found: " + id);
        deploymentRepository.deleteById(id);
    }

    // New: enforce deployment status transitions with logging
    public DeploymentDto transitionStatus(Long id, DeploymentStatus newStatus) {
        Deployment d = deploymentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Deployment not found: " + id));
        DeploymentStatus current = d.getStatus();
        if (current == newStatus) {
            log.debug("Deployment {} already in status {}", id, newStatus);
            return toDto(d);
        }
        boolean allowed = false;
        // Allowed transitions: PENDING -> IN_PROGRESS, IN_PROGRESS -> SUCCESS/FAILED
        if (current == DeploymentStatus.PENDING && newStatus == DeploymentStatus.IN_PROGRESS) allowed = true;
        if (current == DeploymentStatus.IN_PROGRESS && (newStatus == DeploymentStatus.SUCCESS || newStatus == DeploymentStatus.FAILED)) allowed = true;
        // Allow rollback from PENDING to FAILED in exceptional flows
        if (current == DeploymentStatus.PENDING && newStatus == DeploymentStatus.FAILED) allowed = true;
        if (!allowed) {
            throw new IllegalArgumentException("Invalid deployment status transition: " + current + " -> " + newStatus);
        }
        d.setStatus(newStatus);
        d.setTimestamp(Instant.now());
        Deployment saved = deploymentRepository.save(d);
        log.info("Deployment {} transitioned from {} to {}", id, current, newStatus);
        return toDto(saved);
    }

    private void apply(DeploymentDto dto, Deployment d) {
        Project project = projectRepository.findById(dto.getProjectId()).orElseThrow(() -> new ResourceNotFoundException("Project not found: " + dto.getProjectId()));
        d.setProject(project);
        d.setStatus(dto.getStatus());
        d.setTimestamp(dto.getTimestamp() == null ? Instant.now() : dto.getTimestamp());
    }

    private DeploymentDto toDto(Deployment d) {
        return DeploymentDto.builder()
                .id(d.getId())
                .projectId(d.getProject() == null ? null : d.getProject().getId())
                .status(d.getStatus())
                .timestamp(d.getTimestamp())
                .build();
    }
}
