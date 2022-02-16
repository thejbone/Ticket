package io.github.lxgaming.ticket.bungee.util;

import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.bungee.listener.DiscordListener;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.configuration.Config;
import io.github.lxgaming.ticket.common.configuration.category.DiscordCategory;
import io.github.lxgaming.ticket.common.manager.DataManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class DiscordToolbox {

    private JDA jda;
    private DiscordCategory discordCategory;

    public DiscordToolbox() {
        init();
    }

    public void init(){
        try {
            discordCategory = TicketImpl.getInstance().getConfig().map(Config::getDiscord).orElseThrow(IllegalStateException::new);
            this.jda = JDABuilder.createDefault(discordCategory.getToken()).setActivity(Activity.watching(discordCategory.getActivity())).build();
            this.jda.addEventListener(new DiscordListener());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public String getTicketChannel(int tier){
        String channelID;

        switch(tier){
            case 1:
            default:
                channelID = discordCategory.getTextChannelTier1();
                break;
            case 2:
                channelID = discordCategory.getTextChannelTier2();
                break;
            case 3:
                channelID = discordCategory.getTextChannelTier3();
                break;
        }

        return channelID;
    }

    public CompletableFuture<Long> sendTicketData(TicketData ticketData, boolean escalated) {
        return CompletableFuture.supplyAsync(() -> {
            String channelID = getTicketChannel(ticketData.getTier());
            EmbedBuilder eb = new EmbedBuilder();

            eb.setTitle("#" + ticketData.getId() + " - " + (ticketData.getStatus() == 0 ? "Open" : "Closed"));
            eb.setDescription(ticketData.getText());
            eb.setAuthor(Objects.requireNonNull(DataManager.getCachedUser(ticketData.getUser()).orElse(null)).getName() + " - " + ticketData.getLocation().getServer());

            ticketData.getComments().forEach(commentData -> {
                eb.addField((Objects.requireNonNull(DataManager.getCachedUser(commentData.getUser()).orElse(null)).getName())+" commented: ", commentData.getText(), false);
            });
            if(escalated){
                eb.addField("This ticket was just escalated!", "", false);
                if(ticketData.getDiscordMsgId() != 0)
                    deleteComment(ticketData, ticketData.getTier()-1);
            }
            eb.setColor(ticketData.getStatus() == 0 ? Color.GREEN : Color.GRAY);
            Message message = null;
            try {
                 message = Objects.requireNonNull(Objects.requireNonNull(jda.awaitReady().getGuildById(discordCategory.getGuildID())).getTextChannelById(channelID)).sendMessageEmbeds(eb.build()).complete();
                 if(ticketData.getStatus() == 1)
                     Objects.requireNonNull(Objects.requireNonNull(jda.awaitReady().getGuildById(discordCategory.getGuildID())).getTextChannelById(channelID)).addReactionById(Objects.requireNonNull(message).getIdLong(),"\uD83D\uDD13").queue();
                 if(ticketData.getStatus() == 0){
                     Objects.requireNonNull(Objects.requireNonNull(jda.awaitReady().getGuildById(discordCategory.getGuildID())).getTextChannelById(channelID)).addReactionById(Objects.requireNonNull(message).getIdLong(),"\uD83D\uDD12").queue();
                     if(ticketData.getTier() < 3)
                         Objects.requireNonNull(Objects.requireNonNull(jda.awaitReady().getGuildById(discordCategory.getGuildID())).getTextChannelById(channelID)).addReactionById(Objects.requireNonNull(message).getIdLong(),"\u2B06\uFE0F").queue();
                 }
                 if(ticketData.getDiscordMsgId() != 0 && !escalated)
                     deleteComment(ticketData, ticketData.getTier());
            } catch(Exception e){
                e.printStackTrace();
            }
            return Objects.requireNonNull(message).getIdLong();

        });
    }

    public void deleteComment(TicketData ticket, int tier){
           try {
               Objects.requireNonNull(Objects.requireNonNull(jda.awaitReady().getGuildById(discordCategory.getGuildID())).getTextChannelById(getTicketChannel(tier))).deleteMessageById(ticket.getDiscordMsgId()).complete();
           }
           catch (Exception e){
               e.printStackTrace();
           }
    }

    public DiscordCategory getDiscordCategory() {
        return discordCategory;
    }


}
