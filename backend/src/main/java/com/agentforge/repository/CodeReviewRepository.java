package com.agentforge.repository;

import com.agentforge.entity.CodeReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {
    List<CodeReview> findByProjectIdOrderByIdAsc(Long projectId);
    long countByProjectId(Long projectId);
    long countByProjectIdAndSeverity(Long projectId, String severity);
    void deleteByProjectId(Long projectId);
}
