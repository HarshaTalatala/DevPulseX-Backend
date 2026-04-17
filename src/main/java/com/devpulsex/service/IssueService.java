package com.devpulsex.service;

import com.devpulsex.dto.issue.IssueDto;
import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.model.Issue;
import com.devpulsex.model.IssueStatus;
import com.devpulsex.model.Project;
import com.devpulsex.model.Team;
import com.devpulsex.model.User;
import com.devpulsex.repository.IssueRepository;
import com.devpulsex.repository.ProjectRepository;
import com.devpulsex.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IssueService {
    private static final Logger log = LoggerFactory.getLogger(IssueService.class);

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuthorizationScopeService authorizationScopeService;

    public IssueService(IssueRepository issueRepository, ProjectRepository projectRepository, UserRepository userRepository,
            AuthorizationScopeService authorizationScopeService) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.authorizationScopeService = authorizationScopeService;
    }

    public List<IssueDto> getAll() {
        return issueRepository.findAll().stream()
                .filter(issue -> issue.getProject() != null && authorizationScopeService.hasProjectAccess(issue.getProject()))
                .map(this::toDto)
                .toList();
    }

    @SuppressWarnings("null")
    public IssueDto getById(Long id) {
        Issue issue = issueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + id));
        authorizationScopeService.requireProjectAccess(issue.getProject());
        return toDto(issue);
    }

    public IssueDto create(IssueDto dto) {
        Issue i = new Issue();
        apply(dto, i);
        return toDto(issueRepository.save(i));
    }

    @SuppressWarnings("null")
    public IssueDto update(Long id, IssueDto dto) {
        Issue i = issueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + id));
        authorizationScopeService.requireProjectAccess(i.getProject());
        apply(dto, i);
        return toDto(issueRepository.save(i));
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        Issue issue = issueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + id));
        authorizationScopeService.requireProjectAccess(issue.getProject());
        issueRepository.delete(issue);
    }

    // New: transition issue status following allowed lifecycle
    @SuppressWarnings("null")
    public IssueDto transitionStatus(Long id, IssueStatus newStatus) {
        Issue issue = issueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + id));
        authorizationScopeService.requireProjectAccess(issue.getProject());
        IssueStatus current = issue.getStatus();
        if (current == newStatus) {
            log.debug("Issue {} already in status {}", id, newStatus);
            return toDto(issue);
        }
        boolean allowed = false;
        // Allowed transitions: OPEN -> IN_PROGRESS, IN_PROGRESS -> CLOSED, RESOLVED -> CLOSED
        if (current == IssueStatus.OPEN && newStatus == IssueStatus.IN_PROGRESS) allowed = true;
        if (current == IssueStatus.IN_PROGRESS && newStatus == IssueStatus.CLOSED) allowed = true;
        if (current == IssueStatus.RESOLVED && newStatus == IssueStatus.CLOSED) allowed = true;
        // Soft allow: OPEN -> CLOSED not allowed directly to enforce workflow
        if (!allowed) {
            throw new IllegalArgumentException("Invalid issue status transition: " + current + " -> " + newStatus);
        }
        issue.setStatus(newStatus);
        Issue saved = issueRepository.save(issue);
        log.info("Issue {} transitioned from {} to {}", id, current, newStatus);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    private void apply(IssueDto dto, Issue i) {
        Project project = projectRepository.findById(dto.getProjectId()).orElseThrow(() -> new ResourceNotFoundException("Project not found: " + dto.getProjectId()));
        authorizationScopeService.requireProjectAccess(project);
        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found: " + dto.getUserId()));
        validateUserInProjectTeam(project, user);
        i.setProject(project);
        i.setUser(user);
        i.setDescription(dto.getDescription());
        i.setStatus(dto.getStatus());
    }

    private void validateUserInProjectTeam(Project project, User user) {
        Team team = project == null ? null : project.getTeam();
        if (team == null || user == null) {
            throw new IllegalArgumentException("Issue must belong to a valid project team");
        }

        User currentUser = authorizationScopeService.getCurrentUser();
        if (authorizationScopeService.isAdmin(currentUser)) {
            return;
        }

        boolean isMember = team.getMembers().stream().anyMatch(member -> member.getId().equals(user.getId()));
        if (!isMember) {
            throw new IllegalArgumentException("Issue user must be a member of the project's team");
        }
    }

    private IssueDto toDto(Issue i) {
        return IssueDto.builder()
                .id(i.getId())
                .projectId(i.getProject() == null ? null : i.getProject().getId())
                .userId(i.getUser() == null ? null : i.getUser().getId())
                .description(i.getDescription())
                .status(i.getStatus())
                .build();
    }
}
