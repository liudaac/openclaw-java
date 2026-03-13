package openclaw.cron.executor;

import openclaw.cron.model.CronJob;
import openclaw.cron.model.JobExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Isolated job executor that runs commands in separate processes.
 * 
 * <p>Provides process isolation for security and resource management.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class IsolatedJobExecutor implements JobExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(IsolatedJobExecutor.class);
    
    @Override
    public CompletableFuture<JobExecution> execute(CronJob job) {
        return CompletableFuture.supplyAsync(() -> {
            JobExecution execution = new JobExecution(job.getId());
            execution.setExecutorHost(getHostname());
            
            logger.info("Starting isolated execution for job {}: {}", job.getId(), job.getName());
            
            try {
                ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", job.getCommand());
                
                // Set working directory if specified
                if (job.getWorkingDirectory() != null) {
                    pb.directory(new java.io.File(job.getWorkingDirectory()));
                }
                
                // Redirect error stream to output
                pb.redirectErrorStream(true);
                
                // Start the process
                Process process = pb.start();
                
                // Read output
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                
                // Wait for completion with timeout
                boolean finished = process.waitFor(job.getTimeoutSeconds(), TimeUnit.SECONDS);
                
                if (!finished) {
                    process.destroyForcibly();
                    execution.setStatus(JobExecution.JobExecutionStatus.TIMEOUT);
                    execution.setError("Execution timed out after " + job.getTimeoutSeconds() + " seconds");
                    logger.warn("Job {} execution timed out", job.getId());
                } else {
                    int exitCode = process.exitValue();
                    execution.setExitCode(exitCode);
                    execution.setOutput(output.toString());
                    
                    if (exitCode == 0) {
                        execution.setStatus(JobExecution.JobExecutionStatus.SUCCESS);
                        logger.info("Job {} completed successfully", job.getId());
                    } else {
                        execution.setStatus(JobExecution.JobExecutionStatus.FAILED);
                        execution.setError("Process exited with code " + exitCode);
                        logger.warn("Job {} failed with exit code {}", job.getId(), exitCode);
                    }
                }
                
            } catch (Exception e) {
                logger.error("Job {} execution failed", job.getId(), e);
                execution.setStatus(JobExecution.JobExecutionStatus.FAILED);
                execution.setError(e.getMessage());
            }
            
            return execution;
        });
    }
    
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
