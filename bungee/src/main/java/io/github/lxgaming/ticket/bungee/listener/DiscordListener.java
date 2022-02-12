package io.github.lxgaming.ticket.bungee.listener;

import io.github.lxgaming.ticket.api.data.CommentData;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.bungee.BungeePlugin;
import io.github.lxgaming.ticket.bungee.util.ActivityToolbox;
import io.github.lxgaming.ticket.bungee.util.DiscordToolbox;
import io.github.lxgaming.ticket.common.manager.DataManager;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageEmbedEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.Instant;
import java.util.*;

public class DiscordListener extends ListenerAdapter  {

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event){
        if(Objects.requireNonNull(event.getUser()).isBot())
            return;
        if(event.isFromType(ChannelType.TEXT)){
            Collection<TicketData> tickets = null;
            long eventReferenceMessageId = event.getMessageIdLong();
            boolean continueMessage = false;
            if(event.getChannel().getId().equals(BungeePlugin.getInstance().getDiscordToolbox().getDiscordCategory().getTextChannelTier1())){
                tickets = DataManager.getCachedOpenTicketsByTier(1);
                continueMessage = true;
            }
            if(event.getChannel().getId().equals(BungeePlugin.getInstance().getDiscordToolbox().getDiscordCategory().getTextChannelTier2())){
                tickets = DataManager.getCachedOpenTicketsByTier(2);
                continueMessage = true;
            }
            if(event.getChannel().getId().equals(BungeePlugin.getInstance().getDiscordToolbox().getDiscordCategory().getTextChannelTier3())){
                tickets = DataManager.getCachedOpenTicketsByTier(3);
                continueMessage = true;
            }
            if(continueMessage){
                tickets.forEach(a -> {
                    if(a.getDiscordMsgId() == eventReferenceMessageId){
                        UserData userData = new UserData();
                        userData.setUniqueId(UUID.nameUUIDFromBytes(event.getUser().getId().getBytes()));
                        userData.setName(event.getUser().getName());
                        // Reopen
                        if(event.getReactionEmote().getName().contains("\uD83D\uDD13")){
                            event.getReaction().removeReaction().complete();
                            ActivityToolbox.sendClosedReopen(a, userData, 0, false).whenComplete((result,ex) -> {
                                if(result == null || !result){
                                    BungeePlugin.getInstance().getLogger().info("Failed to reopen ticket!");
                                } else if (ex != null){
                                    ex.printStackTrace();
                                }
                            });
                        }
                        // Close
                        else if(event.getReactionEmote().getName().contains("\uD83D\uDD12")){
                            event.getReaction().removeReaction().complete();
                            ActivityToolbox.sendClosedReopen(a, userData, 1, false).whenComplete((result,ex) -> {
                                if(result == null || !result){
                                    BungeePlugin.getInstance().getLogger().info("Failed to reopen ticket!");
                                } else if (ex != null){
                                    ex.printStackTrace();
                                }
                            });
                        }
                        // Escalate
                        else if(event.getReactionEmote().getName().contains("\u2B06")){
                            event.getReaction().removeReaction().complete();
                            ActivityToolbox.sendEscalate(a,userData, false).whenComplete((result, ex) -> {
                                if(result == null || !result){
                                    BungeePlugin.getInstance().getLogger().info("Failed to escalate ticket!");
                                } else if (ex != null){
                                    ex.printStackTrace();
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if(event.getAuthor().isBot())
            return;
        if(event.isFromType(ChannelType.TEXT)){
            Collection<TicketData> openTickets = null;
            long eventReferenceMessageId = Objects.requireNonNull(event.getMessage().getMessageReference()).getMessageIdLong();
            boolean continueMessage = false;
            if(event.getChannel().getId().equals(BungeePlugin.getInstance().getDiscordToolbox().getDiscordCategory().getTextChannelTier1())){
                openTickets = DataManager.getCachedOpenTicketsByTier(1);
                continueMessage = true;
            }
            if(event.getChannel().getId().equals(BungeePlugin.getInstance().getDiscordToolbox().getDiscordCategory().getTextChannelTier2())){
                openTickets = DataManager.getCachedOpenTicketsByTier(2);
                continueMessage = true;
            }
            if(event.getChannel().getId().equals(BungeePlugin.getInstance().getDiscordToolbox().getDiscordCategory().getTextChannelTier3())){
                openTickets = DataManager.getCachedOpenTicketsByTier(3);
                continueMessage = true;
            }
            if(continueMessage){
                openTickets.forEach(a -> {
                    if(a.getDiscordMsgId() == eventReferenceMessageId){
                        String msg = event.getMessage().getContentRaw();
                        UserData userData = new UserData();
                        userData.setUniqueId(UUID.nameUUIDFromBytes(event.getAuthor().getId().getBytes()));
                        userData.setName(event.getAuthor().getName());
                        ActivityToolbox.sendComment(a, userData, msg, false).whenComplete((result,ex) -> {
                            if(result == null || !result){
                                BungeePlugin.getInstance().getLogger().info("Failed to send comment!!!");
                                event.getMessage().reply("Failed to send comment!").complete();
                            } else if (ex != null){
                                ex.printStackTrace();
                            }
                        });
                        event.getMessage().delete().queue();
                    }
                });
            }
        }
    }

}
