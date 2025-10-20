package com.devpulsex.service;

import com.devpulsex.dto.issue.IssueDto;
import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.model.Issue;
import com.devpulsex.model.IssueStatus;
import com.devpulsex.model.Project;
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

    public IssueService(IssueRepository issueRepository, ProjectRepository projectRepository, UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public List<IssueDto> getAll() { return issueRepository.findAll().stream().map(this::toDto).toList(); }

    public IssueDto getById(Long id) { return issueRepository.findById(id).map(this::toDto).orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + id)); }

    public IssueDto create(IssueDto dto) {
        Issue i = new Issue();
        apply(dto, i);
        return toDto(issueRepository.save(i));
    }

    public IssueDto update(Long id, IssueDto dto) {
        Issue i = issueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + id));
        apply(dto, i);
        return toDto(issueRepository.save(i));
    }

    public void delete(Long id) {
        if (!issueRepository.existsById(id)) throw new ResourceNotFoundException("Issue not found: " + id);
        issueRepository.deleteById(id);
    }

    // New: transition issue status following allowed lifecycle
    public IssueDto transitionStatus(Long id, IssueStatus newStatus) {
        Issue issue = issueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + id));
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

    private void apply(IssueDto dto, Issue i) {
        Project project = projectRepository.findById(dto.getProjectId()).orElseThrow(() -> new ResourceNotFoundException("Project not found: " + dto.getProjectId()));
        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found: " + dto.getUserId()));
        i.setProject(project);
        i.setUser(user);
        i.setDescription(dto.getDescription());
        i.setStatus(dto.getStatus());
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
