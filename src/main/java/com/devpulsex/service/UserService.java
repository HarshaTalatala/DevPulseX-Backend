package com.devpulsex.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.devpulsex.dto.user.CreateUserRequest;
import com.devpulsex.dto.user.UpdateUserRequest;
import com.devpulsex.dto.user.UserDto;
import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.model.User;
import com.devpulsex.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDto> getAll() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    public UserDto getById(Long id) {
        return userRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public UserDto findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public UserDto create(CreateUserRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .build();
        return toDto(userRepository.save(user));
    }

    public UserDto update(Long id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (!user.getEmail().equalsIgnoreCase(req.getEmail()) && userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setRole(req.getRole());
        return toDto(userRepository.save(user));
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    // Public method for auth controllers to build complete UserDto
    public UserDto toDto(User u) {
        return buildUserDto(u);
    }

    private UserDto buildUserDto(User u) {
        return UserDto.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .role(u.getRole())
                .githubId(u.getGithubId())
                .githubUsername(u.getGithubUsername())
                .githubAvatarUrl(u.getGithubAvatarUrl())
                .googleId(u.getGoogleId())
                .googleEmail(u.getGoogleEmail())
                .googleName(u.getGoogleName())
                .googlePictureUrl(u.getGooglePictureUrl())
                .trelloId(u.getTrelloId())
                .trelloUsername(u.getTrelloUsername())
                .build();
    }
}
