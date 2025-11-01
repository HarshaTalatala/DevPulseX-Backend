package com.devpulsex.repository;

import com.devpulsex.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByGithubId(Long githubId);
    Optional<User> findByGithubUsername(String githubUsername);
}
