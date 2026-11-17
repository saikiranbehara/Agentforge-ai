package com.agentforge.ai.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedSourceFile {
    private String filePath;
    private String content;
    private String language;
    private String description;
}
