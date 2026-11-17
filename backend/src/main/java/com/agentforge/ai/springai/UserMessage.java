package com.agentforge.ai.springai;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserMessage implements Message {
    private final String content;

    @Override
    public MessageType getMessageType() {
        return MessageType.USER;
    }
}
