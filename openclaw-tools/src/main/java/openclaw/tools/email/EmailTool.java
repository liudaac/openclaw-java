package openclaw.tools.email;

import openclaw.plugin.sdk.tool.Tool;
import openclaw.plugin.sdk.tool.ToolContext;
import openclaw.plugin.sdk.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Email Tool - SMTP 邮件发送工具
 * 
 * 功能:
 * - 发送纯文本邮件
 * - 发送 HTML 邮件
 * - 发送带附件的邮件
 * - 支持多收件人
 * - 支持抄送/密送
 * 
 * 对应 Node.js: src/tools/email.ts
 */
@Component
public class EmailTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailTool.class);
    
    @Override
    public String getName() {
        return "email";
    }
    
    @Override
    public String getDescription() {
        return "Send emails via SMTP with support for text, HTML, and attachments";
    }
    
    @Override
    public String getSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "to": {
                  "type": "array",
                  "items": {"type": "string"},
                  "description": "List of recipient email addresses"
                },
                "cc": {
                  "type": "array",
                  "items": {"type": "string"},
                  "description": "List of CC email addresses"
                },
                "bcc": {
                  "type": "array",
                  "items": {"type": "string"},
                  "description": "List of BCC email addresses"
                },
                "subject": {
                  "type": "string",
                  "description": "Email subject"
                },
                "body": {
                  "type": "string",
                  "description": "Email body content"
                },
                "isHtml": {
                  "type": "boolean",
                  "description": "Whether body is HTML",
                  "default": false
                },
                "attachments": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "filename": {"type": "string"},
                      "content": {"type": "string"},
                      "contentType": {"type": "string"}
                    }
                  },
                  "description": "List of attachments"
                },
                "smtpHost": {
                  "type": "string",
                  "description": "SMTP server host"
                },
                "smtpPort": {
                  "type": "integer",
                  "description": "SMTP server port"
                },
                "username": {
                  "type": "string",
                  "description": "SMTP username"
                },
                "password": {
                  "type": "string",
                  "description": "SMTP password"
                },
                "useSsl": {
                  "type": "boolean",
                  "description": "Use SSL/TLS",
                  "default": true
                }
              },
              "required": ["to", "subject", "body"]
            }
            """;
    }
    
    @Override
    public CompletableFuture<ToolResult> execute(Map<String, Object> params, ToolContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 解析参数
                List<String> to = parseStringList(params.get("to"));
                List<String> cc = parseStringList(params.getOrDefault("cc", Collections.emptyList()));
                List<String> bcc = parseStringList(params.getOrDefault("bcc", Collections.emptyList()));
                String subject = (String) params.get("subject");
                String body = (String) params.get("body");
                boolean isHtml = (boolean) params.getOrDefault("isHtml", false);
                List<Map<String, Object>> attachments = parseAttachmentList(params.get("attachments"));
                
                // SMTP 配置
                String smtpHost = (String) params.get("smtpHost");
                Integer smtpPort = (Integer) params.get("smtpPort");
                String username = (String) params.get("username");
                String password = (String) params.get("password");
                boolean useSsl = (boolean) params.getOrDefault("useSsl", true);
                
                // 如果未提供 SMTP 配置，从环境变量或配置中获取
                if (smtpHost == null) {
                    smtpHost = System.getenv("SMTP_HOST");
                    smtpPort = smtpPort != null ? smtpPort : 
                        Integer.parseInt(System.getenv().getOrDefault("SMTP_PORT", "587"));
                    username = username != null ? username : System.getenv("SMTP_USERNAME");
                    password = password != null ? password : System.getenv("SMTP_PASSWORD");
                }
                
                if (smtpHost == null || username == null || password == null) {
                    return ToolResult.error("SMTP configuration not provided");
                }
                
                // 发送邮件
                EmailResult result = sendEmail(
                    to, cc, bcc, subject, body, isHtml, attachments,
                    smtpHost, smtpPort, username, password, useSsl
                );
                
                if (result.isSuccess()) {
                    return ToolResult.success(Map.of(
                        "messageId", result.getMessageId(),
                        "recipients", result.getRecipientCount(),
                        "attachments", result.getAttachmentCount()
                    ));
                } else {
                    return ToolResult.error(result.getErrorMessage());
                }
                
            } catch (Exception e) {
                logger.error("Failed to send email", e);
                return ToolResult.error("Failed to send email: " + e.getMessage());
            }
        });
    }
    
    private EmailResult sendEmail(
            List<String> to, List<String> cc, List<String> bcc,
            String subject, String body, boolean isHtml,
            List<Map<String, Object>> attachments,
            String smtpHost, int smtpPort,
            String username, String password, boolean useSsl) throws Exception {
        
        // 创建邮件会话
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(useSsl));
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.ssl.trust", smtpHost);
        
        if (useSsl && smtpPort == 465) {
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }
        
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        
        // 创建邮件消息
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        
        // 设置收件人
        for (String recipient : to) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }
        
        // 设置抄送
        for (String recipient : cc) {
            message.addRecipient(Message.RecipientType.CC, new InternetAddress(recipient));
        }
        
        // 设置密送
        for (String recipient : bcc) {
            message.addRecipient(Message.RecipientType.BCC, new InternetAddress(recipient));
        }
        
        // 设置主题
        message.setSubject(subject);
        
        // 创建邮件内容
        if (attachments == null || attachments.isEmpty()) {
            // 无附件
            if (isHtml) {
                message.setContent(body, "text/html; charset=utf-8");
            } else {
                message.setText(body);
            }
        } else {
            // 有附件
            MimeMultipart multipart = new MimeMultipart("mixed");
            
            // 添加正文
            MimeBodyPart bodyPart = new MimeBodyPart();
            if (isHtml) {
                bodyPart.setContent(body, "text/html; charset=utf-8");
            } else {
                bodyPart.setText(body);
            }
            multipart.addBodyPart(bodyPart);
            
            // 添加附件
            for (Map<String, Object> attachment : attachments) {
                String filename = (String) attachment.get("filename");
                String content = (String) attachment.get("content");
                String contentType = (String) attachment.getOrDefault("contentType", 
                    "application/octet-stream");
                
                MimeBodyPart attachmentPart = new MimeBodyPart();
                
                // 解码 Base64 内容
                byte[] contentBytes = Base64.getDecoder().decode(content);
                DataSource dataSource = new ByteArrayDataSource(contentBytes, contentType);
                attachmentPart.setDataHandler(new DataHandler(dataSource));
                attachmentPart.setFileName(filename);
                
                multipart.addBodyPart(attachmentPart);
            }
            
            message.setContent(multipart);
        }
        
        // 发送邮件
        Transport.send(message);
        
        String messageId = ((MimeMessage) message).getMessageID();
        int recipientCount = to.size() + cc.size() + bcc.size();
        int attachmentCount = attachments != null ? attachments.size() : 0;
        
        logger.info("Email sent successfully: {} to {} recipients", messageId, recipientCount);
        
        return new EmailResult(true, messageId, recipientCount, attachmentCount, null);
    }
    
    private List<String> parseStringList(Object obj) {
        if (obj == null) return Collections.emptyList();
        if (obj instanceof List) {
            return ((List<?>) obj).stream()
                .map(Object::toString)
                .toList();
        }
        if (obj instanceof String) {
            return List.of((String) obj);
        }
        return Collections.emptyList();
    }
    
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseAttachmentList(Object obj) {
        if (obj == null) return Collections.emptyList();
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        return Collections.emptyList();
    }
    
    // Inner classes
    
    private static class EmailResult {
        private final boolean success;
        private final String messageId;
        private final int recipientCount;
        private final int attachmentCount;
        private final String errorMessage;
        
        public EmailResult(boolean success, String messageId, int recipientCount,
                          int attachmentCount, String errorMessage) {
            this.success = success;
            this.messageId = messageId;
            this.recipientCount = recipientCount;
            this.attachmentCount = attachmentCount;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessageId() { return messageId; }
        public int getRecipientCount() { return recipientCount; }
        public int getAttachmentCount() { return attachmentCount; }
        public String getErrorMessage() { return errorMessage; }
    }
}
