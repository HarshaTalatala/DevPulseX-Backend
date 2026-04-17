package com.devpulsex.service;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.model.Project;
import com.devpulsex.model.Team;
import com.devpulsex.model.User;
import com.devpulsex.repository.UserRepository;

@Service
public class AuthorizationScopeService {
    private final UserRepository userRepository;

    public AuthorizationScopeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("Authentication required");
        }

        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + auth.getName()));
    }

    public boolean isAdmin(User user) {
        return user.getRole() != null && user.getRole().name().equals("ADMIN");
    }

    public boolean hasTeamAccess(Team team) {
        User currentUser = getCurrentUser();
        if (isAdmin(currentUser)) {
            return true;
        }
        if (team == null || team.getMembers() == null) {
            return false;
        }
        return team.getMembers().stream().anyMatch(member -> Objects.equals(member.getId(), currentUser.getId()));
    }

    public boolean hasProjectAccess(Project project) {
        if (project == null) {
            return false;
        }
        return hasTeamAccess(project.getTeam());
    }

    public void requireTeamAccess(Team team) {
        if (!hasTeamAccess(team)) {
            throw new AccessDeniedException("You do not have access to this team");
        }
    }

    public void requireProjectAccess(Project project) {
        if (!hasProjectAccess(project)) {
            throw new AccessDeniedException("You do not have access to this project");
        }
    }
}
