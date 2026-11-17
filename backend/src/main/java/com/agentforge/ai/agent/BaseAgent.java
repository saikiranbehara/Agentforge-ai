package com.agentforge.ai.agent;

public interface BaseAgent {
    String getAgentType();
    String getAgentName();
    AgentExecutionResult execute(AgentContext context);
}
