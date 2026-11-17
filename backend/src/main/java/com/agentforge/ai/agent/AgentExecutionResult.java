package com.agentforge.ai.agent;

import com.agentforge.ai.model.GeneratedSourceFile;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentExecutionResult {
    private boolean success;
    private String outputData;
    private String summary;
    private String errorMessage;
    private long executionTimeMs;
    
    @Builder.Default
    private List<GeneratedSourceFile> files = new ArrayList<>();
}
