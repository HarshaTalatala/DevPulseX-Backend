package com.devpulsex.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.devpulsex.dto.team.TeamDto;
import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.model.Team;
import com.devpulsex.model.User;
import com.devpulsex.repository.TeamRepository;
import com.devpulsex.repository.UserRepository;

@Service
public class TeamService {
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    public List<TeamDto> getAll() {
        return teamRepository.findAll().stream().map(this::toDto).toList();
    }

    public TeamDto getById(Long id) {
        return teamRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
    }

    public TeamDto create(TeamDto dto) {
        Team team = new Team();
        team.setName(dto.getName());
        team.setMembers(resolveMembers(dto.getMemberIds()));
        return toDto(teamRepository.save(team));
    }

    public TeamDto update(Long id, TeamDto dto) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
        team.setName(dto.getName());
        team.setMembers(resolveMembers(dto.getMemberIds()));
        return toDto(teamRepository.save(team));
    }

    public void delete(Long id) {
        if (!teamRepository.existsById(id)) {
            throw new ResourceNotFoundException("Team not found: " + id);
        }
        teamRepository.deleteById(id);
    }

    private Set<User> resolveMembers(Set<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return new HashSet<>();
        List<User> users = userRepository.findAllById(memberIds);
        return new HashSet<>(users);
    }

    private TeamDto toDto(Team t) {
        Set<Long> memberIds = t.getMembers() == null ? Set.of() : t.getMembers().stream().map(User::getId).collect(java.util.stream.Collectors.toSet());
        return TeamDto.builder()
                .id(t.getId())
                .name(t.getName())
                .memberIds(memberIds)
                .build();
    }
}
