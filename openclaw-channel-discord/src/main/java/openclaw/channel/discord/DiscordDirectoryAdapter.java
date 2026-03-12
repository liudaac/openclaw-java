package openclaw.channel.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import openclaw.sdk.channel.ChannelDirectoryAdapter;
import openclaw.sdk.channel.DirectoryQueryResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Discord Directory Adapter
 */
public class DiscordDirectoryAdapter implements ChannelDirectoryAdapter {

    private JDA jda;

    public void setJda(JDA jda) {
        this.jda = jda;
    }

    @Override
    public CompletableFuture<DirectoryQueryResult> query(DirectoryQuery query) {
        return CompletableFuture.supplyAsync(() -> {
            if (jda == null) {
                return DirectoryQueryResult.failure("JDA not initialized");
            }

            try {
                switch (query.type()) {
                    case USER:
                        return queryUser(query);
                    case CHAT:
                        return queryChannel(query);
                    case GROUP:
                        return queryGuild(query);
                    default:
                        return DirectoryQueryResult.failure("Unsupported query type: " + query.type());
                }

            } catch (Exception e) {
                return DirectoryQueryResult.failure(e.getMessage());
            }
        });
    }

    private DirectoryQueryResult queryUser(DirectoryQuery query) {
        String userId = query.query();
        User user = jda.getUserById(userId);

        if (user == null) {
            return DirectoryQueryResult.failure("User not found: " + userId);
        }

        DirectoryEntry entry = new DirectoryEntry(
                user.getId(),
                user.getName(),
                user.getEffectiveAvatarUrl(),
                Optional.ofNullable(user.getGlobalName()).orElse(user.getName()),
                Map.of(
                        "discriminator", user.getDiscriminator(),
                        "bot", user.isBot()
                )
        );

        return DirectoryQueryResult.success(List.of(entry));
    }

    private DirectoryQueryResult queryChannel(DirectoryQuery query) {
        String channelId = query.query();
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel == null) {
            return DirectoryQueryResult.failure("Channel not found: " + channelId);
        }

        DirectoryEntry entry = new DirectoryEntry(
                channel.getId(),
                channel.getName(),
                "",
                channel.getGuild().getName(),
                Map.of(
                        "guildId", channel.getGuild().getId(),
                        "topic", channel.getTopic() != null ? channel.getTopic() : ""
                )
        );

        return DirectoryQueryResult.success(List.of(entry));
    }

    private DirectoryQueryResult queryGuild(DirectoryQuery query) {
        String guildId = query.query();
        Guild guild = jda.getGuildById(guildId);

        if (guild == null) {
            return DirectoryQueryResult.failure("Guild not found: " + guildId);
        }

        DirectoryEntry entry = new DirectoryEntry(
                guild.getId(),
                guild.getName(),
                guild.getIconUrl() != null ? guild.getIconUrl() : "",
                guild.getName(),
                Map.of(
                        "memberCount", guild.getMemberCount(),
                        "ownerId", guild.getOwnerId()
                )
        );

        return DirectoryQueryResult.success(List.of(entry));
    }

    @Override
    public CompletableFuture<List<DirectoryEntry>> listUsers(String chatId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            if (jda == null) {
                return List.of();
            }

            TextChannel channel = jda.getTextChannelById(chatId);
            if (channel == null) {
                return List.of();
            }

            Guild guild = channel.getGuild();
            return guild.getMembers().stream()
                    .limit(limit)
                    .map(member -> new DirectoryEntry(
                            member.getUser().getId(),
                            member.getUser().getName(),
                            member.getUser().getEffectiveAvatarUrl(),
                            member.getEffectiveName(),
                            Map.of("nickname", member.getNickname() != null ? member.getNickname() : "")
                    ))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<List<DirectoryEntry>> listChats(String groupId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            if (jda == null) {
                return List.of();
            }

            Guild guild = jda.getGuildById(groupId);
            if (guild == null) {
                return List.of();
            }

            return guild.getTextChannels().stream()
                    .limit(limit)
                    .map(channel -> new DirectoryEntry(
                            channel.getId(),
                            channel.getName(),
                            "",
                            guild.getName(),
                            Map.of("topic", channel.getTopic() != null ? channel.getTopic() : "")
                    ))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<Optional<DirectoryEntry>> getUserInfo(String userId) {
        return query(new DirectoryQuery(DirectoryQuery.QueryType.USER, userId, 1))
                .thenApply(result -> {
                    if (result.success() && !result.entries().isEmpty()) {
                        return Optional.of(result.entries().get(0));
                    }
                    return Optional.empty();
                });
    }

    @Override
    public CompletableFuture<Optional<DirectoryEntry>> getChatInfo(String chatId) {
        return query(new DirectoryQuery(DirectoryQuery.QueryType.CHAT, chatId, 1))
                .thenApply(result -> {
                    if (result.success() && !result.entries().isEmpty()) {
                        return Optional.of(result.entries().get(0));
                    }
                    return Optional.empty();
                });
    }
}
