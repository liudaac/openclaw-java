# Plugin SDK 边界规范

**版本**: 2026.3.18  
**目标**: 定义清晰的 API 边界，防止外部直接访问内部实现

---

## 设计原则

### 1. 最小暴露原则
- 只暴露必要的 public API
- 内部实现使用 package-private 或 private
- 通过接口和工厂模式隐藏实现细节

### 2. 向后兼容
- 所有 public API 必须保持稳定
- 内部重构不影响外部使用
- 使用 @Deprecated 标记废弃方法

### 3. 模块化边界
- 每个模块有清晰的职责边界
- 模块间通过接口通信
- 禁止直接访问其他模块的内部类

---

## 包结构规范

```
openclaw-plugin-sdk/
├── src/main/java/openclaw/plugin/sdk/
│   ├── annotation/          # 注解（public）
│   │   ├── PublicApi.java   # 标记公开 API
│   │   ├── BetaApi.java     # 标记 Beta API
│   │   └── DeprecatedApi.java # 标记废弃 API
│   │
│   ├── api/                 # 公开 API 接口（public）
│   │   ├── AgentTool.java
│   │   ├── ChannelAdapter.java
│   │   ├── WebSearchProvider.java
│   │   └── ...
│   │
│   ├── spi/                 # 服务提供者接口（public）
│   │   ├── ServiceProvider.java
│   │   └── PluginActivator.java
│   │
│   ├── internal/            # 内部实现（package-private）
│   │   ├── PluginRegistry.java
│   │   ├── ServiceLoaderHelper.java
│   │   └── ReflectionUtils.java
│   │
│   └── util/                # 工具类（public）
│       ├── ValidationUtils.java
│       ├── CacheUtils.java
│       └── ...
```

---

## API 可见性规范

### Public API（公开）

```java
// 标记为 @PublicApi
@PublicApi
public interface AgentTool {
    String getName();
    CompletableFuture<ToolResult> execute(ToolExecuteContext context);
}

// 工厂方法
@PublicApi
public final class AgentTools {
    private AgentTools() {} // 禁止实例化
    
    public static AgentTool createWebSearchTool(WebSearchProvider provider) {
        return new ProviderBasedWebSearchTool(provider);
    }
}
```

### Internal API（内部）

```java
// 包级可见，不导出
class PluginRegistry {
    void register(Plugin plugin) { ... }
}

// 内部工具类
final class ReflectionUtils {
    private ReflectionUtils() {}
    
    static <T> T createInstance(Class<T> clazz) { ... }
}
```

### SPI API（服务提供者）

```java
// 供 Provider 实现
public interface ServiceProvider {
    String getServiceName();
    void activate(PluginContext context);
    void deactivate();
}

// 插件激活器
public interface PluginActivator {
    void start(PluginContext context);
    void stop();
}
```

---

## 注解规范

### @PublicApi

```java
/**
 * 标记公开的稳定 API。
 * 这些 API 承诺向后兼容，可以在插件中安全使用。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface PublicApi {
    String since() default "";
    String stability() default "stable"; // stable, experimental
}
```

### @BetaApi

```java
/**
 * 标记 Beta API。
 * 这些 API 可能会在不通知的情况下更改。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface BetaApi {
    String reason() default "";
}
```

### @InternalApi

```java
/**
 * 标记内部 API。
 * 这些 API 不应该在插件中使用，可能会在任何版本中更改。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface InternalApi {
}
```

---

## 模块边界规范

### 1. Plugin SDK 模块

**职责**: 提供插件开发的基础接口和工具

**公开 API**:
- `AgentTool` - 工具接口
- `ChannelAdapter` - 频道适配器
- `WebSearchProvider` - 搜索 Provider
- `ValidationUtils` - 验证工具

**内部实现**:
- `PluginRegistry` - 插件注册
- `ServiceLoaderHelper` - SPI 加载辅助

**禁止**:
- 直接访问 `internal` 包下的类
- 使用反射访问私有成员

