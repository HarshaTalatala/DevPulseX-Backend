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

    public ProjectService(ProjectRepository projectRepository, TeamRepository teamRepository) {
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
    }

    public List<ProjectDto> getAll() {
        return projectRepository.findAll().stream().map(this::toDto).toList();
    }

    public ProjectDto getById(Long id) {
        return projectRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }

    public ProjectDto create(ProjectDto dto) {
        Team team = teamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + dto.getTeamId()));
        Project p = Project.builder().name(dto.getName()).team(team).trelloBoardId(dto.getTrelloBoardId()).build();
        return toDto(projectRepository.save(p));
    }

    public ProjectDto update(Long id, ProjectDto dto) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
        p.setName(dto.getName());
        if (dto.getTeamId() != null) {
            Team team = teamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + dto.getTeamId()));
            p.setTeam(team);
        }
        // Allow updating / clearing trelloBoardId
        p.setTrelloBoardId(dto.getTrelloBoardId());
        return toDto(projectRepository.save(p));
    }

    public void delete(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project not found: " + id);
        }
        projectRepository.deleteById(id);
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
