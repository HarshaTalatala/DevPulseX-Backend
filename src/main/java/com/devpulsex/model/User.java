package com.devpulsex.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToMany(mappedBy = "members")
    @Builder.Default
    private Set<Team> teams = new HashSet<>();

    // GitHub OAuth fields
    @Column(name = "github_id")
    private Long githubId;

    @Column(name = "github_username")
    private String githubUsername;

    @Column(name = "github_avatar_url")
    private String githubAvatarUrl;

    // Note: In production, encrypt or store securely (e.g., token table + KMS)
    @Column(name = "github_access_token", length = 2048)
    private String githubAccessToken;
}
