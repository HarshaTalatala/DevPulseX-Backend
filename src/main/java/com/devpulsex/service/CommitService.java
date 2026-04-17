package com.devpulsex.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.devpulsex.dto.commit.CommitDto;
import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.model.Commit;
import com.devpulsex.model.Project;
import com.devpulsex.model.Team;
import com.devpulsex.model.User;
import com.devpulsex.repository.CommitRepository;
import com.devpulsex.repository.ProjectRepository;
import com.devpulsex.repository.UserRepository;

@Service
public class CommitService {
    private final CommitRepository commitRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuthorizationScopeService authorizationScopeService;

    public CommitService(CommitRepository commitRepository, ProjectRepository projectRepository, UserRepository userRepository,
            AuthorizationScopeService authorizationScopeService) {
        this.commitRepository = commitRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.authorizationScopeService = authorizationScopeService;
    }

    public List<CommitDto> getAll() {
        return commitRepository.findAll().stream()
                .filter(commit -> commit.getProject() != null && authorizationScopeService.hasProjectAccess(commit.getProject()))
                .map(this::toDto)
                .toList();
    }

    @SuppressWarnings("null")
    public CommitDto getById(Long id) {
        Commit commit = commitRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Commit not found: " + id));
        authorizationScopeService.requireProjectAccess(commit.getProject());
        return toDto(commit);
    }

    public CommitDto create(CommitDto dto) {
        Commit c = new Commit();
        apply(dto, c);
        if (c.getTimestamp() == null) c.setTimestamp(Instant.now());
        return toDto(commitRepository.save(c));
    }

    @SuppressWarnings("null")
    public CommitDto update(Long id, CommitDto dto) {
        Commit c = commitRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Commit not found: " + id));
        authorizationScopeService.requireProjectAccess(c.getProject());
        apply(dto, c);
        return toDto(commitRepository.save(c));
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        Commit commit = commitRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Commit not found: " + id));
        authorizationScopeService.requireProjectAccess(commit.getProject());
        commitRepository.delete(commit);
    }

    @SuppressWarnings("null")
    private void apply(CommitDto dto, Commit c) {
        Project project = projectRepository.findById(dto.getProjectId()).orElseThrow(() -> new ResourceNotFoundException("Project not found: " + dto.getProjectId()));
        authorizationScopeService.requireProjectAccess(project);
        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found: " + dto.getUserId()));
        validateUserInProjectTeam(project, user);
        c.setProject(project);
        c.setUser(user);
        c.setMessage(dto.getMessage());
        c.setTimestamp(dto.getTimestamp() == null ? Instant.now() : dto.getTimestamp());
    }

    private void validateUserInProjectTeam(Project project, User user) {
        Team team = project == null ? null : project.getTeam();
        if (team == null || user == null) {
            throw new IllegalArgumentException("Commit must belong to a valid project team");
        }

        User currentUser = authorizationScopeService.getCurrentUser();
        if (authorizationScopeService.isAdmin(currentUser)) {
            return;
        }

        boolean isMember = team.getMembers().stream().anyMatch(member -> member.getId().equals(user.getId()));
        if (!isMember) {
            throw new IllegalArgumentException("Commit user must be a member of the project's team");
        }
    }

    private CommitDto toDto(Commit c) {
        return CommitDto.builder()
                .id(c.getId())
                .projectId(c.getProject() == null ? null : c.getProject().getId())
                .userId(c.getUser() == null ? null : c.getUser().getId())
                .message(c.getMessage())
                .timestamp(c.getTimestamp())
                .build();
    }
}
