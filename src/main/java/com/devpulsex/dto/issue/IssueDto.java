package com.devpulsex.dto.issue;

import com.devpulsex.model.IssueStatus;
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
public class IssueDto {
    private Long id;
    @NotNull
    private Long projectId;
    @NotNull
    private Long userId;
    @NotBlank
    private String description;
    @NotNull
    private IssueStatus status;
}
