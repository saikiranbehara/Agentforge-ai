package com.agentforge.ai.springai;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SystemMessage implements Message {
    private final String content;

    @Override
    public MessageType getMessageType() {
        return MessageType.SYSTEM;
    }
}
