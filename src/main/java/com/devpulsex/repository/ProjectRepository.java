package com.devpulsex.repository;

import com.devpulsex.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByName(String name);

    boolean existsByIdAndTeam_Members_Id(Long projectId, Long memberId);
}
