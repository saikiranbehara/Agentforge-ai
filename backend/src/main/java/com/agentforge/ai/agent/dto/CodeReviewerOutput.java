package com.agentforge.ai.agent.dto;

import com.agentforge.ai.model.CodeReviewReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeReviewerOutput {
    private boolean success;
    private CodeReviewReport report;
    private String summary;
    private String errorMessage;
    private long executionDurationMs;
}
