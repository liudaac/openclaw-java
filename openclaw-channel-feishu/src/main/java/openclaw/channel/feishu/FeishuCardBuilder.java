package openclaw.channel.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Feishu Card Builder - 构建飞书交互式卡片
 * 
 * 功能:
 * - 构建各种卡片模板
 * - 支持按钮、表单、列表等组件
 * - 支持卡片回调
 * 
 * 对应 Node.js: src/channels/feishu/cards.ts
 */
public class FeishuCardBuilder {
    
    private final ObjectMapper objectMapper;
    private final ObjectNode card;
    private final ArrayNode elements;
    
    public FeishuCardBuilder() {
        this.objectMapper = new ObjectMapper();
        this.card = objectMapper.createObjectNode();
        this.card.put("config", createConfig());
        this.card.put("header", createHeader(""));
        this.elements = objectMapper.createArrayNode();
    }
    
    /**
     * 设置卡片标题
     */
    public FeishuCardBuilder withTitle(String title, String subtitle) {
        ObjectNode header = objectMapper.createObjectNode();
        header.put("title", createTextElement(title, "plain_text", true));
        if (subtitle != null) {
            header.put("subtitle", createTextElement(subtitle, "plain_text", false));
        }
        header.put("template", "blue");
        card.set("header", header);
        return this;
    }
    
    /**
     * 添加文本段落
     */
    public FeishuCardBuilder addText(String text, boolean bold) {
        ObjectNode element = objectMapper.createObjectNode();
        element.put("tag", "div");
        element.set("text", createTextElement(text, "lark_md", bold));
        elements.add(element);
        return this;
    }
    
    /**
     * 添加 Markdown 文本
     */
    public FeishuCardBuilder addMarkdown(String markdown) {
        ObjectNode element = objectMapper.createObjectNode();
        element.put("tag", "div");
        element.set("text", createMarkdownElement(markdown));
        elements.add(element);
        return this;
    }
    
    /**
     * 添加按钮
     */
    public FeishuCardBuilder addButton(String text, String value, String type) {
        ObjectNode action = objectMapper.createObjectNode();
        action.put("tag", "button");
        action.set("text", createTextElement(text, "plain_text", false));
        action.put("type", type); // primary, default, danger
        action.put("value", value);
        
        ObjectNode element = objectMapper.createObjectNode();
        element.put("tag", "action");
        ArrayNode actions = objectMapper.createArrayNode();
        actions.add(action);
        element.set("actions", actions);
        
        elements.add(element);
        return this;
    }
    
    /**
     * 添加多个按钮
     */
    public FeishuCardBuilder addButtonGroup(List<ButtonConfig> buttons) {
        ObjectNode element = objectMapper.createObjectNode();
        element.put("tag", "action");
        
        ArrayNode actions = objectMapper.createArrayNode();
        for (ButtonConfig btn : buttons) {
            ObjectNode action = objectMapper.createObjectNode();
            action.put("tag", "button");
            action.set("text", createTextElement(btn.getText(), "plain_text", false));
            action.put("type", btn.getType());
            action.put("value", btn.getValue());
            actions.add(action);
        }
        
        element.set("actions", actions);
        element.put("layout", "bisected"); // 两列布局
        
        elements.add(element);
        return this;
    }
    
    /**
     * 添加输入框
     */
    public FeishuCardBuilder addInput(String name, String label, String placeholder) {
        ObjectNode element = objectMapper.createObjectNode();
        element.put("tag", "div");
        element.put("text", createTextElement(label, "plain_text", false));
        elements.add(element);
        
        ObjectNode input = objectMapper.createObjectNode();
        input.put("tag", "input");
        input.put("element_id", name);
        input.set("placeholder", createTextElement(placeholder, "plain_text", false));
        
        elements.add(input);
        return this;
    }
    
    /**
     * 添加选择器
     */
    public FeishuCardBuilder addSelect(String name, String label, List<Option> options) {
        ObjectNode element = objectMapper.createObjectNode();
        element.put("tag", "div");
        element.set("text", createTextElement(label, "plain_text", false));
        elements.add(element);
        
        ObjectNode select = objectMapper.createObjectNode();
        select.put("tag", "select_static");
        select.put("element_id", name);
        
        ArrayNode optionArray = objectMapper.createArrayNode();
        for (Option opt : options) {
            ObjectNode option = objectMapper.createObjectNode();
            option.set("text", createTextElement(opt.getLabel(), "plain_text", false));
            option.put("value", opt.getValue());
            optionArray.add(option);
        }
        select.set("options", optionArray);
        
        elements.add(select);
        return this;
    }
    
    /**
     * 添加列表
     */
    public FeishuCardBuilder addList(List<ListItem> items) {
        for (ListItem item : items) {
            ObjectNode element = objectMapper.createObjectNode();
            element.put("tag", "div");
            
            ObjectNode text = objectMapper.createObjectNode();
            text.put("tag", "lark_md");
            text.put("content", "**" + item.getTitle() + "**\n" + item.getDescription());
            
            element.set("text", text);
            elements.add(element);
            
            // 添加分隔线
            ObjectNode hr = objectMapper.createObjectNode();
            hr.put("tag", "hr");
            elements.add(hr);
        }
        return this;
    }
    
    /**
     * 添加图片
     */
    public FeishuCardBuilder addImage(String imageKey, String altText) {
        ObjectNode element = objectMapper.createObjectNode();
        element.put("tag", "img");
        element.put("img_key", imageKey);
        element.set("alt", createTextElement(altText, "plain_text", false));
        element.put("mode", "fit_horizontal");
        elements.add(element);
        return this;
    }
    
