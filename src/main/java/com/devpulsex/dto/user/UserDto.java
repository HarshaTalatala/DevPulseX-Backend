package com.devpulsex.dto.user;

import com.devpulsex.model.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private Role role;
    
    // GitHub OAuth fields
    private Long githubId;
    private String githubUsername;
    private String githubAvatarUrl;
    
    // Google OAuth fields
    private String googleId;
    private String googleEmail;
    private String googleName;
    private String googlePictureUrl;
    
    // Trello OAuth fields
    private String trelloId;
    private String trelloUsername;
}
