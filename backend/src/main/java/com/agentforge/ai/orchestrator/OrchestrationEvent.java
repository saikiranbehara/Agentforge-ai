package com.agentforge.ai.orchestrator;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrchestrationEvent {
    private Long projectId;
    private String agentType;
    private String agentName;
    private String status; // PENDING, RUNNING, COMPLETED, FAILED, WAITING_APPROVAL
    private int progressPercentage;
    private String currentTask;
    private String message;
    private Long durationMs;
    private LocalDateTime timestamp;
}
