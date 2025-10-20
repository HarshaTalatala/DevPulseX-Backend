package com.devpulsex.dto.task;

import com.devpulsex.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDto {
    private Long id;
    @NotBlank
    private String title;
    private String description;
    private Long projectId;
    private Long assignedUserId;
    @NotNull
    private TaskStatus status;
    private LocalDate dueDate;
}
