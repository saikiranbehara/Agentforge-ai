package com.agentforge.ai.springai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Generation {
    private AssistantMessage output;

    public String getText() {
        return output != null ? output.getContent() : "";
    }
}
