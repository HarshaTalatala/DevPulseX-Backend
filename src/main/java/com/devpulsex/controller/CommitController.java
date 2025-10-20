package com.devpulsex.controller;

import com.devpulsex.dto.commit.CommitDto;
import com.devpulsex.service.CommitService;
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
@RequestMapping("/api/commits")
@Tag(name = "Commits", description = "Track commits per project and user")
public class CommitController {

    private static final Logger log = LoggerFactory.getLogger(CommitController.class);

    private final CommitService commitService;

    public CommitController(CommitService commitService) { this.commitService = commitService; }

    @GetMapping
    @Operation(summary = "Get all commits")
    public List<CommitDto> getAll() { return commitService.getAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "Get commit by id")
    public CommitDto getById(@PathVariable Long id) { return commitService.getById(id); }

    @PostMapping
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create a commit")
    public ResponseEntity<CommitDto> create(@Valid @RequestBody CommitDto dto) {
        CommitDto created = commitService.create(dto);
        log.info("Created commit {} in project {} by user {}", created.getId(), created.getProjectId(), created.getUserId());
        return ResponseEntity.created(URI.create("/api/commits/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update a commit")
    public CommitDto update(@PathVariable Long id, @Valid @RequestBody CommitDto dto) { return commitService.update(id, dto); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a commit")
    public ResponseEntity<Void> delete(@PathVariable Long id) { commitService.delete(id); return ResponseEntity.noContent().build(); }
}
