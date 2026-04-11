package openclaw.cli.command;

import openclaw.cli.service.ExecPolicyService;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * Exec Policy 命令
 * 
 * 功能:
 * - openclaw exec-policy show - 显示当前执行策略
 * - openclaw exec-policy preset <name> - 应用预设策略
 * - openclaw exec-policy set - 自定义策略设置
 * 
 * 对应 Node.js: openclaw exec-policy show/preset/set
 */
@CommandLine.Command(
    name = "exec-policy",
    description = "Show or synchronize requested exec policy with host approvals",
    subcommands = {
        ExecPolicyCommand.ShowCommand.class,
        ExecPolicyCommand.PresetCommand.class,
        ExecPolicyCommand.SetCommand.class
    }
)
public class ExecPolicyCommand implements Callable<Integer> {
    
    @Override
    public Integer call() {
        System.out.println("Usage: openclaw exec-policy <show|preset|set> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  show    Show the local config policy, host approvals, and effective merge");
        System.out.println("  preset  Apply a synchronized preset: yolo, cautious, or deny-all");
        System.out.println("  set     Synchronize local config and host approvals using explicit values");
        System.out.println();
        System.out.println("Docs: https://docs.openclaw.ai/cli/approvals");
        return 0;
    }
    
    /**
     * exec-policy show 子命令
     */
    @CommandLine.Command(
        name = "show",
        description = "Show the local config policy, host approvals, and effective merge"
    )
    public static class ShowCommand implements Callable<Integer> {
        
        @CommandLine.Option(
            names = {"--json"},
            description = "Output as JSON"
        )
        private boolean json = false;
        
        @CommandLine.ParentCommand
        private ExecPolicyCommand parent;
        
