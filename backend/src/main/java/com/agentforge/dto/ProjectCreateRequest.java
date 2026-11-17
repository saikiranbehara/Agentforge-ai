package com.agentforge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCreateRequest {
    @NotBlank(message = "Project name is required")
    private String projectName;

    private String description;

    @NotBlank(message = "Requirements are required")
    private String requirements;

    private String backendFramework;
    private String frontendFramework;
    private String databaseType;
    private String aiModel;
}
