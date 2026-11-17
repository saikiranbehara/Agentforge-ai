package com.agentforge.repository;

import com.agentforge.entity.AgentExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentExecutionRepository extends JpaRepository<AgentExecution, Long> {
    List<AgentExecution> findByProjectIdOrderByStartedAtAsc(Long projectId);
    Optional<AgentExecution> findByProjectIdAndAgentId(Long projectId, Long agentId);
    void deleteByProjectId(Long projectId);
}
