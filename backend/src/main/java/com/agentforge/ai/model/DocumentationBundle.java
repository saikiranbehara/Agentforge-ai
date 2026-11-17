package com.agentforge.ai.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentationBundle {
    private String readme;
    private String architectureDoc;
    private String installationGuide;
    private String apiDocs;
    private String deploymentGuide;
}
