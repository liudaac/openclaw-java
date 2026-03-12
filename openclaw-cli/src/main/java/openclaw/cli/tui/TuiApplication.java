package openclaw.cli.tui;

import openclaw.cli.service.AgentService;
import openclaw.cli.service.SessionService;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Scanner;

/**
 * TUI (Text User Interface) 应用
 * 
 * 功能:
 * - 交互式命令行界面
 * - 会话管理
 * - 消息发送/接收
 * - 状态显示
 * 
 * 对应 Node.js: TUI 功能
 */
public class TuiApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(TuiApplication.class);
    
    private Terminal terminal;
    private AgentService agentService;
    private SessionService sessionService;
    private String currentSession;
    private boolean running = false;
    
    public void start() {
        try {
            terminal = TerminalBuilder.builder()
                .system(true)
                .build();
            
            agentService = new AgentService();
            sessionService = new SessionService();
            
            // 推断活动 agent
            String activeAgent = inferActiveAgent();
            
            running = true;
            
            printWelcome();
            printHelp();
            
            Scanner scanner = new Scanner(terminal.input());
            
            while (running) {
                printPrompt();
                
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    continue;
                }
                
                processCommand(input);
            }
            
        } catch (IOException e) {
            logger.error("Failed to start TUI", e);
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * 推断活动 agent
     */
    private String inferActiveAgent() {
        // 从当前工作空间推断
        String cwd = System.getProperty("user.dir");
        
        // 检查是否在 agent 工作空间
        if (cwd.contains("/workspace/") || cwd.contains("\\workspace\\")) {
            // 提取 agent 名称
            String[] parts = cwd.split("[/\\]");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("workspace".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
        }
        
        // 默认返回 main
        return "main";
    }
    
    /**
     * 处理命令
     */
    private void processCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        switch (command) {
            case "/help":
            case "/h":
                printHelp();
                break;
                
            case "/quit":
            case "/q":
            case "/exit":
                running = false;
                println("Goodbye!");
                break;
                
            case "/session":
            case "/s":
                handleSessionCommand(args);
                break;
                
            case "/new":
                currentSession = null;
                println("Started new session");
                break;
                
            case "/clear":
            case "/cls":
                clearScreen();
                break;
                
            case "/status":
                printStatus();
                break;
                
            case "/agents":
                listAgents();
                break;
                
            case "/models":
                listModels();
                break;
                
            default:
                // 发送消息
                if (input.startsWith("/")) {
                    println("Unknown command: " + command);
                    println("Type /help for available commands");
                } else {
                    sendMessage(input);
                }
        }
    }
    
    /**
     * 处理会话命令
     */
    private void handleSessionCommand(String args) {
        if (args.isEmpty()) {
            if (currentSession != null) {
                println("Current session: " + currentSession);
            } else {
                println("No active session");
            }
            return;
        }
        
        String[] parts = args.split("\\s+");
        String subCommand = parts[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
            case "ls":
                listSessions();
                break;
                
            case "switch":
            case "use":
                if (parts.length > 1) {
                    currentSession = parts[1];
                    println("Switched to session: " + currentSession);
                } else {
                    println("Usage: /session switch <session-key>");
                }
                break;
                
            default:
                println("Unknown session command: " + subCommand);
        }
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(String message) {
        if (currentSession == null) {
            // 创建新会话
            currentSession = sessionService.createSession();
            println("Created new session: " + currentSession);
        }
        
        println("You: " + message);
        
        // 发送消息到 agent
        try {
            String response = agentService.sendMessage(currentSession, message);
            println("Assistant: " + response);
        } catch (Exception e) {
            println("Error: " + e.getMessage());
        }
    }
    
    /**
     * 打印欢迎信息
     */
    private void printWelcome() {
        println();
        println("╔════════════════════════════════════════╗");
        println("║     🦞 OpenClaw Java TUI v2026.3.9    ║");
        println("╚════════════════════════════════════════╝");
        println();
        println("Active agent: " + inferActiveAgent());
        println();
    }
    
    /**
     * 打印帮助
     */
    private void printHelp() {
        println();
        println("Commands:");
        println("  /help, /h          Show this help");
        println("  /quit, /q, /exit   Exit TUI");
        println("  /new               Start new session");
        println("  /session           Show current session");
        println("  /session list      List all sessions");
        println("  /session switch    Switch to session");
        println("  /clear, /cls       Clear screen");
        println("  /status            Show status");
        println("  /agents            List agents");
        println("  /models            List models");
        println();
        println("Type a message to send to the assistant");
        println();
    }
    
    /**
     * 打印状态
     */
    private void printStatus() {
        println();
        println("Status:");
        println("  Version: 2026.3.9");
        println("  Agent: " + inferActiveAgent());
        println("  Session: " + (currentSession != null ? currentSession : "none"));
        println();
    }
    
    /**
     * 列出会话
     */
    private void listSessions() {
        println();
        println("Sessions:");
        // 这里应该从 sessionService 获取会话列表
        println("  (Session list not implemented)");
        println();
    }
    
    /**
     * 列出 agents
     */
    private void listAgents() {
        println();
        println("Agents:");
        println("  main");
        println("  (Agent list not fully implemented)");
        println();
    }
    
    /**
     * 列出模型
     */
    private void listModels() {
        println();
        println("Models:");
        println("  gpt-4");
        println("  gpt-4-turbo");
        println("  gpt-3.5-turbo");
        println("  claude-3-opus");
        println("  claude-3.5-sonnet");
        println("  llama3");
        println();
    }
    
    /**
     * 清屏
     */
    private void clearScreen() {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();
    }
    
    /**
     * 打印提示符
     */
    private void printPrompt() {
        if (currentSession != null) {
            print("[" + currentSession.substring(0, Math.min(8, currentSession.length())) + "] > ");
        } else {
            print("[new] > ");
        }
    }
    
    /**
     * 打印文本
     */
    private void print(String text) {
        System.out.print(text);
    }
    
    /**
     * 打印文本并换行
     */
    private void println(String text) {
        System.out.println(text);
    }
    
    private void println() {
        System.out.println();
    }
}
