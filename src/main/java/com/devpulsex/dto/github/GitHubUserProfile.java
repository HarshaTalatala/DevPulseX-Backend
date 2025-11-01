package com.devpulsex.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GitHubUserProfile {
    private Long id;
    private String login;
    private String name;
    private String email;
    @JsonProperty("avatar_url")
    private String avatarUrl;
}
