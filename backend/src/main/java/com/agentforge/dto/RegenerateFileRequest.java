package com.agentforge.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegenerateFileRequest {
    private String customInstructions;
}
