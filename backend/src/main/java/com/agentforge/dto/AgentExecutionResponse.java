package com.agentforge.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentExecutionResponse {
    private Long id;
    private Long projectId;
    private Long agentId;
    private String agentName;
    private String agentType;
    private String iconName;
    private Integer orderIndex;
    private String executionStatus;
    private Integer progressPercentage;
    private String currentTask;
    private String inputData;
    private String outputData;
    private String errorMessage;
    private Long durationMs;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
