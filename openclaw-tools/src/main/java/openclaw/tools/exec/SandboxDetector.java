package openclaw.tools.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Detects whether the current execution environment is a sandboxed container.
 * Supports Docker, Containerd, and other container runtimes.
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 */
public class SandboxDetector {

    private static final Logger logger = LoggerFactory.getLogger(SandboxDetector.class);

    // Container marker files
    private static final String DOCKERENV_FILE = "/.dockerenv";
    private static final String CONTAINERD_ENV_FILE = "/run/.containerenv";
    private static final String CGROUP_FILE = "/proc/self/cgroup";
    private static final String PROC_1_CGROUP = "/proc/1/cgroup";

    // Container indicators in cgroup
    private static final String[] CONTAINER_INDICATORS = {
            "docker",
            "containerd",
            "crio",
            "podman",
            "kubepods",
            "lxc",
            "container"
    };

    private final Boolean sandboxedOverride;

    /**
     * Creates a SandboxDetector with automatic detection.
     */
    public SandboxDetector() {
        this(null);
    }

    /**
     * Creates a SandboxDetector with optional override.
     *
     * @param sandboxedOverride if non-null, forces sandbox status to this value
     */
    public SandboxDetector(Boolean sandboxedOverride) {
        this.sandboxedOverride = sandboxedOverride;
    }

    /**
     * Checks if the current environment is running inside a sandboxed container.
     *
     * @return true if running in a sandbox, false otherwise
     */
    public boolean isSandboxed() {
        if (sandboxedOverride != null) {
            logger.debug("Sandbox status overridden to: {}", sandboxedOverride);
            return sandboxedOverride;
        }

        // Check multiple indicators
        if (checkDockerEnv()) {
            logger.debug("Docker environment detected via /.dockerenv");
            return true;
        }

        if (checkContainerdEnv()) {
            logger.debug("Containerd environment detected via /run/.containerenv");
            return true;
        }

        if (checkCgroup()) {
            logger.debug("Container detected via cgroup inspection");
            return true;
        }

        if (checkRestrictedUser()) {
            logger.debug("Restricted user environment detected");
            return true;
        }

        logger.debug("No sandbox environment detected");
        return false;
    }

    /**
     * Checks for Docker's .dockerenv file.
     */
    private boolean checkDockerEnv() {
        return Files.exists(Paths.get(DOCKERENV_FILE));
    }

    /**
     * Checks for Containerd's .containerenv file.
     */
    private boolean checkContainerdEnv() {
        return Files.exists(Paths.get(CONTAINERD_ENV_FILE));
    }

    /**
     * Checks cgroup files for container indicators.
     */
    private boolean checkCgroup() {
        // Check /proc/self/cgroup first
        if (checkCgroupFile(CGROUP_FILE)) {
            return true;
        }

        // Fallback to /proc/1/cgroup
        if (checkCgroupFile(PROC_1_CGROUP)) {
            return true;
        }

        return false;
    }

    /**
     * Reads a cgroup file and checks for container indicators.
     */
    private boolean checkCgroupFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String lowerLine = line.toLowerCase();
                for (String indicator : CONTAINER_INDICATORS) {
                    if (lowerLine.contains(indicator)) {
                        logger.debug("Container indicator '{}' found in cgroup: {}", indicator, line);
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("Failed to read cgroup file {}: {}", path, e.getMessage());
        }

        return false;
    }

    /**
     * Checks if running as a restricted/non-root user in a container-like environment.
     * This is a supplementary check for cases where container detection might fail.
     */
    private boolean checkRestrictedUser() {
        // Check if we're running as root (UID 0)
        String uid = System.getProperty("user.name");
        if ("root".equals(uid)) {
            // Running as root - less likely to be a restricted sandbox
            return false;
        }

        // Check for container-specific environment variables
        String containerEnv = System.getenv("container");
        if (containerEnv != null && !containerEnv.isEmpty()) {
            logger.debug("Container environment variable detected: {}", containerEnv);
            return true;
        }

        // Check for Kubernetes service account (indicates pod/container)
        Path k8sServiceAccount = Paths.get("/var/run/secrets/kubernetes.io");
        if (Files.exists(k8sServiceAccount)) {
            logger.debug("Kubernetes service account directory detected");
            return true;
        }

        return false;
    }

    /**
     * Gets a detailed description of the sandbox status.
     *
     * @return a human-readable description of the sandbox detection results
     */
    public String getSandboxStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Sandbox Detection Results:\n");
        status.append("  Docker env (/.dockerenv): ").append(checkDockerEnv() ? "YES" : "NO").append("\n");
        status.append("  Containerd env (/run/.containerenv): ").append(checkContainerdEnv() ? "YES" : "NO").append("\n");
        status.append("  Cgroup container indicators: ").append(checkCgroup() ? "YES" : "NO").append("\n");
        status.append("  Restricted user/K8s: ").append(checkRestrictedUser() ? "YES" : "NO").append("\n");
        status.append("  Overall sandboxed: ").append(isSandboxed() ? "YES" : "NO");
        return status.toString();
    }
}
