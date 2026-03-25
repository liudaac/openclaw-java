package openclaw.channel.feishu;

import openclaw.channel.feishu.config.FeishuGroupConfig;
import openclaw.channel.feishu.policy.FeishuGroupPolicy;
import openclaw.channel.feishu.policy.FeishuPolicy;
import openclaw.sdk.channel.ChannelConfigAdapter;
import openclaw.sdk.channel.ChannelConfigSchema;
import openclaw.sdk.channel.ConfigUiHint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Feishu configuration adapter with policy support.
 *
 * <p>Enhanced with group policy configuration support.</p>
 *
 * <p>Ported from original TypeScript: extensions/feishu/src/policy.ts</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.25
 */
public class FeishuConfigAdapter implements ChannelConfigAdapter<FeishuChannelPlugin.FeishuAccount> {

    private FeishuPolicy policy;
    private final Map<String, FeishuGroupConfig> groupConfigs;

    public FeishuConfigAdapter() {
        this.policy = FeishuPolicy.defaults();
        this.groupConfigs = new HashMap<>();
    }

    @Override
    public CompletableFuture<ConfigValidationResult> validate(Map<String, Object> config) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> errors = new ArrayList<>();

            // Check required fields
            if (!config.containsKey("appId")) {
                errors.add("appId is required");
            }
            if (!config.containsKey("appSecret")) {
                errors.add("appSecret is required");
            }

            // Validate appId format
            if (config.containsKey("appId")) {
                String appId = config.get("appId").toString();
                if (appId.length() < 10) {
                    errors.add("Invalid appId format");
                }
            }

            // Validate groupPolicy if present
            if (config.containsKey("groupPolicy")) {
                String policyValue = config.get("groupPolicy").toString();
                try {
                    FeishuGroupPolicy.fromString(policyValue);
                } catch (Exception e) {
                    errors.add("Invalid groupPolicy value: " + policyValue);
                }
            }

            // Validate dmPolicy if present
            if (config.containsKey("dmPolicy")) {
                String dmPolicy = config.get("dmPolicy").toString();
                if (!List.of("open", "pairing", "disabled").contains(dmPolicy)) {
                    errors.add("Invalid dmPolicy value: " + dmPolicy);
                }
            }

