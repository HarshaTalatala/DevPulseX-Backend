package com.devpulsex.repository;

import com.devpulsex.model.Issue;
import com.devpulsex.model.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    long countByProject_Id(Long projectId);
    long countByProject_IdAndStatus(Long projectId, IssueStatus status);
    long countByUser_Id(Long userId);
    long countByUser_IdAndStatus(Long userId, IssueStatus status);
    long countByStatus(IssueStatus status);
    List<Issue> findByProject_Id(Long projectId);
}
