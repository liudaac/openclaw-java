# OpenClaw Browser Module

基于 Playwright Java API 的浏览器自动化模块。

## 特性

- ✅ **Playwright 原生集成** - 使用官方 Java API，非 CLI 调用
- ✅ **会话管理** - 多会话、多页面支持
- ✅ **完整操作** - 导航、点击、输入、滚动、截图等
- ✅ **页面快照** - 捕获页面结构和元素信息
- ✅ **JavaScript 执行** - 在页面上下文中执行脚本
- ✅ **异步 API** - 所有操作支持 CompletableFuture

## 架构

```
openclaw-browser/
├── BrowserService.java      # 主服务
├── model/
│   └── BrowserSession.java  # 会话模型
├── session/
│   └── SessionManager.java  # 会话管理
├── action/
│   └── BrowserActions.java  # 操作封装
└── snapshot/
    └── PageSnapshot.java    # 页面快照
```

## 依赖

```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.40.0</version>
</dependency>
```

## 使用示例

```java
@Autowired
private BrowserService browserService;

// 创建会话
BrowserSession session = browserService.createSession(
    "default",
    SessionOptions.defaults()
).join();

// 创建页面
Page page = browserService.createPage(session.getId()).join();

// 导航
browserService.navigate(session.getId(), "https://example.com").join();

// 截图
byte[] screenshot = browserService.screenshot(session.getId()).join();

// 获取快照
PageSnapshot snapshot = browserService.getSnapshot(session.getId()).join();

// 点击元素
browserService.click(session.getId(), "#submit-button").join();

// 输入文本
browserService.type(session.getId(), "#search-input", "OpenClaw").join();

// 执行 JavaScript
Object result = browserService.evaluate(session.getId(), "document.title").join();

// 关闭会话
browserService.closeSession(session.getId()).join();
```

## 与原版对比

| 功能 | Node.js | Java (新) |
|------|---------|-----------|
| Playwright | 原生集成 | 原生 Java API ✅ |
| 会话管理 | 完整 | 完整 ✅ |
| 多页面 | 支持 | 支持 ✅ |
| 快照 | 完整 | 完整 ✅ |
| Chrome 扩展 | 支持 | 待实现 |
| 网络拦截 | 支持 | 待实现 |

## 待实现

- [ ] Chrome 扩展中继
- [ ] 网络拦截
- [ ] 移动端模拟
- [ ] 视频录制
