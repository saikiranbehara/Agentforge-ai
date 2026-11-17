package com.agentforge.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {
    private long totalProjects;
    private long activeProjects;
    private long completedProjects;
    private long failedProjects;
    private List<ProjectResponse> recentProjects;
    private List<AgentExecutionResponse> recentExecutions;
}
