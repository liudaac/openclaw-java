# Web Search Provider 架构迁移 - 实施进度

**实施日期**: 2026-03-18  
**状态**: Phase 1 完成，代码已编写待编译验证

---

## 已完成工作

### 1. Plugin SDK 模块 (openclaw-plugin-sdk)

#### 新增接口和类

| 文件 | 说明 | 状态 |
|------|------|------|
| `WebSearchProvider.java` | Provider 接口定义 | ✅ 完成 |
| `WebSearchToolDefinition.java` | 工具定义接口 | ✅ 完成 |
| `WebSearchContext.java` | 搜索上下文 | ✅ 完成 |
| `WebSearchRuntimeMetadataContext.java` | 运行时元数据上下文 | ✅ 完成 |
| `WebSearchProviderRegistry.java` | Provider 注册中心 | ✅ 完成 |

#### 新增工具类

| 文件 | 说明 | 状态 |
|------|------|------|
| `utils/ValidationUtils.java` | 参数验证工具 | ✅ 完成 |
| `utils/CacheUtils.java` | 缓存管理工具 | ✅ 完成 |
| `utils/CredentialUtils.java` | 凭证管理工具 | ✅ 完成 |
| `utils/HttpUtils.java` | HTTP 请求工具 | ✅ 完成 |

### 2. Brave Provider 模块 (openclaw-provider-brave)

#### 新增文件

| 文件 | 说明 | 状态 |
|------|------|------|
| `BraveWebSearchProvider.java` | Brave Provider 实现 | ✅ 完成 |
| `pom.xml` | Maven 配置 | ✅ 完成 |
| `META-INF/services/...WebSearchProvider` | SPI 注册 | ✅ 完成 |

### 3. 根项目配置更新

| 文件 | 更新内容 | 状态 |
|------|----------|------|
| `pom.xml` | 添加 openclaw-provider-brave 模块 | ✅ 完成 |
| `pom.xml` | 添加 dependencyManagement | ✅ 完成 |

---

## 文件结构

```
openclaw-java/
├── openclaw-plugin-sdk/
│   └── src/main/java/openclaw/plugin/sdk/websearch/
│       ├── WebSearchProvider.java              (接口)
│       ├── WebSearchToolDefinition.java        (接口)
│       ├── WebSearchContext.java               (类)
│       ├── WebSearchRuntimeMetadataContext.java (类)
│       ├── WebSearchProviderRegistry.java      (类)
│       └── utils/
│           ├── ValidationUtils.java            (工具类)
│           ├── CacheUtils.java                 (工具类)
│           ├── CredentialUtils.java            (工具类)
│           └── HttpUtils.java                  (工具类)
│
├── openclaw-provider-brave/
│   ├── pom.xml
│   └── src/main/
│       ├── java/openclaw/provider/brave/
│       │   └── BraveWebSearchProvider.java     (Provider实现)
│       └── resources/META-INF/services/
│           └── openclaw.plugin.sdk.websearch.WebSearchProvider
│
└── pom.xml                                     (已更新)
```

---

## 关键特性实现

### 1. Provider 接口
- ✅ Provider ID、Label、Hint 等元数据
- ✅ 环境变量支持 (envVars)
- ✅ 凭证管理 (get/set CredentialValue)
- ✅ 工具创建 (createTool)
- ✅ 自动检测优先级 (autoDetectOrder)

### 2. 工具定义
- ✅ JSON Schema 参数定义
- ✅ 异步执行 (CompletableFuture)
- ✅ Provider ID 和 Mode 标识

### 3. 工具类
- ✅ ValidationUtils: 参数验证、日期处理、语言代码验证
- ✅ CacheUtils: 内存缓存 + 磁盘缓存、TTL 管理
- ✅ CredentialUtils: 环境变量读取、配置解析
- ✅ HttpUtils: HTTP 客户端、请求构建、错误处理

### 4. Brave Provider
- ✅ Web Search API 支持
- ✅ 参数验证和错误处理
- ✅ 缓存集成
- ✅ SPI 注册

---

## 待完成工作

### Phase 2: 完善和测试

1. **编译验证**
   - 需要 Maven 和 Java 环境
   - 运行 `mvn compile` 验证代码

2. **单元测试**
   - WebSearchProviderRegistry 测试
   - BraveWebSearchProvider 测试
   - 工具类单元测试

3. **集成到 openclaw-tools**
   - 更新 WebSearchTool 使用新的 Provider 架构
   - 配置系统集成

4. **其他 Provider 实现**
   - Perplexity Provider
   - Google Provider
   - Moonshot Provider
   - xAI Provider

---

## 使用示例

### 1. 加载 Providers

```java
WebSearchProviderRegistry registry = new WebSearchProviderRegistry();
Optional<WebSearchProvider> brave = registry.getProvider("brave");
```

### 2. 创建搜索工具

```java
WebSearchContext ctx = WebSearchContext.builder()
    .searchConfig(Map.of("apiKey", "your-api-key"))
    .build();

WebSearchToolDefinition tool = brave.get().createTool(ctx);
```

### 3. 执行搜索

```java
Map<String, Object> args = Map.of(
    "query", "OpenClaw Java",
    "count", 5
);

tool.execute(args).thenAccept(result -> {
    System.out.println(result);
});
```

---

## 下一步建议

1. **在本地开发环境编译验证**
   ```bash
   cd /root/openclaw-java
   mvn clean compile
   ```

2. **运行测试**
   ```bash
   mvn test -pl openclaw-plugin-sdk,openclaw-provider-brave
   ```

3. **集成到主项目**
   - 更新 openclaw-tools 的 WebSearchTool
   - 添加配置属性类
   - 创建 Spring Boot Auto-Configuration

4. **继续其他 Provider**
   - 参考 Brave Provider 实现其他 Provider
   - 每个 Provider 是一个独立模块

---

## 与原版的对比

| 特性 | TypeScript (原版) | Java (当前) |
|------|-------------------|-------------|
| Provider 接口 | `WebSearchProviderPlugin` | `WebSearchProvider` ✅ |
| 工具定义 | `WebSearchProviderToolDefinition` | `WebSearchToolDefinition` ✅ |
| Provider 注册 | 插件系统 | SPI (ServiceLoader) ✅ |
| 缓存 | 内置 | CacheUtils ✅ |
| HTTP 请求 | fetch | HttpClient (Java 11+) ✅ |
| 凭证管理 | 配置系统 | CredentialUtils ✅ |
| Brave Provider | 完整实现 | 简化实现 ✅ |

---

## 注意事项

1. **Java 版本**: 代码使用 Java 17 特性 (var, switch expressions 等)
2. **依赖**: 需要 Jackson 进行 JSON 处理
3. **SPI 机制**: Provider 通过 `META-INF/services` 注册
4. **异步**: 使用 `CompletableFuture` 实现异步操作

---

*最后更新: 2026-03-18 21:40*
