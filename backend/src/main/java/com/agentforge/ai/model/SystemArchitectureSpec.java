package com.agentforge.ai.model;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemArchitectureSpec {
    private String architecturePattern;
    private String overview;
    private Map<String, String> techStack;
    private List<TableSpec> databaseSchema;
    private List<ApiEndpointSpec> apiEndpoints;
    private List<ModuleDependencySpec> moduleDependencies;
    private List<String> folderStructure;
    private List<String> designPatterns;
    private List<String> securityConsiderations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TableSpec {
        private String tableName;
        private String description;
        private List<ColumnSpec> columns;
        private List<String> relationships;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnSpec {
        private String name;
        private String type;
        private boolean primaryKey;
        private boolean nullable;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiEndpointSpec {
        private String method; // GET, POST, PUT, DELETE
        private String path;
        private String summary;
        private String requestBody;
        private String responseBody;
        private String authRequired;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModuleDependencySpec {
        private String module;
        private List<String> dependsOn;
        private String communication;
    }
}