### 2. Provider 模块

**职责**: 实现具体的 Provider 功能

**必须实现**:
- `WebSearchProvider` 接口
- SPI 注册 (`META-INF/services`)

**可以使用**:
- Plugin SDK 的所有 public API
- Plugin SDK 的 util 包

**禁止**:
- 访问其他 Provider 的内部实现
- 直接访问 openclaw-core 的内部类

### 3. Core 模块

**职责**: 核心运行时和配置管理

**公开 API**:
- `OpenClawConfig` - 配置接口
- `RuntimeEnv` - 运行时环境

**内部实现**:
- `ConfigManager` - 配置管理器
- `PluginLoader` - 插件加载器

---

## API 导出规范

### module-info.java（Java 9+）

```java
module openclaw.plugin.sdk {
    // 导出的包
    exports openclaw.plugin.sdk.annotation;
    exports openclaw.plugin.sdk.api;
    exports openclaw.plugin.sdk.spi;
    exports openclaw.plugin.sdk.util;
    
    // 不导出的包（内部实现）
    // openclaw.plugin.sdk.internal - 不导出
    
    // 使用的服务
    uses openclaw.plugin.sdk.spi.ServiceProvider;
    uses openclaw.plugin.sdk.api.WebSearchProvider;
    
    // 提供的服务
    provides openclaw.plugin.sdk.api.AgentTool
        with openclaw.plugin.sdk.internal.AgentToolRegistry;
}
```

### 传统方式（Java 8）

使用包可见性和文档约定：

```java
/**
 * @apiNote This is an internal class, not part of public API.
 * @implNote Do not use outside of openclaw-plugin-sdk.
 */
class InternalClass {
    // package-private
}
```

---

## 版本兼容性

### 语义化版本

- **Major**: 不兼容的 API 更改
- **Minor**: 向后兼容的功能添加
- **Patch**: 向后兼容的问题修复

### API 废弃策略

```java
@PublicApi(since = "2026.3.0")
@Deprecated(since = "2026.6.0", forRemoval = true)
@DeprecatedApi(replacement = "NewApi.class", removalVersion = "2026.9.0")
public interface OldApi {
    // ...
}
```

---

## 实现步骤

### 1. 创建注解包

```
openclaw.plugin.sdk.annotation/
├── PublicApi.java
├── BetaApi.java
├── InternalApi.java
└── DeprecatedApi.java
```

### 2. 重构现有代码

- 将现有类移动到合适的包
- 添加可见性注解
- 更新文档

### 3. 创建 module-info.java（可选）

对于 Java 9+ 项目，添加模块描述符。

### 4. 添加边界检查工具

- 编译时检查（注解处理器）
- 运行时检查（反射扫描）

---

## 检查清单

- [ ] 创建 annotation 包
- [ ] 标记所有 public API
- [ ] 将内部类移动到 internal 包
- [ ] 更新文档
- [ ] 添加 module-info.java（可选）
- [ ] 创建边界检查工具
- [ ] 更新开发者文档

---

## 示例代码

### 标记 Public API

```java
package openclaw.plugin.sdk.api;

import openclaw.plugin.sdk.annotation.PublicApi;

@PublicApi(since = "2026.3.0", stability = "stable")
public interface WebSearchProvider {
    String getId();
    WebSearchToolDefinition createTool(WebSearchContext context);
}
```

### 标记 Internal API

```java
package openclaw.plugin.sdk.internal;

import openclaw.plugin.sdk.annotation.InternalApi;

@InternalApi
class PluginRegistry {
    void register(Plugin plugin) { ... }
}
```

### 使用边界检查

```java
// 编译时检查
@PublicApi
public class MyPlugin {
    public void init() {
        // 允许：使用 public API
        WebSearchProvider provider = ...;
        
        // 禁止：使用 internal API
        // PluginRegistry registry = new PluginRegistry(); // 编译错误
    }
}
```

---

**实施建议**: 先创建注解包，然后逐步标记现有 API，最后添加检查工具。
