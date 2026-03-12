# OpenClaw Java 完整对话流程详解

## 📋 概述

本文档详细描述从用户发送消息到收到回复的完整流程，包括所有涉及的组件和数据流转。

---

## 🎯 场景设定

**场景**: 用户通过 Telegram 向 Agent 发送消息，Agent 使用 OpenAI GPT-4 回复

**参与者**:
- 用户 (Telegram)
- OpenClaw Java Server
- OpenAI API

---

## 🔄 完整流程 (时序图)

```
时间 →

用户          Telegram          OpenClaw              Agent              OpenAI
 │               │                  │                   │                  │
 │  1.发送消息   │                  │                   │                  │
 │─────────────>│                  │                   │                  │
 │               │                  │                   │                  │
 │               │  2.Webhook      │                   │                  │
 │               │─────────────────>│                  │                  │
 │               │                  │                   │                  │
 │               │                  │  3.解析消息       │                  │
 │               │                  │─────────────────>│                  │
 │               │                  │                   │                  │
 │               │                  │                   │  4.构建Prompt   │
 │               │                  │                   │─────────────────>│
 │               │                  │                   │                  │
 │               │                  │                   │  5.调用LLM      │
 │               │                  │                   │─────────────────>│
 │               │                  │                   │                  │
 │               │                  │                   │  6.LLM响应      │
 │               │                  │                   │<─────────────────│
 │               │                  │                   │                  │
 │               │                  │                   │  7.解析响应     │
 │               │                  │                   │─────────────────>│
 │               │                  │                   │                  │
 │               │                  │                   │  8.保存上下文   │
 │               │                  │                   │─────────────────>│
 │               │                  │                   │                  │
 │               │                  │  9.构建回复       │                  │
 │               │                  │<─────────────────│                  │
 │               │                  │                   │                  │
 │               │  10.发送回复     │                   │                  │
 │               │<─────────────────│                   │                  │
 │               │                  │                   │                  │
 │  11.收到回复  │                  │                   │                  │
 │<─────────────│                  │                   │                  │
 │               │                  │                   │                  │
```

---

## 🔍 详细步骤

### 步骤 1: 用户发送消息

**用户操作**:
```
在 Telegram 中发送: "你好，请介绍一下自己"
```

**数据**:
```json
{
  "message_id": "12345",
  "from": {
    "id": "user_123",
    "first_name": "张三"
  },
  "chat": {
    "id": "chat_456",
    "type": "private"
  },
  "text": "你好，请介绍一下自己",
  "date": 1709635200
}
```

---

### 步骤 2: Telegram 发送 Webhook

**Telegram Bot API**:
```
POST https://your-server.com/webhook/telegram
Content-Type: application/json

{
  "update_id": 123456789,
  "message": {
    "message_id": 12345,
    "from": {...},
    "chat": {...},
    "date": 1709635200,
    "text": "你好，请介绍一下自己"
  }
}
```

**OpenClaw 接收**:
```java
// TelegramWebhookController
@PostMapping("/webhook/telegram")
public Mono<WebhookResponse> receiveWebhook(@RequestBody String payload) {
    return webhookController.processWebhook(payload);
}
```

---

### 步骤 3: OpenClaw 解析消息

**代码流程**:
```java
// TelegramWebhookController.processWebhook()
public Mono<WebhookResponse> processWebhook(String payload) {
    // 3.1 解析 JSON
    TelegramMessageInfo message = parseMessage(payload);
    
    // 3.2 转换为 ChannelMessage
    ChannelMessage channelMessage = ChannelMessage.builder()
        .text(message.text())
        .from(message.userId())
        .fromName(message.firstName())
        .chatId(message.chatId())
        .messageId(String.valueOf(message.messageId()))
        .timestamp(System.currentTimeMillis())
        .metadata(Map.of(
            "chatType", message.chatType(),
            "account", account
        ))
        .build();
    
    // 3.3 传递给 InboundAdapter
    return inboundAdapter.onMessage(channelMessage);
}
```

**数据转换**:
```
Telegram JSON → TelegramMessageInfo → ChannelMessage
```

---

### 步骤 4: Agent 构建 Prompt

**代码流程**:
```java
// AcpProtocolImpl.sendMessage()
public CompletableFuture<Void> sendMessage(String sessionKey, AgentMessage message) {
    return CompletableFuture.runAsync(() -> {
        // 4.1 获取会话
        AgentSession session = sessions.get(sessionKey);
        
        // 4.2 添加用户消息
        session.addMessage(message);
        
        // 4.3 构建 Prompt
        StringBuilder prompt = new StringBuilder();
        
        // 系统提示
        if (session.getSystemPrompt() != null) {
            prompt.append("System: ").append(session.getSystemPrompt()).append("\n\n");
        }
        
        // 历史消息
        for (AgentMessage msg : session.getMessages()) {
            prompt.append(msg.role()).append(": ").append(msg.content()).append("\n");
        }
        
        // 当前消息
        prompt.append("assistant: ");
        
        // 4.4 调用 LLM
        callLLM(session, prompt.toString());
    });
}
```

