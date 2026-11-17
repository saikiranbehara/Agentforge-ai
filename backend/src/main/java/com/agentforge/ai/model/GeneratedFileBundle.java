package com.agentforge.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedFileBundle {
    @Builder.Default
    private List<GeneratedSourceFile> files = new ArrayList<>();
    private String summary;
}
