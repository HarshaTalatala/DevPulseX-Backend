package com.devpulsex.controller;

import com.devpulsex.dto.task.TaskDto;
import com.devpulsex.model.TaskStatus;
import com.devpulsex.service.TaskService;
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
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Manage tasks: assignment, status, due dates")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "Get all tasks")
    public List<TaskDto> getAll() { return taskService.getAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by id")
    public TaskDto getById(@PathVariable Long id) { return taskService.getById(id); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create a task")
    public ResponseEntity<TaskDto> create(@Valid @RequestBody TaskDto dto) {
        TaskDto created = taskService.create(dto);
        log.info("Created task {} for project {}", created.getId(), created.getProjectId());
        return ResponseEntity.created(URI.create("/api/tasks/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update a task")
    public TaskDto update(@PathVariable Long id, @Valid @RequestBody TaskDto dto) { return taskService.update(id, dto); }

    @PostMapping("/{id}/assign/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Assign a task to a user")
    public TaskDto assign(@PathVariable Long id, @PathVariable Long userId) { return taskService.assign(id, userId); }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Transition task status following workflow rules")
    public TaskDto transitionStatus(@PathVariable Long id, @RequestParam TaskStatus status) {
        TaskDto result = taskService.transitionStatus(id, status);
        log.info("Task {} status changed to {}", id, status);
        return result;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> delete(@PathVariable Long id) { taskService.delete(id); return ResponseEntity.noContent().build(); }
}
