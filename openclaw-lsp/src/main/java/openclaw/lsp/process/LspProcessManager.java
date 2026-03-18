package openclaw.lsp.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages LSP server processes.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class LspProcessManager {

    private static final Logger logger = LoggerFactory.getLogger(LspProcessManager.class);

    private final List<Process> processes = new ArrayList<>();

    /**
     * Start an LSP server process.
     *
     * @param command the command to execute
     * @param args the arguments
     * @return the started process
     * @throws IOException if process fails to start
     */
    public Process startProcess(String command, String[] args) throws IOException {
        List<String> commandList = new ArrayList<>();
        commandList.add(command);
        if (args != null) {
            for (String arg : args) {
                commandList.add(arg);
            }
        }

        ProcessBuilder builder = new ProcessBuilder(commandList);
        builder.redirectErrorStream(true);

        logger.info("Starting LSP server: {}", String.join(" ", commandList));
        Process process = builder.start();
        processes.add(process);

        return process;
    }

    /**
     * Stop an LSP server process gracefully.
     *
     * @param process the process to stop
     * @return true if stopped successfully
     */
    public boolean stopProcess(Process process) {
        if (process == null || !process.isAlive()) {
            return true;
        }

        logger.info("Stopping LSP server process");
        processes.remove(process);

        // Try graceful shutdown
        process.destroy();

        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                logger.warn("Process did not terminate gracefully, forcing");
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }

        return !process.isAlive();
    }

    /**
     * Check if a process is running.
     *
     * @param process the process
     * @return true if running
     */
    public boolean isRunning(Process process) {
        return process != null && process.isAlive();
    }

    /**
     * Stop all managed processes.
     */
    public void stopAll() {
        logger.info("Stopping all LSP server processes");
        List<Process> toStop = new ArrayList<>(processes);
        processes.clear();

        for (Process process : toStop) {
            stopProcess(process);
        }
    }

    /**
     * Get the number of managed processes.
     *
     * @return the count
     */
    public int getProcessCount() {
        return processes.size();
    }
}
