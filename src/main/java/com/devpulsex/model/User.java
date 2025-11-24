package com.devpulsex.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    // Google OAuth fields
    @Column(name = "google_id")
    private String googleId;

    @Column(name = "google_email")
    private String googleEmail;

    @Column(name = "google_name")
    private String googleName;

    @Column(name = "google_picture_url")
    private String googlePictureUrl;

    // Note: In production, encrypt or store securely (e.g., token table + KMS)
    @Column(name = "google_access_token", length = 2048)
    private String googleAccessToken;

    @Column(name = "google_refresh_token", length = 2048)
    private String googleRefreshToken;

    // Trello OAuth fields
    @Column(name = "trello_id")
    private String trelloId;

    @Column(name = "trello_username")
    private String trelloUsername;

    // Note: In production, encrypt or store securely (e.g., token table + KMS)
    @Column(name = "trello_access_token", length = 2048)
    private String trelloAccessToken;
}
