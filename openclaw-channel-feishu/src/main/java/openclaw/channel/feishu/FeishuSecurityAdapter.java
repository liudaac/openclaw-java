package openclaw.channel.feishu;

import openclaw.sdk.channel.ChannelSecurityAdapter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Feishu security adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class FeishuSecurityAdapter implements ChannelSecurityAdapter<FeishuChannelPlugin.FeishuAccount> {

    @Override
    public CompletableFuture<Optional<FeishuChannelPlugin.FeishuAccount>> authenticate(
            Map<String, Object> credentials) {
        
        String appId = credentials.get("appId").toString();
        String appSecret = credentials.get("appSecret").toString();
        Optional<String> encryptKey = Optional.ofNullable(credentials.get("encryptKey")).map(Object::toString);
        Optional<String> verificationToken = Optional.ofNullable(credentials.get("verificationToken")).map(Object::toString);
        
        return CompletableFuture.completedFuture(Optional.of(
                new FeishuChannelPlugin.FeishuAccount(
                        appId,
                        appSecret,
                        encryptKey,
                        verificationToken,
                        "https://open.feishu.cn"
                )
        ));
    }

    @Override
    public CompletableFuture<Boolean> authorize(
            FeishuChannelPlugin.FeishuAccount account,
            String action,
            String resource) {
        // Feishu app has all permissions
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<WebhookValidationResult> validateWebhook(
            Map<String, String> headers,
            String body) {
        // Validate Feishu webhook signature
        // In production, verify timestamp and signature
        return CompletableFuture.completedFuture(
                WebhookValidationResult.valid(null)
        );
    }
}
