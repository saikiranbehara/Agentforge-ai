package com.agentforge.ai.springai;

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
public class ChatResponse {
    @Builder.Default
    private List<Generation> generations = new ArrayList<>();

    public Generation getResult() {
        if (generations != null && !generations.isEmpty()) {
            return generations.get(0);
        }
        return new Generation(new AssistantMessage(""));
    }
}
