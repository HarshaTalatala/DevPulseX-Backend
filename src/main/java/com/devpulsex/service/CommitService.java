package com.devpulsex.service;

import com.devpulsex.dto.commit.CommitDto;
import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.model.Commit;
import com.devpulsex.model.Project;
import com.devpulsex.model.User;
import com.devpulsex.repository.CommitRepository;
import com.devpulsex.repository.ProjectRepository;
import com.devpulsex.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CommitService {
    private static final Logger log = LoggerFactory.getLogger(CommitService.class);

    private final CommitRepository commitRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public CommitService(CommitRepository commitRepository, ProjectRepository projectRepository, UserRepository userRepository) {
        this.commitRepository = commitRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public List<CommitDto> getAll() { return commitRepository.findAll().stream().map(this::toDto).toList(); }

    public CommitDto getById(Long id) {
        return commitRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Commit not found: " + id));
    }

    public CommitDto create(CommitDto dto) {
        Commit c = new Commit();
        apply(dto, c);
        if (c.getTimestamp() == null) c.setTimestamp(Instant.now());
        return toDto(commitRepository.save(c));
    }

    public CommitDto update(Long id, CommitDto dto) {
        Commit c = commitRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Commit not found: " + id));
        apply(dto, c);
        return toDto(commitRepository.save(c));
    }

    public void delete(Long id) {
        if (!commitRepository.existsById(id)) throw new ResourceNotFoundException("Commit not found: " + id);
        commitRepository.deleteById(id);
    }

    private void apply(CommitDto dto, Commit c) {
        Project project = projectRepository.findById(dto.getProjectId()).orElseThrow(() -> new ResourceNotFoundException("Project not found: " + dto.getProjectId()));
        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found: " + dto.getUserId()));
        c.setProject(project);
        c.setUser(user);
        c.setMessage(dto.getMessage());
        c.setTimestamp(dto.getTimestamp() == null ? Instant.now() : dto.getTimestamp());
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
