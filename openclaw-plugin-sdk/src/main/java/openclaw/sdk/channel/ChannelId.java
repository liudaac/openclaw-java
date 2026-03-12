package openclaw.sdk.channel;

/**
 * Channel identifier.
 *
 * @param id the channel ID string
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record ChannelId(String id) {

    /**
     * Well-known channel IDs.
     */
    public static final ChannelId TELEGRAM = new ChannelId("telegram");
    public static final ChannelId SLACK = new ChannelId("slack");
    public static final ChannelId DISCORD = new ChannelId("discord");
    public static final ChannelId FEISHU = new ChannelId("feishu");
    public static final ChannelId WHATSAPP = new ChannelId("whatsapp");
    public static final ChannelId SIGNAL = new ChannelId("signal");
    public static final ChannelId IMESSAGE = new ChannelId("imessage");
    public static final ChannelId GOOGLE_CHAT = new ChannelId("googlechat");
    public static final ChannelId MATRIX = new ChannelId("matrix");
    public static final ChannelId IRC = new ChannelId("irc");
    public static final ChannelId LINE = new ChannelId("line");
    public static final ChannelId MS_TEAMS = new ChannelId("msteams");
    public static final ChannelId MATTERMOST = new ChannelId("mattermost");
    public static final ChannelId NEXTCLOUD_TALK = new ChannelId("nextcloud-talk");
    public static final ChannelId NOSTR = new ChannelId("nostr");
    public static final ChannelId SYNOLOGY_CHAT = new ChannelId("synology-chat");
    public static final ChannelId TLON = new ChannelId("tlon");
    public static final ChannelId TWITCH = new ChannelId("twitch");
    public static final ChannelId ZALO = new ChannelId("zalo");
    public static final ChannelId ZALO_USER = new ChannelId("zalouser");
    public static final ChannelId WEB_CHAT = new ChannelId("webchat");

    @Override
    public String toString() {
        return id;
    }
}