            if (errors.isEmpty()) {
                return ConfigValidationResult.success();
            }
            return ConfigValidationResult.failure(errors);
        });
    }

    @Override
    public CompletableFuture<Optional<FeishuChannelPlugin.FeishuAccount>> resolveAccount(Map<String, Object> config) {
        return validate(config).thenApply(result -> {
            if (!result.valid()) {
                return Optional.empty();
            }

            String appId = config.get("appId").toString();
            String appSecret = config.get("appSecret").toString();
            String apiUrl = config.getOrDefault("apiUrl", "https://open.feishu.cn").toString();

            Optional<String> encryptKey = Optional.ofNullable(config.get("encryptKey")).map(Object::toString);
            Optional<String> verificationToken = Optional.ofNullable(config.get("verificationToken")).map(Object::toString);

            // Build policy from config
            this.policy = buildPolicyFromConfig(config);

            // Build group configs from config
            buildGroupConfigsFromConfig(config);

            return Optional.of(new FeishuChannelPlugin.FeishuAccount(
                    appId, appSecret, encryptKey, verificationToken, apiUrl
            ));
        });
    }

    /**
     * Builds policy from configuration map.
     */
    private FeishuPolicy buildPolicyFromConfig(Map<String, Object> config) {
        FeishuPolicy.Builder builder = FeishuPolicy.builder();

        // groupPolicy
        if (config.containsKey("groupPolicy")) {
            builder.groupPolicy(FeishuGroupPolicy.fromString(config.get("groupPolicy").toString()));
        }

        // requireMention
        if (config.containsKey("requireMention")) {
            builder.requireMention(Boolean.parseBoolean(config.get("requireMention").toString()));
        }

        // allowFrom
        if (config.containsKey("allowFrom")) {
            builder.allowFrom(parseStringList(config.get("allowFrom")));
        }

        // groupAllowFrom
        if (config.containsKey("groupAllowFrom")) {
            builder.groupAllowFrom(parseStringList(config.get("groupAllowFrom")));
        }

        // dmPolicy
        if (config.containsKey("dmPolicy")) {
            builder.dmPolicy(config.get("dmPolicy").toString());
        }

        // historyLimit
        if (config.containsKey("historyLimit")) {
            try {
                builder.historyLimit(Integer.parseInt(config.get("historyLimit").toString()));
            } catch (NumberFormatException e) {
                // Use default
            }
        }

        // mediaMaxMb
        if (config.containsKey("mediaMaxMb")) {
            try {
                builder.mediaMaxMb(Integer.parseInt(config.get("mediaMaxMb").toString()));
            } catch (NumberFormatException e) {
                // Use default
            }
        }

        return builder.build();
    }

    /**
     * Builds group configs from configuration map.
     */
    @SuppressWarnings("unchecked")
    private void buildGroupConfigsFromConfig(Map<String, Object> config) {
        groupConfigs.clear();

        if (!config.containsKey("groups")) {
            return;
        }

        Object groupsObj = config.get("groups");
        if (!(groupsObj instanceof Map)) {
            return;
        }

        Map<String, Object> groups = (Map<String, Object>) groupsObj;
        for (Map.Entry<String, Object> entry : groups.entrySet()) {
            String groupId = entry.getKey();
            Object groupConfigObj = entry.getValue();

            if (!(groupConfigObj instanceof Map)) {
                continue;
            }

            try {
                Map<String, Object> groupConfig = (Map<String, Object>) groupConfigObj;
                FeishuGroupConfig.Builder builder = FeishuGroupConfig.builder()
                        .groupId(groupId);

                if (groupConfig.containsKey("requireMention")) {
                    builder.requireMention(Boolean.parseBoolean(groupConfig.get("requireMention").toString()));
                }

                if (groupConfig.containsKey("groupPolicy")) {
                    builder.groupPolicy(FeishuGroupPolicy.fromString(groupConfig.get("groupPolicy").toString()));
                }

                if (groupConfig.containsKey("allowFrom")) {
                    builder.allowFrom(parseStringList(groupConfig.get("allowFrom")));
                }

                if (groupConfig.containsKey("topicSessionMode")) {
                    builder.topicSessionMode(groupConfig.get("topicSessionMode").toString());
                }

                groupConfigs.put(groupId, builder.build());
            } catch (Exception e) {
                // Skip invalid group config
            }
        }
    }

    /**
     * Parses a string list from config value.
     */
    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        if (value instanceof String) {
            String str = (String) value;
            if (str.isEmpty()) {
                return List.of();
            }
            // Handle comma-separated string
            return List.of(str.split(","));
        }
        return List.of();
    }

    @Override
    public ChannelConfigSchema getSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        properties.put("appId", Map.of(
                "type", "string",
                "description", "Feishu app ID"
        ));
        properties.put("appSecret", Map.of(
                "type", "string",
                "description", "Feishu app secret"
        ));
        properties.put("encryptKey", Map.of(
                "type", "string",
                "description", "Webhook encrypt key"
        ));
        properties.put("verificationToken", Map.of(
                "type", "string",
                "description", "Webhook verification token"
        ));
        properties.put("apiUrl", Map.of(
                "type", "string",
                "description", "Feishu API URL",
                "default", "https://open.feishu.cn"
        ));
        properties.put("groupPolicy", Map.of(
                "type", "string",
                "description", "Group chat policy: open, allowlist, or disabled",
                "default", "open",
                "enum", List.of("open", "allowlist", "disabled")
        ));
        properties.put("dmPolicy", Map.of(
                "type", "string",
                "description", "Direct message policy: open, pairing, or disabled",
                "default", "pairing",
                "enum", List.of("open", "pairing", "disabled")
        ));
        properties.put("requireMention", Map.of(
                "type", "boolean",
                "description", "Require @mention in group chats",
                "default", true
        ));
        properties.put("allowFrom", Map.of(
                "type", "array",
                "description", "Allowed user IDs/names for DM",
                "items", Map.of("type", "string")
        ));
        properties.put("groupAllowFrom", Map.of(
                "type", "array",
                "description", "Allowed group IDs for group chats",
                "items", Map.of("type", "string")
        ));
        properties.put("historyLimit", Map.of(
                "type", "integer",
                "description", "Group chat history limit",
                "default", 50
        ));
        properties.put("mediaMaxMb", Map.of(
                "type", "integer",
                "description", "Maximum media file size in MB",
                "default", 30
        ));
        properties.put("groups", Map.of(
                "type", "object",
                "description", "Per-group configuration"
        ));

        schema.put("properties", properties);
        schema.put("required", List.of("appId", "appSecret"));

        Map<String, ConfigUiHint> hints = Map.of(
                "appId", ConfigUiHint.builder()
                        .label("App ID")
                        .help("From Feishu Developer Console")
                        .build(),
                "appSecret", ConfigUiHint.builder()
                        .label("App Secret")
                        .help("From Feishu Developer Console")
                        .sensitive(true)
                        .build(),
                "encryptKey", ConfigUiHint.builder()
                        .label("Encrypt Key")
                        .help("For webhook security")
                        .sensitive(true)
                        .advanced(true)
                        .build(),
                "groupPolicy", ConfigUiHint.builder()
                        .label("Group Policy")
                        .help("open: respond to all, allowlist: only allowed users, disabled: no response")
                        .build(),
                "dmPolicy", ConfigUiHint.builder()
                        .label("DM Policy")
                        .help("open: respond to all, pairing: require pairing, disabled: no response")
                        .build(),
                "requireMention", ConfigUiHint.builder()
                        .label("Require Mention")
                        .help("When enabled, bot only responds to @mentions in groups")
                        .build()
        );

        return ChannelConfigSchema.withHints(schema, hints);
    }

    @Override
    public Map<String, Object> getDefaults() {
        return Map.of(
                "apiUrl", "https://open.feishu.cn",
                "groupPolicy", "open",
                "dmPolicy", "pairing",
                "requireMention", true,
                "historyLimit", 50,
                "mediaMaxMb", 30
        );
    }

    /**
     * Gets the current policy.
     *
     * @return the policy
     */
    public FeishuPolicy getPolicy() {
        return policy;
    }

    /**
     * Gets the group configs map.
     *
     * @return map of group ID to config
     */
    public Map<String, FeishuGroupConfig> getGroupConfigs() {
        return Map.copyOf(groupConfigs);
    }

    /**
     * Gets a specific group config.
     *
     * @param groupId the group ID
     * @return the group config if found
     */
    public Optional<FeishuGroupConfig> getGroupConfig(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return Optional.empty();
        }

        // Direct match
        FeishuGroupConfig direct = groupConfigs.get(groupId);
        if (direct != null) {
            return Optional.of(direct);
        }

        // Case-insensitive match
        String lowered = groupId.toLowerCase();
        for (Map.Entry<String, FeishuGroupConfig> entry : groupConfigs.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toLowerCase().equals(lowered)) {
                return Optional.of(entry.getValue());
            }
        }

        return Optional.empty();
    }
}