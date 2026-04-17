package com.devpulsex.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.devpulsex.dto.project.ProjectDto;
import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.model.Project;
import com.devpulsex.model.Team;
import com.devpulsex.repository.ProjectRepository;
import com.devpulsex.repository.TeamRepository;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final AuthorizationScopeService authorizationScopeService;

    public ProjectService(ProjectRepository projectRepository, TeamRepository teamRepository,
            AuthorizationScopeService authorizationScopeService) {
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
        this.authorizationScopeService = authorizationScopeService;
    }

    public List<ProjectDto> getAll() {
        return projectRepository.findAll().stream()
                .filter(authorizationScopeService::hasProjectAccess)
                .map(this::toDto)
                .toList();
    }

    @SuppressWarnings("null")
    public ProjectDto getById(Long id) {
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
        authorizationScopeService.requireProjectAccess(project);
        return toDto(project);
    }

    @SuppressWarnings("null")
    public ProjectDto create(ProjectDto dto) {
        Team team = teamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + dto.getTeamId()));
        authorizationScopeService.requireTeamAccess(team);
        Project p = Project.builder().name(dto.getName()).team(team).trelloBoardId(dto.getTrelloBoardId()).build();
        return toDto(projectRepository.save(p));
    }

    @SuppressWarnings("null")
    public ProjectDto update(Long id, ProjectDto dto) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
        authorizationScopeService.requireProjectAccess(p);
        p.setName(dto.getName());
        if (dto.getTeamId() != null) {
            Team team = teamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + dto.getTeamId()));
            authorizationScopeService.requireTeamAccess(team);
            p.setTeam(team);
        }
        // Allow updating / clearing trelloBoardId
        p.setTrelloBoardId(dto.getTrelloBoardId());
        return toDto(projectRepository.save(p));
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
        authorizationScopeService.requireProjectAccess(project);
        projectRepository.delete(project);
    }

    private ProjectDto toDto(Project p) {
        Long teamId = p.getTeam() == null ? null : p.getTeam().getId();
        return ProjectDto.builder()
                .id(p.getId())
                .name(p.getName())
                .teamId(teamId)
            .trelloBoardId(p.getTrelloBoardId())
                .build();
    }
}
