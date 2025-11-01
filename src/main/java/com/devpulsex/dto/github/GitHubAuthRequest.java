package com.devpulsex.dto.github;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GitHubAuthRequest {
    @NotBlank
    private String code;
}
