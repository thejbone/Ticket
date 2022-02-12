package io.github.lxgaming.ticket.bungee.util;

import io.github.lxgaming.ticket.api.data.CommentData;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.bungee.BungeePlugin;
import io.github.lxgaming.ticket.bungee.listener.DiscordListener;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.configuration.Config;
import io.github.lxgaming.ticket.common.configuration.category.DiscordCategory;
import io.github.lxgaming.ticket.common.manager.DataManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

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
            this.jda = JDABuilder.createDefault(discordCategory.getToken()).build();
            this.jda.addEventListener(new DiscordListener());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void closeTicket(TicketData ticketData){
        CompletableFuture.runAsync(() -> {
            try {
                EmbedBuilder eb = new EmbedBuilder();
                Objects.requireNonNull(Objects.requireNonNull(jda.awaitReady().getGuildById(discordCategory.getGuildID())).getTextChannelById(getTicketChannel(ticketData.getTier()))).editMessageEmbedsById(ticketData.getDiscordMsgId()).queue(a -> {
                    eb.setTitle("Tier " + ticketData.getTier() + " #" + ticketData.getId() + " - " + (ticketData.getStatus() == 1 ? "Closed" : "Opened"));
                    eb.setDescription(ticketData.getText());
                    eb.setAuthor(Objects.requireNonNull(DataManager.getCachedUser(ticketData.getUser()).orElse(null)).getName() + " - " + ticketData.getLocation().getServer());
                    if(ticketData.getStatus() == 1)
                        eb.setColor(Color.black);
                    a.editMessageEmbeds(eb.build()).complete();
                });
            } catch(Exception e){
                e.printStackTrace();
            }
        });
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

    public void sendTicketComment(TicketData ticketData, CommentData commentData) {
        CompletableFuture.runAsync(() -> {
            try {
                jda.awaitReady().getGuildById(discordCategory.getGuildID()).getTextChannelById(getTicketChannel(ticketData.getTier())).editMessageEmbedsById(ticketData.getDiscordMsgId()).queue(a -> {
                  a.getEmbeds().get(0).getFields().add(new MessageEmbed.Field(
                          DataManager.getCachedUser(commentData.getUser()).orElse(null).getName() + " commented:  "
                          , commentData.getText(), false));
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Long> sendTicketData(TicketData ticketData, boolean esculated) {
        return CompletableFuture.supplyAsync(() -> {
            String channelID;
            EmbedBuilder eb = new EmbedBuilder();

            eb.setTitle("Tier " + ticketData.getTier() + " #" + ticketData.getId() + " - " + (ticketData.getStatus() == 0 ? "Open" : "Closed"));
            eb.setDescription(ticketData.getText());
            eb.setAuthor(Objects.requireNonNull(DataManager.getCachedUser(ticketData.getUser()).orElse(null)).getName() + " - " + ticketData.getLocation().getServer());

            switch(ticketData.getTier()){
                case 1:
                default:
                    channelID = discordCategory.getTextChannelTier1();
                    eb.setColor(Color.green);
                    break;
                case 2:
                    channelID = discordCategory.getTextChannelTier2();
                    eb.setColor(Color.blue);
                    break;
                case 3:
                    channelID = discordCategory.getTextChannelTier3();
                    eb.setColor(Color.red);
                    break;
            }

            if(esculated){
                ticketData.getComments().forEach(commentData -> {
                    eb.addField((Objects.requireNonNull(DataManager.getCachedUser(commentData.getUser()).orElse(null)).getName())+" commented: ", commentData.getText(), false);
                });
                eb.addField("This ticket was just escalated!", "", false);
            }
            Message message = null;
            try {
                 message = Objects.requireNonNull(Objects.requireNonNull(jda.awaitReady().getGuildById(discordCategory.getGuildID())).getTextChannelById(channelID)).sendMessageEmbeds(eb.build()).complete();
                 Objects.requireNonNull(Objects.requireNonNull(jda.awaitReady().getGuildById(discordCategory.getGuildID())).getTextChannelById(channelID)).addReactionById(Objects.requireNonNull(message).getIdLong(),"\uD83D\uDD13").complete();
                 Objects.requireNonNull(Objects.requireNonNull(jda.awaitReady().getGuildById(discordCategory.getGuildID())).getTextChannelById(channelID)).addReactionById(Objects.requireNonNull(message).getIdLong(),"\uD83D\uDD12").complete();
                 if(ticketData.getTier() < 3)
                     Objects.requireNonNull(Objects.requireNonNull(jda.awaitReady().getGuildById(discordCategory.getGuildID())).getTextChannelById(channelID)).addReactionById(Objects.requireNonNull(message).getIdLong(),"\u2B06\uFE0F").complete();
                 if(ticketData.getDiscordMsgId() != 0)
                     Objects.requireNonNull(Objects.requireNonNull(jda.awaitReady().getGuildById(discordCategory.getGuildID())).getTextChannelById(channelID)).deleteMessageById(ticketData.getDiscordMsgId()).complete();
            } catch(Exception e){
                e.printStackTrace();
            }
            return Objects.requireNonNull(message).getIdLong();

        });
    }


    public DiscordCategory getDiscordCategory() {
        return discordCategory;
    }


}
