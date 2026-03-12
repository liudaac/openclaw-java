package openclaw.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * OpenClaw Server Application - Phase 1 Entry Point
 *
 * <p>This is the main entry point for the OpenClaw Java server implementation.
 * It provides HTTP and WebSocket endpoints for the OpenClaw gateway.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
@SpringBootApplication(scanBasePackages = {"openclaw.server", "openclaw.gateway", "openclaw.agent"})
@EnableAsync
public class OpenClawServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenClawServerApplication.class, args);
    }
}
