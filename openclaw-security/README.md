# OpenClaw Security (Java)

Security policies and guards for OpenClaw Java Edition, compatible with Node.js version 2026.3.9.

## Features

- **SSRF Protection**: URL validation against private IP ranges
- **Security Config Validation**: Dangerous flag detection
- **Input Validation**: SQL injection, XSS, path traversal protection
- **Fetch Guard**: HTTP request protection

## Architecture

```
openclaw-security/
├── ssrf/           # SSRF protection
├── config/         # Security config validation
└── guard/          # Input validation guards
```

## SSRF Protection

### Basic Usage

```java
SsrfPolicy policy = new DefaultSsrfPolicy();

// Validate URL
SsrfValidationResult result = policy.validate("https://api.example.com/data");
if (!result.allowed()) {
    System.out.println("Blocked: " + result.reason());
}

// With custom allowlist/blocklist
Set<String> allowlist = Set.of("api.example.com");
Set<String> blocklist = Set.of("evil.com");
SsrfPolicy policy = new DefaultSsrfPolicy(allowlist, blocklist);
```

### Fetch Guard

```java
FetchGuard guard = new FetchGuard();

// Validate before fetch
guard.validateOrThrow("https://safe-api.com/data");

// Guarded fetch
CompletableFuture<String> result = guard.guardedFetch(
    "https://api.example.com/data",
    url -> {
        // Perform actual HTTP request
        return httpClient.get(url);
    }
);
```

### Blocked Resources

- Private IP ranges (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, etc.)
- Localhost and loopback
- Sensitive ports (22, 23, 25, 3306, 5432, etc.)
- Dangerous schemes (file, ftp, gopher, etc.)

## Security Config Validation

```java
SecurityConfigValidator validator = new SecurityConfigValidator();

Map<String, Object> config = Map.of(
    "security.password.minLength", 8,
    "security.session.timeoutMinutes", 60,
    "security.ssrf.disabled", true  // Dangerous!
);

ValidationResult result = validator.validate(config);

if (!result.valid()) {
    System.out.println("Errors: " + result.errors());
}
if (result.hasIssues()) {
    System.out.println("Warnings: " + result.warnings());
}
```

### Dangerous Flags Detected

- `security.ssrf.disabled`
- `security.cors.allowAll`
- `security.csrf.disabled`
- `security.xss.disabled`
- `security.tls.verifyDisabled`
- `security.auth.disabled`
- `security.audit.disabled`
- `security.rateLimit.disabled`
- `security.inputValidation.disabled`
- `security.fileUpload.unrestricted`
- `security.exec.unrestricted`
- `security.network.unrestricted`

## Input Validation

```java
InputValidator validator = new InputValidator();

// General validation
ValidationResult result = validator.validate(userInput);

// SQL injection check
ValidationResult sqlResult = validator.validateNoSqlInjection(userInput);

// XSS check
ValidationResult xssResult = validator.validateNoXss(userInput);

// Path traversal check
ValidationResult pathResult = validator.validateNoPathTraversal(userInput);

// Sanitize for display
String safe = validator.sanitize(userInput);
```

### Protection Against

- SQL injection patterns
- XSS vectors (script tags, event handlers, etc.)
- Path traversal (../, ..\\, etc.)
- Dangerous characters

## Statistics

- **Total Java files**: 5
- **SSRF checks**: Private IPs, hostnames, ports, schemes
- **Config checks**: 12 dangerous flags
- **Input checks**: SQLi, XSS, path traversal

## Next Steps

1. Add rate limiting guard
2. Add CORS policy validation
3. Add TLS/SSL configuration validation
4. Add authentication guards