**构建的 Prompt**:
```
System: 你是一个有帮助的助手。

user: 你好
assistant: 你好！很高兴为你服务。
user: 请介绍一下自己
assistant: 
```

---

### 步骤 5: 调用 OpenAI LLM

**代码流程**:
```java
// LlmService.chat()
public CompletableFuture<String> chat(String prompt) {
    return CompletableFuture.supplyAsync(() -> {
        // 5.1 构建请求
        ChatRequest request = ChatRequest.builder()
            .model("gpt-4")
            .messages(List.of(
                Message.builder()
                    .role("user")
                    .content(prompt)
                    .build()
            ))
            .temperature(0.7)
            .build();
        
        // 5.2 发送 HTTP 请求
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                objectMapper.writeValueAsString(request)
            ))
            .build();
        
        // 5.3 接收响应
        HttpResponse<String> response = httpClient.send(
            httpRequest, 
            HttpResponse.BodyHandlers.ofString()
        );
        
        // 5.4 解析响应
        ChatResponse chatResponse = objectMapper.readValue(
            response.body(), 
            ChatResponse.class
        );
        
        return chatResponse.getChoices().get(0).getMessage().getContent();
    });
}
```

**HTTP 请求**:
```http
POST https://api.openai.com/v1/chat/completions
Authorization: Bearer sk-...
Content-Type: application/json

{
  "model": "gpt-4",
  "messages": [
    {"role": "user", "content": "System: 你是一个有帮助的助手。\n\nuser: 你好\nassistant: 你好！很高兴为你服务。\nuser: 请介绍一下自己\nassistant: "}
  ],
  "temperature": 0.7
}
```

---

### 步骤 6: OpenAI 返回响应

**OpenAI 响应**:
```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1709635200,
  "model": "gpt-4",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "你好！我是一个AI助手，由OpenClaw平台驱动。我可以帮助你回答问题、执行任务、提供建议等。有什么我可以帮你的吗？"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 50,
    "completion_tokens": 80,
    "total_tokens": 130
  }
}
```

---

### 步骤 7: Agent 解析响应

**代码流程**:
```java
// AcpProtocolImpl.callLLM()
private String callLLM(AgentSession session, String prompt) {
    // 7.1 调用 LLM
    String response = llmService.chat(prompt).join();
    
    // 7.2 解析响应
    // 检查是否包含工具调用
    if (response.contains("TOOL_CALL:")) {
        // 解析工具请求
        ToolCall toolCall = parseToolCall(response);
        // 执行工具
        ToolResult result = executeTool(toolCall);
        // 再次调用 LLM
        return callLLMWithResult(session, prompt, result);
    }
    
    // 7.3 返回纯文本响应
    return response;
}
```

**响应内容**:
```
"你好！我是一个AI助手，由OpenClaw平台驱动。我可以帮助你回答问题、执行任务、提供建议等。有什么我可以帮你的吗？"
```

---

### 步骤 8: 保存上下文

**代码流程**:
```java
// AgentSession.addMessage()
public void addMessage(AgentMessage message) {
    messages.add(message);
    
    // 8.1 保存到内存
    sessionCache.put(sessionKey, this);
    
    // 8.2 保存到向量存储 (可选)
    if (vectorSearchEnabled) {
        float[] embedding = embeddingService.embed(message.content()).join();
        vectorSearchService.addEmbedding(
            message.messageId(),
            embedding,
            Map.of(
                "sessionKey", sessionKey,
                "role", message.role(),
                "content", message.content()
            )
        ).join();
    }
    
    // 8.3 更新最后活动时间
    lastActivity = Instant.now();
}
```

**保存的数据**:
```json
{
  "sessionKey": "session-abc-123",
  "messages": [
    {"role": "user", "content": "你好", "timestamp": 1709635100},
    {"role": "assistant", "content": "你好！很高兴为你服务。", "timestamp": 1709635150},
    {"role": "user", "content": "请介绍一下自己", "timestamp": 1709635200},
    {"role": "assistant", "content": "你好！我是一个AI助手...", "timestamp": 1709635210}
  ],
  "lastActivity": "2024-03-11T17:00:10Z"
}
```

---

### 步骤 9: 构建回复

**代码流程**:
```java
// TelegramOutboundAdapter.send()
public CompletableFuture<SendResult> send(ChannelMessage message) {
    return CompletableFuture.supplyAsync(() -> {
        // 9.1 构建 Telegram 消息
        String chatId = message.chatId();
        String text = message.text();
        
        // 9.2 发送 HTTP 请求
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.telegram.org/bot" + token + "/sendMessage"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"chat_id\":\"" + chatId + "\",\"text\":\"" + text + "\"}"
            ))
            .build();
        
        // 9.3 发送
        HttpResponse<String> response = httpClient.send(
            request, 
            HttpResponse.BodyHandlers.ofString()
        );
        
        // 9.4 解析响应
        return parseSendResponse(response);
    });
}
```

