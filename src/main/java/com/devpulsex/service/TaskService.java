package com.devpulsex.service;

import com.devpulsex.dto.task.TaskDto;
import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.model.Project;
import com.devpulsex.model.Task;
import com.devpulsex.model.TaskStatus;
import com.devpulsex.model.User;
import com.devpulsex.repository.ProjectRepository;
import com.devpulsex.repository.TaskRepository;
import com.devpulsex.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public List<TaskDto> getAll() { return taskRepository.findAll().stream().map(this::toDto).toList(); }

    public TaskDto getById(Long id) {
        return taskRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    public TaskDto create(TaskDto dto) {
        Task task = new Task();
        apply(dto, task);
        return toDto(taskRepository.save(task));
    }

    public TaskDto update(Long id, TaskDto dto) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        apply(dto, task);
        return toDto(taskRepository.save(task));
    }

    public void delete(Long id) {
        if (!taskRepository.existsById(id)) throw new ResourceNotFoundException("Task not found: " + id);
        taskRepository.deleteById(id);
    }

    // Business logic: assign a task to a user
    public TaskDto assign(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        task.setAssignedUser(user);
        return toDto(taskRepository.save(task));
    }

    // Business logic: transition task status (e.g., TODO->IN_PROGRESS->REVIEW->DONE)
    public TaskDto transitionStatus(Long taskId, TaskStatus newStatus) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
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

    private void apply(TaskDto dto, Task task) {
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setDueDate(dto.getDueDate());
        if (dto.getProjectId() != null) {
            Project project = projectRepository.findById(dto.getProjectId()).orElseThrow(() -> new ResourceNotFoundException("Project not found: " + dto.getProjectId()));
            task.setProject(project);
        }
        if (dto.getAssignedUserId() != null) {
            User user = userRepository.findById(dto.getAssignedUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found: " + dto.getAssignedUserId()));
            task.setAssignedUser(user);
        } else {
            task.setAssignedUser(null);
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
