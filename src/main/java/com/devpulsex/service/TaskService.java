package com.devpulsex.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devpulsex.dto.task.TaskDto;
import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.model.Project;
import com.devpulsex.model.Task;
import com.devpulsex.model.TaskStatus;
import com.devpulsex.model.Team;
import com.devpulsex.model.User;
import com.devpulsex.repository.ProjectRepository;
import com.devpulsex.repository.TaskRepository;
import com.devpulsex.repository.UserRepository;

@Service
public class TaskService {
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuthorizationScopeService authorizationScopeService;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, UserRepository userRepository,
            AuthorizationScopeService authorizationScopeService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.authorizationScopeService = authorizationScopeService;
    }

    public List<TaskDto> getAll() {
        return taskRepository.findAll().stream()
                .filter(task -> task.getProject() != null && authorizationScopeService.hasProjectAccess(task.getProject()))
                .map(this::toDto)
                .toList();
    }

    @SuppressWarnings("null")
    public TaskDto getById(Long id) {
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        authorizationScopeService.requireProjectAccess(task.getProject());
        return toDto(task);
    }

    public TaskDto create(TaskDto dto) {
        Task task = new Task();
        apply(dto, task);
        return toDto(taskRepository.save(task));
    }

    @SuppressWarnings("null")
    public TaskDto update(Long id, TaskDto dto) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        authorizationScopeService.requireProjectAccess(task.getProject());
        apply(dto, task);
        return toDto(taskRepository.save(task));
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        authorizationScopeService.requireProjectAccess(task.getProject());
        taskRepository.delete(task);
    }

    // Business logic: assign a task to a user
    @SuppressWarnings("null")
    public TaskDto assign(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        authorizationScopeService.requireProjectAccess(task.getProject());
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        validateUserInProjectTeam(task.getProject(), user);
        task.setAssignedUser(user);
        return toDto(taskRepository.save(task));
    }

    // Business logic: transition task status
    @SuppressWarnings("null")
    public TaskDto transitionStatus(Long taskId, TaskStatus newStatus) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        authorizationScopeService.requireProjectAccess(task.getProject());
        TaskStatus current = task.getStatus();
        if (current == newStatus) {
            log.debug("Task {} already in status {}", taskId, newStatus);
            return toDto(task);
        }
        boolean allowed = false;
        if (current == TaskStatus.TODO && newStatus == TaskStatus.IN_PROGRESS) allowed = true;
        if (current == TaskStatus.IN_PROGRESS && (newStatus == TaskStatus.REVIEW || newStatus == TaskStatus.DONE)) allowed = true;
        if (current == TaskStatus.REVIEW && newStatus == TaskStatus.DONE) allowed = true;
        if (current == TaskStatus.BLOCKED && newStatus == TaskStatus.TODO) allowed = true; // unblock
        if (!allowed) {
            throw new IllegalArgumentException("Invalid task status transition: " + current + " -> " + newStatus);
        }
        task.setStatus(newStatus);
        Task saved = taskRepository.save(task);
        log.info("Task {} transitioned from {} to {}", taskId, current, newStatus);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    private void apply(TaskDto dto, Task task) {
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setDueDate(dto.getDueDate());
        if (dto.getProjectId() != null) {
            Project project = projectRepository.findById(dto.getProjectId()).orElseThrow(() -> new ResourceNotFoundException("Project not found: " + dto.getProjectId()));
            authorizationScopeService.requireProjectAccess(project);
            task.setProject(project);
        }
        if (dto.getAssignedUserId() != null) {
            User user = userRepository.findById(dto.getAssignedUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found: " + dto.getAssignedUserId()));
            validateUserInProjectTeam(task.getProject(), user);
            task.setAssignedUser(user);
        } else {
            task.setAssignedUser(null);
        }
    }

    private void validateUserInProjectTeam(Project project, User user) {
        Team team = project == null ? null : project.getTeam();
        if (team == null || user == null) {
            throw new IllegalArgumentException("Task must belong to a valid project team");
        }

        User currentUser = authorizationScopeService.getCurrentUser();
        if (authorizationScopeService.isAdmin(currentUser)) {
            return;
        }

        boolean isMember = team.getMembers().stream().anyMatch(member -> member.getId().equals(user.getId()));
        if (!isMember) {
            throw new IllegalArgumentException("Assigned user must be a member of the project's team");
        }
    }

    private TaskDto toDto(Task t) {
        Long projectId = t.getProject() == null ? null : t.getProject().getId();
        Long assignedId = t.getAssignedUser() == null ? null : t.getAssignedUser().getId();
        return TaskDto.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .projectId(projectId)
                .assignedUserId(assignedId)
                .status(t.getStatus())
                .dueDate(t.getDueDate())
                .build();
    }

}