**发送的数据**:
```json
{
  "chat_id": "chat_456",
  "text": "你好！我是一个AI助手，由OpenClaw平台驱动。我可以帮助你回答问题、执行任务、提供建议等。有什么我可以帮你的吗？"
}
```

---

### 步骤 10: Telegram 发送回复

**Telegram Bot API**:
```http
POST https://api.telegram.org/bot<token>/sendMessage
Content-Type: application/json

{
  "chat_id": "chat_456",
  "text": "你好！我是一个AI助手..."
}
```

**Telegram 响应**:
```json
{
  "ok": true,
  "result": {
    "message_id": 12346,
    "from": {...},
    "chat": {...},
    "date": 1709635210,
    "text": "你好！我是一个AI助手..."
  }
}
```

---

### 步骤 11: 用户收到回复

**用户看到**:
```
[AI助手] 你好！我是一个AI助手，由OpenClaw平台驱动。我可以帮助你回答问题、执行任务、提供建议等。有什么我可以帮你的吗？
```

---

## 📊 数据流转总结

### 完整数据流

```
用户输入 → Telegram → Webhook → ChannelMessage → AgentSession → Prompt → OpenAI → Response → ChannelMessage → Telegram → 用户
```

### 涉及组件

| 层级 | 组件 | 职责 |
|------|------|------|
| 用户层 | Telegram App | 消息发送和接收 |
| 平台层 | Telegram API | Webhook 推送 |
| 接入层 | TelegramWebhookController | Webhook 接收和解析 |
| 适配层 | TelegramInboundAdapter | 消息转换和分发 |
| 业务层 | AcpProtocolImpl | Agent 业务逻辑 |
| 服务层 | LlmService | LLM 调用 |
| 外部层 | OpenAI API | AI 推理 |
| 存储层 | AgentSession | 上下文保存 |
| 输出层 | TelegramOutboundAdapter | 消息发送 |

---

## ⏱️ 性能指标

### 各环节耗时 (估算)

| 步骤 | 操作 | 耗时 |
|------|------|------|
| 1-2 | 用户发送 → Webhook | 100-300ms |
| 3 | 解析消息 | 5-10ms |
| 4 | 构建 Prompt | 1-5ms |
| 5-6 | OpenAI API 调用 | 500-2000ms |
| 7 | 解析响应 | 5-10ms |
| 8 | 保存上下文 | 10-50ms |
| 9-10 | 发送回复 | 100-300ms |
| **总计** | **完整流程** | **700-2600ms** |

### 优化点

1. **OpenAI API 调用** - 主要耗时，可使用流式响应
2. **上下文保存** - 异步保存，不阻塞回复
3. **消息发送** - 批量发送，减少 HTTP 请求

---

## 🔒 安全考虑

### 数据安全

1. **API Key** - 存储在环境变量，不在代码中硬编码
2. **用户数据** - 内存存储，定期清理
3. **Webhook 签名** - Telegram/Feishu/Slack 验证签名

### 访问控制

1. **CORS** - 限制跨域请求
2. **Rate Limit** - 限制 API 调用频率
3. **SSRF** - 防止服务器端请求伪造

---

## 📝 日志记录

### 关键日志点

```
[2024-03-11 17:00:00] INFO  Received Telegram webhook: message_id=12345
[2024-03-11 17:00:00] DEBUG Parsed message: user=user_123, text="你好，请介绍一下自己"
[2024-03-11 17:00:00] INFO  Agent session found: session-abc-123
[2024-03-11 17:00:00] DEBUG Building prompt with 3 messages
[2024-03-11 17:00:01] INFO  Calling OpenAI API: model=gpt-4
[2024-03-11 17:00:02] INFO  OpenAI response received: tokens=130
[2024-03-11 17:00:02] DEBUG Saving context to session
[2024-03-11 17:00:02] INFO  Sending reply to Telegram: chat_id=chat_456
[2024-03-11 17:00:02] INFO  Message sent successfully: message_id=12346
```

---

## 🎯 总结

一次完整的对话流程涉及:

1. **11 个主要步骤**
2. **9 个核心组件**
3. **3 个外部服务** (Telegram, OpenAI)
4. **2-3 秒** 总耗时
5. **完整的数据流转和状态管理**

整个流程设计遵循:
- 异步非阻塞
- 松耦合架构
- 可观测性 (日志、指标)
- 容错处理 (重试、熔断)

---

*文档生成时间: 2024-03-11*  
*版本: 2026.3.9*
