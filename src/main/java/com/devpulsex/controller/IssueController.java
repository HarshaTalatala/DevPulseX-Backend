package com.devpulsex.controller;

import com.devpulsex.dto.issue.IssueDto;
import com.devpulsex.model.IssueStatus;
import com.devpulsex.service.IssueService;
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
@RequestMapping("/api/issues")
@Tag(name = "Issues", description = "Manage project issues and their lifecycle")
public class IssueController {

    private static final Logger log = LoggerFactory.getLogger(IssueController.class);

    private final IssueService issueService;

    public IssueController(IssueService issueService) { this.issueService = issueService; }

    @GetMapping
    @Operation(summary = "Get all issues")
    public List<IssueDto> getAll() { return issueService.getAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "Get issue by id")
    public IssueDto getById(@PathVariable Long id) { return issueService.getById(id); }

    @PostMapping
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create an issue")
    public ResponseEntity<IssueDto> create(@Valid @RequestBody IssueDto dto) {
        IssueDto created = issueService.create(dto);
        log.info("Created issue {} for project {}", created.getId(), created.getProjectId());
        return ResponseEntity.created(URI.create("/api/issues/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update an issue")
    public IssueDto update(@PathVariable Long id, @Valid @RequestBody IssueDto dto) { return issueService.update(id, dto); }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Transition issue status following lifecycle rules")
    public IssueDto transitionStatus(@PathVariable Long id, @RequestParam IssueStatus status) {
        IssueDto result = issueService.transitionStatus(id, status);
        log.info("Issue {} status changed to {}", id, status);
        return result;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an issue")
    public ResponseEntity<Void> delete(@PathVariable Long id) { issueService.delete(id); return ResponseEntity.noContent().build(); }
}
