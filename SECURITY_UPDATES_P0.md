# P0 安全更新实施文档

基于原版 OpenClaw 2026.3.11-2026.3.13 安全更新

---

## 🔴 关键安全更新清单

### 1. Exec 审批安全加固

#### 1.1 Ruby 脚本执行安全
**原版更新**: `GHSA-57jw-9722-6rf2`
- 修复 Ruby `-r`, `--require`, `-I` 审批流程
- 防止预加载和加载路径模块解析绕过审批

**Java版实施**:
```java
// 新增: openclaw-security/src/main/java/openclaw/security/exec/RubyExecValidator.java
public class RubyExecValidator {
    // 检测 Ruby 危险参数
    public boolean hasDangerousFlags(String command) {
        // 检测 -r, --require, -I 参数
        return command.matches(".*\\s-(r|I)\\s+.*") || 
               command.matches(".*\\s--require\\s+.*");
    }
    
    // 提取实际执行的脚本
    public String extractScript(String command) {
        // 移除 -r, --require, -I 参数后解析实际脚本
    }
}
```

#### 1.2 Perl 脚本执行安全
**原版更新**: `GHSA-jvqh-rfmh-jh27`
- 修复 Perl `-M` 和 `-I` 审批流程

**Java版实施**:
```java
// 新增: openclaw-security/src/main/java/openclaw/security/exec/PerlExecValidator.java
public class PerlExecValidator {
    public boolean hasDangerousFlags(String command) {
        return command.matches(".*\\s-M\\w+.*") ||
               command.matches(".*\\s-I\\s+\\S+.*");
    }
}
```

#### 1.3 PowerShell 脚本执行安全
**原版更新**: `GHSA-x7pp-23xv-mmr4`
- 修复 PowerShell `-File` 和 `-f` 包装器

**Java版实施**:
```java
// 新增: openclaw-security/src/main/java/openclaw/security/exec/PowerShellValidator.java
public class PowerShellValidator {
    public boolean isFileExecution(String command) {
        return command.matches(".*\\s-[Ff](ile)?\\s+.*");
    }
    
    public String extractScriptPath(String command) {
        // 提取 -File 参数后的脚本路径
    }
}
```

#### 1.4 env 包装器安全
**原版更新**: `GHSA-jc5j-vg4r-j5jx`
- 修复 `env` 调度包装器 (macOS)

**Java版实施**:
```java
// 新增: openclaw-security/src/main/java/openclaw/security/exec/EnvWrapperValidator.java
public class EnvWrapperValidator {
    public boolean isEnvWrapper(String command) {
        return command.trim().startsWith("env ");
    }
    
    public String unwrapCommand(String command) {
        // 移除 env 环境变量设置，提取实际命令
        // 例如: "env FOO=bar /path/to/bin" -> "/path/to/bin"
    }
}
```

#### 1.5 Shell 行继续符安全
**原版更新**:
- 修复反斜杠换行符作为 shell 行继续符
- 防止行继续的 `$(` 替换绕过检查

**Java版实施**:
```java
// 增强: openclaw-security/src/main/java/openclaw/security/exec/ShellCommandParser.java
public class ShellCommandParser {
    public String normalizeLineContinuations(String command) {
        // 处理反斜杠换行符: "cmd \\\n        // arg" -> "cmd arg"
        return command.replaceAll("\\s*\\\\\\s*\\n\\s*", " ");
    }
    
    public boolean hasCommandSubstitution(String command) {
        // 检测 $(...) 和 `...` 命令替换
        return command.matches(".*\\$\\s*\\(.*") ||
               command.matches(".*`[^`]+`.*");
    }
}
```

#### 1.6 macOS 技能自动允许信任
**原版更新**:
- 绑定技能自动允许信任到可执行文件名和解析路径
- 防止同名二进制文件继承信任

**Java版实施**:
```java
// 增强: openclaw-security/src/main/java/openclaw/security/exec/SkillTrustValidator.java
public class SkillTrustValidator {
    public boolean validateSkillTrust(String executableName, String resolvedPath) {
        // 同时验证可执行文件名和解析后的完整路径
        String trustedName = getTrustedSkillName(executableName);
        String trustedPath = getTrustedSkillPath(executableName);
        
        return executableName.equals(trustedName) &&
               resolvedPath.equals(trustedPath);
    }
}
```

---

### 2. Webhook 安全加固

#### 2.1 Feishu Webhook 安全
**原版更新**: `GHSA-g353-mgv3-8pcj`
- 需要 `encryptKey` 和 `verificationToken`

**Java版实施**:
```java
// 修改: openclaw-channel-feishu/src/main/java/openclaw/channel/feishu/webhook/FeishuWebhookController.java
@RestController
public class FeishuWebhookController {
    
