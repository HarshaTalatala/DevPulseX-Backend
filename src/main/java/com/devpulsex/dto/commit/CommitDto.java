package com.devpulsex.dto.commit;

import jakarta.validation.constraints.NotBlank;
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
public class CommitDto {
    private Long id;
    @NotNull
    private Long projectId;
    @NotNull
    private Long userId;
    @NotBlank
    private String message;
    private Instant timestamp;
}
