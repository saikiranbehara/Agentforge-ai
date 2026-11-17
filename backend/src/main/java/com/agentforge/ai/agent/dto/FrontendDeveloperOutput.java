package com.agentforge.ai.agent.dto;

import com.agentforge.ai.model.GeneratedSourceFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrontendDeveloperOutput {
    private boolean success;
    @Builder.Default
    private List<GeneratedSourceFile> files = new ArrayList<>();
    private String summary;
    private String errorMessage;
    private long executionDurationMs;
}
