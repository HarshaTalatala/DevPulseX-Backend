package com.devpulsex.repository;

import com.devpulsex.model.Deployment;
import com.devpulsex.model.DeploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    long countByProject_Id(Long projectId);
    long countByStatus(DeploymentStatus status);
    long countByProject_IdAndStatus(Long projectId, DeploymentStatus status);
    List<Deployment> findByProject_IdOrderByTimestampDesc(Long projectId);
    Optional<Deployment> findFirstByProject_IdOrderByTimestampDesc(Long projectId);
}
