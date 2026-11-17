package com.agentforge.sandbox;

import com.agentforge.ai.model.GeneratedSourceFile;
import java.util.List;

public interface ExecutionSandbox {
    SandboxResult executeCode(Long projectId, List<GeneratedSourceFile> files, String command, long timeoutSeconds);
    boolean isAvailable();
    String getProviderName();
}
