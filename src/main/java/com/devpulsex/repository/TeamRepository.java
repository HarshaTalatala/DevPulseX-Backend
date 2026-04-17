package com.devpulsex.repository;

import com.devpulsex.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByName(String name);

    boolean existsByIdAndMembers_Id(Long teamId, Long memberId);
}
