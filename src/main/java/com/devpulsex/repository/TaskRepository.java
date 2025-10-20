package com.devpulsex.repository;

import com.devpulsex.model.Task;
import com.devpulsex.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);

    long countByProject_Id(Long projectId);
    long countByAssignedUser_Id(Long userId);
    long countByProject_IdAndStatus(Long projectId, TaskStatus status);
    long countByAssignedUser_IdAndStatus(Long userId, TaskStatus status);
}
