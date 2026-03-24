# OpenClaw Desktop

现代化的 JavaFX 桌面应用程序，提供一体化的 OpenClaw AI 助手体验。

## 特性

### 核心功能
- ✅ **多会话聊天管理** - 创建、切换、删除、重命名对话
- ✅ **流式消息显示** - 打字机效果的实时响应
- ✅ **工具调用可视化** - Cron、Browser、Session、WebSearch 等工具
- ✅ **模型切换** - 支持多 Provider（OpenAI、Claude、Gemini 等）
- ✅ **设置面板** - 主题、API Key、模型配置
- ✅ **搜索功能** - 会话搜索、消息搜索
- ✅ **导出功能** - Markdown、JSON 格式

### UI 特性
- 🎨 **深色/浅色主题** - 现代化配色方案
- 💫 **圆角卡片设计** - 友好的视觉体验
- ✨ **流畅动画效果** - 提升交互质感
- 📱 **响应式布局** - 自适应窗口大小
- 🔮 **毛玻璃效果** - 现代化的视觉层次
- 🌈 **渐变色装饰** - 精致的细节处理

## 架构

```
┌─────────────────────────────────────────┐
│         JavaFX UI 层 (FXML/CSS)         │
├─────────────────────────────────────────┤
│         Spring Boot 上下文              │
│    (ApplicationContext 直接注入)        │
├─────────────────────────────────────────┤
│         OpenClaw 服务层                 │
│    GatewayService / AcpProtocol         │
│    CronService / BrowserService         │
└─────────────────────────────────────────┘
```

**关键特点**：
- 纯 Java 实现，无网络开销
- 直接方法调用，内存级性能
- Spring 依赖注入完整支持
- 单应用打包，独立运行

## 构建

```bash
# 编译整个项目
cd /root/openclaw-java
mvn clean install

# 仅编译桌面模块
cd openclaw-desktop
mvn clean package
```

## 运行

```bash
# 方式1: 直接运行
java -jar target/openclaw-desktop-*.jar

# 方式2: 使用 Maven
mvn javafx:run

# 方式3: Spring Boot 方式
mvn spring-boot:run
```

## 打包

```bash
# 打包为独立应用
jpackage \
  --type app-image \
  --name OpenClawDesktop \
  --input target/libs \
  --main-jar openclaw-desktop-*.jar \
  --main-class openclaw.desktop.OpenClawApp \
  --runtime-image custom-jre
```

## 项目结构

```
openclaw-desktop/
├── src/main/java/openclaw/desktop/
│   ├── OpenClawApp.java              # 启动类
│   ├── config/
│   │   └── DesktopConfig.java        # 桌面配置
│   ├── controller/
│   │   ├── MainController.java       # 主控制器
│   │   ├── ChatController.java       # 聊天界面
│   │   ├── ToolsController.java      # 工具面板
│   │   ├── SettingsController.java   # 设置界面
│   │   └── SidebarController.java    # 侧边栏
│   ├── service/
│   │   ├── ChatService.java          # 聊天服务
│   │   └── ToolExecutorService.java  # 工具执行
│   ├── component/
│   │   ├── MessageCell.java          # 消息单元格
│   │   └── ToolCard.java             # 工具卡片
│   └── model/
│       ├── UIMessage.java            # UI消息模型
│       └── UITheme.java              # 主题配置
├── src/main/resources/
│   ├── fxml/                         # FXML 布局文件
│   ├── css/                          # 样式文件
│   └── icons/                        # 图标资源
└── pom.xml
```

## 技术栈

- **Java 21** - 现代 Java 特性
- **JavaFX 21** - 桌面 UI 框架
- **Spring Boot 3.2** - 依赖注入和服务管理
- **Ikonli** - 图标库
- **Maven** - 构建工具

## 配置

在 `application.yml` 中配置：

```yaml
openclaw:
  desktop:
    theme: DARK  # DARK, LIGHT, AUTO
    font-scale: 1.0
    animations-enabled: true
    transparency-enabled: true
    auto-save: true
    streaming-enabled: true
    default-model: gpt-4
```

## 截图

*(待添加)*

## 许可证

MIT License - 与 OpenClaw 项目一致
