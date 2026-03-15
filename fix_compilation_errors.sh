#!/bin/bash
# 批量修复编译错误脚本

echo "开始批量修复编译错误..."

# 1. 修复所有 JsonNode 返回类型为 ObjectNode
find /root/openclaw-java/openclaw-server -name "*.java" -exec sed -i 's/Mono<ResponseEntity<JsonNode>>/Mono<ResponseEntity<ObjectNode>>/g' {} \;
find /root/openclaw-java/openclaw-server -name "*.java" -exec sed -i 's/ResponseEntity<JsonNode>/ResponseEntity<ObjectNode>/g' {} \;

# 2. 修复 Prometheus 包路径
find /root/openclaw-java/openclaw-server -name "*.java" -exec sed -i 's/io\.micrometer\.prometheus\.PrometheusMeterRegistry/io.micrometer.prometheusmetrics.PrometheusMeterRegistry/g' {} \;
find /root/openclaw-java/openclaw-server -name "*.java" -exec sed -i 's/io\.micrometer\.prometheus\.PrometheusConfig/io.micrometer.prometheusmetrics.PrometheusConfig/g' {} \;

# 3. 修复 Spring AI ChatClient 导入
find /root/openclaw-java/openclaw-server -name "*.java" -exec sed -i 's/import org\.springframework\.ai\.chat\.ChatClient;/import org.springframework.ai.chat.client.ChatClient;/g' {} \;

# 4. 移除不存在的 ChatResponse 导入
find /root/openclaw-java/openclaw-server -name "*.java" -exec sed -i '/import org\.springframework\.ai\.chat\.ChatResponse;/d' {} \;
find /root/openclaw-java/openclaw-server -name "*.java" -exec sed -i '/import org\.springframework\.ai\.openai\.OpenAiChatClient;/d' {} \;

# 5. 修复 ToolResult.output() 为 content()
find /root/openclaw-java/openclaw-server -name "*.java" -exec sed -i 's/\.output()/.content().orElse(null)/g' {} \;

echo "批量修复完成！"
