package com.agentforge.ai.agent.dto;

import com.agentforge.ai.model.SystemArchitectureSpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemArchitectOutput {
    private boolean success;
    private SystemArchitectureSpec architectureSpec;
    private String summary;
    private String rawJson;
    private String errorMessage;
    private long executionDurationMs;
}
