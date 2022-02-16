package io.github.lxgaming.ticket.common.configuration.category;

public class DiscordCategory {

    private String token = "TOKEN";
    private String activity = "Activity";
    private String guildID = "GuildID";
    private String textChannelTier1 = "TextChannelTier1";
    private String textChannelTier2 = "TextChannelTier2";
    private String textChannelTier3 = "TextChannelTier3";

    public String getToken() {
        return token;
    }

    public String getGuildID() {
        return guildID;
    }

    public String getActivity() {
        return activity;
    }

    public String getTextChannelTier1() {
        return textChannelTier1;
    }

    public String getTextChannelTier2() {
        return textChannelTier2;
    }

    public String getTextChannelTier3() {
        return textChannelTier3;
    }

}
