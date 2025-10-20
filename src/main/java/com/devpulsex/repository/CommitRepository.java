package com.devpulsex.repository;

import com.devpulsex.model.Commit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CommitRepository extends JpaRepository<Commit, Long> {
    long countByProject_Id(Long projectId);
    long countByUser_Id(Long userId);
    List<Commit> findByProject_IdAndTimestampBetween(Long projectId, Instant from, Instant to);
    // Efficient fetch of all commits for a given project
    List<Commit> findByProject_Id(Long projectId);
}
