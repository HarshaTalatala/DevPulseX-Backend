package com.devpulsex.dto.deployment;

import com.devpulsex.model.DeploymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentDto {
    private Long id;
    @NotNull
    private Long projectId;
    @NotNull
    private DeploymentStatus status;
    private Instant timestamp;
}