    @PostMapping("/webhook/feishu")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("X-Lark-Signature") String signature,
            @RequestHeader("X-Lark-Request-Timestamp") String timestamp,
            @RequestBody String body) {
        
        // 1. 验证时间戳 (5分钟内)
        if (!validateTimestamp(timestamp)) {
            return ResponseEntity.status(401).build();
        }
        
        // 2. 验证签名 (使用 encryptKey)
        String expectedSignature = calculateSignature(body, timestamp, encryptKey);
        if (!constantTimeEquals(signature, expectedSignature)) {
            return ResponseEntity.status(401).build();
        }
        
        // 3. 验证 verificationToken
        JsonNode json = parseBody(body);
        if (!verificationToken.equals(json.get("token").asText())) {
            return ResponseEntity.status(401).build();
        }
        
        // 处理 webhook
    }
    
    private boolean constantTimeEquals(String a, String b) {
        // 恒定时间比较，防止时序攻击
        return MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }
}
```

#### 2.2 LINE Webhook 安全
**原版更新**: `GHSA-mhxh-9pjm-w7q5`
- 空事件 POST 探测也需要签名

**Java版实施**:
```java
// 修改: openclaw-channel-line/src/main/java/openclaw/channel/line/webhook/LineWebhookController.java
@PostMapping("/webhook/line")
public ResponseEntity<Void> handleWebhook(
        @RequestHeader("X-Line-Signature") String signature,
        @RequestBody String body) {
    
    // 即使 body 为空，也必须验证签名
    if (body == null || body.isEmpty()) {
        // 空事件也需要签名验证
        if (!validateSignature("", signature)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok().build();
    }
    
    // 正常验证
}
```

#### 2.3 Zalo Webhook 安全
**原版更新**: `GHSA-5m9r-p9g7-679c`
- 速率限制无效密钥猜测

**Java版实施**:
```java
// 修改: openclaw-channel-zalo/src/main/java/openclaw/channel/zalo/webhook/ZaloWebhookController.java
@Component
public class ZaloWebhookController {
    
    private final RateLimiter rateLimiter = new RateLimiter(5, Duration.ofMinutes(1));
    
    @PostMapping("/webhook/zalo")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("X-Zalo-Signature") String signature,
            @RequestBody String body,
            HttpServletRequest request) {
        
        String clientIp = getClientIp(request);
        
        // 1. 速率限制检查 (在验证之前)
        if (!rateLimiter.tryAcquire(clientIp)) {
            return ResponseEntity.status(429).build(); // Too Many Requests
        }
        
        // 2. 验证签名
        if (!validateSignature(body, signature)) {
            // 验证失败，但已计入速率限制
            return ResponseEntity.status(401).build();
        }
        
        // 处理 webhook
    }
}
```

#### 2.4 Telegram Webhook 安全
**原版更新**:
- 验证密钥在读取请求体之前

**Java版实施**:
```java
// 修改: openclaw-channel-telegram/src/main/java/openclaw/channel/telegram/webhook/TelegramWebhookController.java
@PostMapping("/webhook/telegram/{secret}")
public ResponseEntity<Void> handleWebhook(
        @PathVariable String secret,
        @RequestBody String body) {
    
    // 1. 首先验证 secret (在解析 body 之前)
    if (!webhookSecret.equals(secret)) {
        return ResponseEntity.status(401).build();
    }
    
    // 2. 限制 body 大小 (防止大请求攻击)
    if (body.length() > MAX_WEBHOOK_BODY_SIZE) {
        return ResponseEntity.status(413).build(); // Payload Too Large
    }
    
    // 3. 解析和处理 body
    Update update = parseUpdate(body);
}
```

---

### 3. 输入验证强化

#### 3.1 零宽字符检测
**原版更新**: `GHSA-pcqg-f7rg-xfvv`
- 剥离零宽和软连字符标记分割字符

**Java版实施**:
```java
// 增强: openclaw-security/src/main/java/openclaw/security/guard/InputValidator.java
public class InputValidator {
    
