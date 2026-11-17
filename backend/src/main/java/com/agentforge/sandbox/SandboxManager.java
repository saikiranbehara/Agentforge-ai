package com.agentforge.sandbox;

import com.agentforge.ai.model.GeneratedSourceFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SandboxManager {

    private final DockerExecutionSandbox dockerSandbox;
    private final ProcessIsolatedSandbox processSandbox;

    public SandboxResult runInSandbox(Long projectId, List<GeneratedSourceFile> files, String command, long timeoutSeconds) {
        if (dockerSandbox.isAvailable()) {
            log.info("Executing project {} code inside Docker container sandbox", projectId);
            return dockerSandbox.executeCode(projectId, files, command, timeoutSeconds);
        } else {
            log.info("Docker daemon not detected; falling back to Isolated Process Watchdog sandbox for project {}", projectId);
            return processSandbox.executeCode(projectId, files, command, timeoutSeconds);
        }
    }

    public String getActiveSandboxType() {
        return dockerSandbox.isAvailable() ? dockerSandbox.getProviderName() : processSandbox.getProviderName();
    }
}
