package com.agentforge.repository;

import com.agentforge.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {
    List<TestResult> findByProjectIdOrderByIdAsc(Long projectId);
    long countByProjectId(Long projectId);
    long countByProjectIdAndTestStatus(Long projectId, String testStatus);
    void deleteByProjectId(Long projectId);
}
