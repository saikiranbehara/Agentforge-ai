package com.agentforge.sandbox;

import com.agentforge.ai.model.GeneratedSourceFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DockerExecutionSandbox implements ExecutionSandbox {

    private final boolean dockerAvailable;

    public DockerExecutionSandbox() {
        this.dockerAvailable = checkDockerAvailability();
    }

    private boolean checkDockerAvailability() {
        try {
            Process process = new ProcessBuilder("docker", "--version").start();
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.info("Docker is not available on host system. Fallback sandbox will be utilized.");
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return dockerAvailable;
    }

    @Override
    public String getProviderName() {
        return "DOCKER_CONTAINER";
    }

    @Override
    public SandboxResult executeCode(Long projectId, List<GeneratedSourceFile> files, String command, long timeoutSeconds) {
        long start = System.currentTimeMillis();
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("agentforge_docker_sandbox_" + projectId + "_");

            // Write files into isolated workspace
            for (GeneratedSourceFile file : files) {
                Path targetPath = tempDir.resolve(file.getFilePath());
                Files.createDirectories(targetPath.getParent());
                Files.writeString(targetPath, file.getContent());
            }

            // Docker container command with CPU limit, Memory limit, and Network isolation
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "--cpus=1.0",
                    "--memory=512m",
                    "--network=none",
                    "-v", tempDir.toAbsolutePath().toString() + ":/workspace",
                    "-w", "/workspace",
                    "maven:3.9-eclipse-temurin-21-alpine",
                    "sh", "-c", command
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - start;

            if (!finished) {
                process.destroyForcibly();
                return SandboxResult.builder()
                        .success(false)
                        .timedOut(true)
                        .exitCode(-1)
                        .stderr("Execution timed out after " + timeoutSeconds + " seconds")
                        .executionTimeMs(duration)
                        .isolationTier(getProviderName())
                        .build();
            }

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.exitValue();

            return SandboxResult.builder()
                    .success(exitCode == 0)
                    .exitCode(exitCode)
                    .stdout(output)
                    .executionTimeMs(duration)
                    .isolationTier(getProviderName())
                    .build();

        } catch (Exception e) {
            log.error("Docker sandbox execution encountered error", e);
            long duration = System.currentTimeMillis() - start;
            return SandboxResult.builder()
                    .success(false)
                    .exitCode(-1)
                    .stderr(e.getMessage())
                    .executionTimeMs(duration)
                    .isolationTier(getProviderName())
                    .build();
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private void cleanupTempDir(Path tempDir) {
        if (tempDir != null && Files.exists(tempDir)) {
            try {
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                log.warn("Failed to clean up temp sandbox dir: {}", tempDir, e);
            }
        }
    }
}
