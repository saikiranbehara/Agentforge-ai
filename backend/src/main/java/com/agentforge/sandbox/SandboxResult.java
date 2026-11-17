package com.agentforge.sandbox;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SandboxResult {
    private boolean success;
    private int exitCode;
    private String stdout;
    private String stderr;
    private long executionTimeMs;
    private boolean timedOut;
    private String isolationTier; // "DOCKER_CONTAINER" or "ISOLATED_PROCESS_WATCHDOG"
}
