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
public class Prompt {
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
    private ChatOptions options;

    public Prompt(String text) {
        this.messages = new ArrayList<>();
        this.messages.add(new UserMessage(text));
    }

    public Prompt(List<Message> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }
}
