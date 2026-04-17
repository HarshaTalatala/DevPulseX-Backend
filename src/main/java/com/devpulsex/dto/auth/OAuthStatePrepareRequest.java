package com.devpulsex.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthStatePrepareRequest {
    @NotBlank
    private String state;
}