        @Override
        public Integer call() {
            try {
                ExecPolicyService service = new ExecPolicyService();
                ExecPolicyService.PolicyShowResult result = service.showPolicy();
                
                if (json) {
                    System.out.println(result.toJson());
                } else {
                    printPolicyShow(result);
                }
                
                return 0;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
        
        private void printPolicyShow(ExecPolicyService.PolicyShowResult result) {
            System.out.println("\n" + bold("Exec Policy"));
            System.out.println();
            System.out.printf("%-14s %s%n", "Config:", result.getConfigPath());
            System.out.printf("%-14s %s%n", "Approvals:", result.getApprovalsPath());
            System.out.printf("%-14s %s%n", "Approvals File:", result.isApprovalsExists() ? "present" : "missing");
            System.out.println();
            System.out.println(bold("Effective Policy"));
            System.out.println();
            
            // Table header
            System.out.printf("%-12s %-24s %-24s %-16s%n", "Scope", "Requested", "Host", "Effective");
            System.out.println("-".repeat(80));
            
            // Table rows
            for (ExecPolicyService.PolicyScope scope : result.getScopes()) {
                String requested = String.format("host=%s (%s)%nsecurity=%s (%s)%nask=%s (%s)",
                    scope.getHostRequested(), scope.getHostRequestedSource(),
                    scope.getSecurityRequested(), scope.getSecurityRequestedSource(),
                    scope.getAskRequested(), scope.getAskRequestedSource());
                
                String host = String.format("security=%s (%s)%nask=%s (%s)%naskFallback=%s (%s)",
                    scope.getSecurityHost(), scope.getSecurityHostSource(),
                    scope.getAskHost(), scope.getAskHostSource(),
                    scope.getAskFallbackEffective(), scope.getAskFallbackSource());
                
                String effective = String.format("security=%s%nask=%s",
                    scope.getSecurityEffective(), scope.getAskEffective());
                
                System.out.printf("%-12s %-24s %-24s %-16s%n", 
                    scope.getScopeLabel(), 
                    requested.replace("%n", " "),
                    host.replace("%n", " "),
                    effective.replace("%n", " "));
            }
            
            System.out.println();
            System.out.println(dim(result.getNote()));
        }
        
        private String bold(String text) {
            return "\033[1m" + text + "\033[0m";
        }
        
        private String dim(String text) {
            return "\033[2m" + text + "\033[0m";
        }
    }
    
    /**
     * exec-policy preset 子命令
     */
    @CommandLine.Command(
        name = "preset",
        description = "Apply a synchronized preset: yolo, cautious, or deny-all"
    )
    public static class PresetCommand implements Callable<Integer> {
        
        @CommandLine.Parameters(
            description = "Preset name: yolo, cautious, deny-all",
            arity = "1"
        )
        private String presetName;
        
        @CommandLine.Option(
            names = {"--json"},
            description = "Output as JSON"
        )
        private boolean json = false;
        
        @CommandLine.ParentCommand
        private ExecPolicyCommand parent;
        
        @Override
        public Integer call() {
            try {
                ExecPolicyService service = new ExecPolicyService();
                
                // Validate preset name
                if (!isValidPreset(presetName)) {
                    System.err.println("Error: Unknown exec-policy preset: " + presetName);
                    System.err.println("Valid presets: yolo, cautious, deny-all");
                    return 1;
                }
                
                ExecPolicyService.PolicyApplyResult result = service.applyPreset(presetName);
                
                if (json) {
                    System.out.println(result.toJson());
                } else {
                    System.out.println("Applied exec-policy preset: " + presetName);
                    System.out.println();
                    printPolicyShow(result.getShowResult());
                }
                
                return 0;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
        
        private boolean isValidPreset(String name) {
            return "yolo".equals(name) || "cautious".equals(name) || "deny-all".equals(name);
        }
        
        private void printPolicyShow(ExecPolicyService.PolicyShowResult result) {
            System.out.println("\n" + bold("Exec Policy"));
            System.out.println();
            System.out.printf("%-14s %s%n", "Config:", result.getConfigPath());
            System.out.printf("%-14s %s%n", "Approvals:", result.getApprovalsPath());
            System.out.printf("%-14s %s%n", "Approvals File:", result.isApprovalsExists() ? "present" : "missing");
            System.out.println();
            System.out.println(bold("Effective Policy"));
            System.out.println();
            
            // Table header
            System.out.printf("%-12s %-24s %-24s %-16s%n", "Scope", "Requested", "Host", "Effective");
            System.out.println("-".repeat(80));
            
            // Table rows
            for (ExecPolicyService.PolicyScope scope : result.getScopes()) {
                System.out.printf("%-12s %-24s %-24s %-16s%n", 
                    scope.getScopeLabel(), 
                    "host=" + scope.getHostRequested(),
                    "security=" + scope.getSecurityHost(),
                    "security=" + scope.getSecurityEffective());
            }
            
            System.out.println();
            System.out.println(dim(result.getNote()));
        }
        
        private String bold(String text) {
            return "\033[1m" + text + "\033[0m";
        }
        
        private String dim(String text) {
            return "\033[2m" + text + "\033[0m";
        }
    }
    
    /**
     * exec-policy set 子命令
     */
    @CommandLine.Command(
        name = "set",
        description = "Synchronize local config and host approvals using explicit values"
    )
    public static class SetCommand implements Callable<Integer> {
        
        @CommandLine.Option(
            names = {"--host"},
            description = "Exec host target: auto|sandbox|gateway|node"
        )
        private String host;
        
        @CommandLine.Option(
            names = {"--security"},
            description = "Exec security: deny|allowlist|full"
        )
        private String security;
        
        @CommandLine.Option(
            names = {"--ask"},
            description = "Exec ask mode: off|on-miss|always"
        )
        private String ask;
        
        @CommandLine.Option(
            names = {"--ask-fallback"},
            description = "Host approvals fallback: deny|allowlist|full"
        )
        private String askFallback;
        
        @CommandLine.Option(
            names = {"--json"},
            description = "Output as JSON"
        )
        private boolean json = false;
        
        @CommandLine.ParentCommand
        private ExecPolicyCommand parent;
        
        @Override
        public Integer call() {
            try {
                // Validate at least one option is provided
                if (host == null && security == null && ask == null && askFallback == null) {
                    System.err.println("Error: Provide at least one of --host, --security, --ask, or --ask-fallback");
                    return 1;
                }
                
                ExecPolicyService service = new ExecPolicyService();
                ExecPolicyService.PolicyApplyResult result = service.applyPolicy(
                    host, security, ask, askFallback
                );
                
                if (json) {
                    System.out.println(result.toJson());
                } else {
                    System.out.println("Synchronized local exec policy.");
                    System.out.println();
                    printPolicyShow(result.getShowResult());
                }
                
                return 0;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
        
        private void printPolicyShow(ExecPolicyService.PolicyShowResult result) {
            System.out.println("\n" + bold("Exec Policy"));
            System.out.println();
            System.out.printf("%-14s %s%n", "Config:", result.getConfigPath());
            System.out.printf("%-14s %s%n", "Approvals:", result.getApprovalsPath());
            System.out.printf("%-14s %s%n", "Approvals File:", result.isApprovalsExists() ? "present" : "missing");
            System.out.println();
            System.out.println(bold("Effective Policy"));
            System.out.println();
            
            // Table header
            System.out.printf("%-12s %-24s %-24s %-16s%n", "Scope", "Requested", "Host", "Effective");
            System.out.println("-".repeat(80));
            
            // Table rows
            for (ExecPolicyService.PolicyScope scope : result.getScopes()) {
                System.out.printf("%-12s %-24s %-24s %-16s%n", 
                    scope.getScopeLabel(), 
                    "host=" + scope.getHostRequested(),
                    "security=" + scope.getSecurityHost(),
                    "security=" + scope.getSecurityEffective());
            }
            
            System.out.println();
            System.out.println(dim(result.getNote()));
        }
        
        private String bold(String text) {
            return "\033[1m" + text + "\033[0m";
        }
        
        private String dim(String text) {
            return "\033[2m" + text + "\033[0m";
        }
    }
}