    // 零宽字符集合
    private static final String ZERO_WIDTH_CHARS = "\u200B\u200C\u200D\uFEFF\u2060\u2061\u2062\u2063";
    
    public String stripZeroWidthChars(String input) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (ZERO_WIDTH_CHARS.indexOf(c) < 0) {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    public String stripSoftHyphens(String input) {
        // 软连字符 U+00AD
        return input.replace("\u00AD", "");
    }
    
    public String sanitizeBoundaryMarkers(String input) {
        // 防止伪造的 EXTERNAL_UNTRUSTED_CONTENT 标记
        String sanitized = stripZeroWidthChars(input);
        sanitized = stripSoftHyphens(sanitized);
        // 规范化 Unicode
        return Normalizer.normalize(sanitized, Normalizer.Form.NFC);
    }
}

---

### 4. 设备配对安全

#### 4.1 短期引导令牌
**原版更新**: `GHSA-2pwv-x786-56f8`
- 设置码改为短期引导令牌 (5分钟过期)
- 单次使用验证

**Java版实施**:
```java
// 修改: openclaw-gateway/src/main/java/openclaw/gateway/auth/DevicePairingService.java
@Service
public class DevicePairingService {
    
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);
    private final Set<String> usedTokens = ConcurrentHashMap.newKeySet();
    
    public String generateSetupCode() {
        String code = generateSecureCode();
        String token = generateBootstrapToken(code);
        storeToken(token, TOKEN_TTL);
        return code;
    }
    
    public boolean verifySetupCode(String code) {
        String token = generateBootstrapToken(code);
        
        // 1. 检查是否已使用
        if (usedTokens.contains(token)) {
            return false;
        }
        
        // 2. 验证令牌有效性
        if (!isTokenValid(token)) {
            return false;
        }
        
        // 3. 标记为已使用
        usedTokens.add(token);
        return true;
    }
}
```

#### 4.2 设备令牌范围限制
**原版更新**:
- 设备令牌范围限制在批准的基线
- 防止过期或过宽的令牌超出批准访问

**Java版实施**:
```java
// 修改: openclaw-gateway/src/main/java/openclaw/gateway/auth/DeviceTokenService.java
@Service
public class DeviceTokenService {
    
    public DeviceToken issueToken(Device device, Set<Scope> approvedScopes) {
        // 限制令牌范围为设备批准的基线
        Set<Scope> tokenScopes = intersectScopes(
            device.getRequestedScopes(),
            approvedScopes
        );
        
        return DeviceToken.builder()
            .deviceId(device.getId())
            .scopes(tokenScopes)
            .expiresAt(Instant.now().plus(TOKEN_TTL))
            .build();
    }
    
    public boolean verifyToken(String token, Set<Scope> requiredScopes) {
        DeviceToken deviceToken = parseToken(token);
        
        // 验证令牌范围是否包含所需范围
        if (!deviceToken.getScopes().containsAll(requiredScopes)) {
            return false;
        }
        
        // 验证是否过期
        return deviceToken.getExpiresAt().isAfter(Instant.now());
    }
}
```

---

## 📋 实施检查清单

### Phase 1: Exec 审批安全
- [ ] RubyExecValidator.java
- [ ] PerlExecValidator.java
- [ ] PowerShellValidator.java
- [ ] EnvWrapperValidator.java
- [ ] ShellCommandParser.java (增强)
- [ ] SkillTrustValidator.java (增强)

### Phase 2: Webhook 安全
- [ ] FeishuWebhookController.java (修改)
- [ ] LineWebhookController.java (修改)
- [ ] ZaloWebhookController.java (修改)
- [ ] TelegramWebhookController.java (修改)
- [ ] RateLimiter.java (新增)

### Phase 3: 输入验证
- [ ] InputValidator.java (增强)
- [ ] UnicodeNormalizer.java (新增)

### Phase 4: 设备配对
- [ ] DevicePairingService.java (修改)
- [ ] DeviceTokenService.java (修改)

---

## 🔗 参考

- 原版 CHANGELOG: `/root/openclaw/CHANGELOG.md`
- 原版安全实现: `/root/openclaw/src/security/`
- Java版安全模块: `/root/openclaw-java/openclaw-security/`

---

*文档创建时间: 2026-03-19*
*基于原版版本: 2026.3.11 - 2026.3.13*