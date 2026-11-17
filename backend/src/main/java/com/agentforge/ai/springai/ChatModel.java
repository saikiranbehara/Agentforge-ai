package com.agentforge.ai.springai;

public interface ChatModel {
    ChatResponse call(Prompt prompt);
    
    default String call(String message) {
        Prompt prompt = new Prompt(message);
        ChatResponse response = call(prompt);
        return (response != null && response.getResult() != null) ? response.getResult().getText() : "";
    }
}
