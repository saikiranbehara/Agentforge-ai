package com.agentforge.repository;

import com.agentforge.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    Optional<Agent> findByAgentType(String agentType);
    List<Agent> findAllByOrderByOrderIndexAsc();
}
