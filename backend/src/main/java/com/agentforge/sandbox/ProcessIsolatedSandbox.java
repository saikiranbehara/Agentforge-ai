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
public class ProcessIsolatedSandbox implements ExecutionSandbox {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "ISOLATED_PROCESS_WATCHDOG";
    }

    @Override
    public SandboxResult executeCode(Long projectId, List<GeneratedSourceFile> files, String command, long timeoutSeconds) {
        long start = System.currentTimeMillis();
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("agentforge_proc_sandbox_" + projectId + "_");

            // Write files into isolated workspace
            for (GeneratedSourceFile file : files) {
                Path targetPath = tempDir.resolve(file.getFilePath());
                Files.createDirectories(targetPath.getParent());
                Files.writeString(targetPath, file.getContent());
            }

            // Run command inside the temp directory with restricted environment
            ProcessBuilder pb;
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            if (isWindows) {
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                pb = new ProcessBuilder("sh", "-c", command);
            }

            pb.directory(tempDir.toFile());
            pb.redirectErrorStream(true);

            // Strip environment of sensitive tokens/keys
            pb.environment().remove("GEMINI_API_KEY");
            pb.environment().remove("OPENAI_API_KEY");
            pb.environment().remove("DB_PASSWORD");
            pb.environment().remove("JWT_SECRET");

            Process process = pb.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - start;

            if (!finished) {
                process.destroyForcibly();
                return SandboxResult.builder()
                        .success(false)
                        .timedOut(true)
                        .exitCode(-1)
                        .stderr("Watchdog terminated execution: exceeded " + timeoutSeconds + "s timeout limit")
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
            log.error("Process sandbox execution failed", e);
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
