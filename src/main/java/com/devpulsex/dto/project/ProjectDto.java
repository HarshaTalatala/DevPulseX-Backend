package com.devpulsex.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDto {
    private Long id;
    @NotBlank
    private String name;
    @NotNull
    private Long teamId;
    // Optional Trello board linkage
    private String trelloBoardId;
}