    /**
     * 添加备注
     */
    public FeishuCardBuilder addNote(String text) {
        ObjectNode element = objectMapper.createObjectNode();
        element.put("tag", "note");
        
        ArrayNode noteElements = objectMapper.createArrayNode();
        noteElements.add(createTextElement(text, "lark_md", false));
        
        element.set("elements", noteElements);
        elements.add(element);
        return this;
    }
    
    /**
     * 添加列布局
     */
    public FeishuCardBuilder addColumns(List<Column> columns) {
        ObjectNode element = objectMapper.createObjectNode();
        element.put("tag", "column_set");
        element.put("flex_mode", "none");
        element.put("background_style", "default");
        
        ArrayNode columnArray = objectMapper.createArrayNode();
        for (Column col : columns) {
            ObjectNode column = objectMapper.createObjectNode();
            column.put("tag", "column");
            column.put("width", col.getWidth());
            
            ArrayNode colElements = objectMapper.createArrayNode();
            for (String text : col.getTexts()) {
                ObjectNode textElement = objectMapper.createObjectNode();
                textElement.put("tag", "div");
                textElement.set("text", createTextElement(text, "plain_text", false));
                colElements.add(textElement);
            }
            
            column.set("elements", colElements);
            columnArray.add(column);
        }
        
        element.set("columns", columnArray);
        elements.add(element);
        return this;
    }
    
    /**
     * 添加确认对话框
     */
    public FeishuCardBuilder addConfirm(String title, String message, 
                                        String confirmText, String cancelText,
                                        String confirmValue, String cancelValue) {
        // 标题
        addText(title, true);
        
        // 消息
        addText(message, false);
        
        // 按钮组
        List<ButtonConfig> buttons = new ArrayList<>();
        buttons.add(new ButtonConfig(confirmText, confirmValue, "primary"));
        buttons.add(new ButtonConfig(cancelText, cancelValue, "default"));
        addButtonGroup(buttons);
        
        return this;
    }
    
    /**
     * 构建卡片 JSON
     */
    public JsonNode build() {
        card.set("elements", elements);
        return card;
    }
    
    /**
     * 构建为字符串
     */
    public String buildAsString() {
        return build().toString();
    }
    
    // Helper methods
    
    private ObjectNode createConfig() {
        ObjectNode config = objectMapper.createObjectNode();
        config.put("wide_screen_mode", true);
        config.put("enable_forward", true);
        return config;
    }
    
    private ObjectNode createHeader(String title) {
        ObjectNode header = objectMapper.createObjectNode();
        header.set("title", createTextElement(title, "plain_text", true));
        header.put("template", "blue");
        return header;
    }
    
    private ObjectNode createTextElement(String content, String tag, boolean bold) {
        ObjectNode text = objectMapper.createObjectNode();
        text.put("tag", tag);
        if (bold) {
            content = "**" + content + "**";
        }
        text.put("content", content);
        return text;
    }
    
    private ObjectNode createMarkdownElement(String content) {
        ObjectNode text = objectMapper.createObjectNode();
        text.put("tag", "lark_md");
        text.put("content", content);
        return text;
    }
    
    // Inner classes
    
    public static class ButtonConfig {
        private final String text;
        private final String value;
        private final String type;
        
        public ButtonConfig(String text, String value, String type) {
            this.text = text;
            this.value = value;
            this.type = type;
        }
        
        public String getText() { return text; }
        public String getValue() { return value; }
        public String getType() { return type; }
    }
    
    public static class Option {
        private final String label;
        private final String value;
        
        public Option(String label, String value) {
            this.label = label;
            this.value = value;
        }
        
        public String getLabel() { return label; }
        public String getValue() { return value; }
    }
    
    public static class ListItem {
        private final String title;
        private final String description;
        
        public ListItem(String title, String description) {
            this.title = title;
            this.description = description;
        }
        
        public String getTitle() { return title; }
        public String getDescription() { return description; }
    }
    
    public static class Column {
        private final String width;
        private final List<String> texts;
        
        public Column(String width, List<String> texts) {
            this.width = width;
            this.texts = texts;
        }
        
        public String getWidth() { return width; }
        public List<String> getTexts() { return texts; }
    }
    
    // Static factory methods for common card templates
    
    /**
     * 创建确认卡片
     */
    public static FeishuCardBuilder confirmCard(String title, String message) {
        return new FeishuCardBuilder()
            .withTitle(title, null)
            .addText(message, false)
            .addButton("确认", "confirm", "primary")
            .addButton("取消", "cancel", "default");
    }
    
    /**
     * 创建表单卡片
     */
    public static FeishuCardBuilder formCard(String title, Map<String, String> fields) {
        FeishuCardBuilder builder = new FeishuCardBuilder()
            .withTitle(title, null);
        
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            builder.addInput(entry.getKey(), entry.getKey(), entry.getValue());
        }
        
        return builder.addButton("提交", "submit", "primary");
    }
    
    /**
     * 创建列表卡片
     */
    public static FeishuCardBuilder listCard(String title, List<ListItem> items) {
        return new FeishuCardBuilder()
            .withTitle(title, null)
            .addList(items);
    }
    
    /**
     * 创建成功提示卡片
     */
    public static FeishuCardBuilder successCard(String title, String message) {
        return new FeishuCardBuilder()
            .withTitle("✅ " + title, null)
            .addText(message, false)
            .addNote("操作已成功完成");
    }
    
    /**
     * 创建错误提示卡片
     */
    public static FeishuCardBuilder errorCard(String title, String message) {
        return new FeishuCardBuilder()
            .withTitle("❌ " + title, null)
            .addText(message, false)
            .addButton("重试", "retry", "primary")
            .addButton("取消", "cancel", "default");
    }
}
