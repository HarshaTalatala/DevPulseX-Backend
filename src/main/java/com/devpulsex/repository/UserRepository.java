package com.devpulsex.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devpulsex.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByGithubId(Long githubId);
    Optional<User> findByGithubUsername(String githubUsername);
    
    Optional<User> findByGoogleId(String googleId);
    
    Optional<User> findByTrelloId(String trelloId);
}
