package com.devpulsex.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.model.Project;
import com.devpulsex.model.Team;
import com.devpulsex.model.User;
import com.devpulsex.repository.ProjectRepository;
import com.devpulsex.repository.TeamRepository;
import com.devpulsex.repository.UserRepository;

@Service
public class AuthorizationScopeService {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;

    public AuthorizationScopeService(UserRepository userRepository,
            TeamRepository teamRepository,
            ProjectRepository projectRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.projectRepository = projectRepository;
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
        if (team == null || team.getId() == null) {
            return false;
        }
        return teamRepository.existsByIdAndMembers_Id(team.getId(), currentUser.getId());
    }

    public boolean hasProjectAccess(Project project) {
        if (project == null || project.getId() == null) {
            return false;
        }

        User currentUser = getCurrentUser();
        if (isAdmin(currentUser)) {
            return true;
        }

        return projectRepository.existsByIdAndTeam_Members_Id(project.getId(), currentUser.getId());
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
