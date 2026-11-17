package com.agentforge.ai.model;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementSpec {
    private String applicationName;
    private String summary;
    private List<String> functionalRequirements;
    private List<String> nonFunctionalRequirements;
    private List<ModuleSpec> modules;
    private List<String> userStories;
    private List<String> dependencies;
    private List<String> assumptions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModuleSpec {
        private String name;
        private String description;
        private List<String> keyFeatures;
    }
}
