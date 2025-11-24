package com.devpulsex.dto.trello;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrelloAuthRequest {
    @NotBlank(message = "Token is required")
    private String token;
}
