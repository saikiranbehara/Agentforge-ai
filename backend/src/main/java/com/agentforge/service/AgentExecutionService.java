package com.agentforge.service;

import com.agentforge.dto.AgentExecutionResponse;
import com.agentforge.entity.Agent;
import com.agentforge.entity.AgentExecution;
import com.agentforge.repository.AgentExecutionRepository;
import com.agentforge.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentExecutionService {

    private final AgentExecutionRepository executionRepository;
    private final AgentRepository agentRepository;

    @Transactional(readOnly = true)
    public List<AgentExecutionResponse> getProjectExecutions(Long projectId) {
        List<Agent> allAgents = agentRepository.findAllByOrderByOrderIndexAsc();
        List<AgentExecution> executions = executionRepository.findByProjectIdOrderByStartedAtAsc(projectId);

        Map<Long, AgentExecution> latestExecByAgent = executions.stream()
                .collect(Collectors.toMap(
                        e -> e.getAgent().getId(),
                        Function.identity(),
                        (existing, replacing) -> replacing
                ));

        List<AgentExecutionResponse> responseList = new ArrayList<>();

        for (Agent agent : allAgents) {
            AgentExecution exec = latestExecByAgent.get(agent.getId());
            if (exec != null) {
                responseList.add(mapToDto(exec));
            } else {
                // Pending agent placeholder
                responseList.add(AgentExecutionResponse.builder()
                        .projectId(projectId)
                        .agentId(agent.getId())
                        .agentName(agent.getAgentName())
                        .agentType(agent.getAgentType())
                        .iconName(agent.getIconName())
                        .orderIndex(agent.getOrderIndex())
                        .executionStatus("PENDING")
                        .progressPercentage(0)
                        .currentTask("Awaiting prior agent pipeline completion")
                        .build());
            }
        }

        return responseList;
    }

    private AgentExecutionResponse mapToDto(AgentExecution exec) {
        return AgentExecutionResponse.builder()
                .id(exec.getId())
                .projectId(exec.getProject().getId())
                .agentId(exec.getAgent().getId())
                .agentName(exec.getAgent().getAgentName())
                .agentType(exec.getAgent().getAgentType())
                .iconName(exec.getAgent().getIconName())
                .orderIndex(exec.getAgent().getOrderIndex())
                .executionStatus(exec.getExecutionStatus())
                .progressPercentage(exec.getProgressPercentage())
                .currentTask(exec.getCurrentTask())
                .inputData(exec.getInputData())
                .outputData(exec.getOutputData())
                .errorMessage(exec.getErrorMessage())
                .durationMs(exec.getDurationMs())
                .startedAt(exec.getStartedAt())
                .completedAt(exec.getCompletedAt())
                .build();
    }
}
