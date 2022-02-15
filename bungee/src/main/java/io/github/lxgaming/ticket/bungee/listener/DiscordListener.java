package io.github.lxgaming.ticket.bungee.listener;

import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.bungee.BungeePlugin;
import io.github.lxgaming.ticket.bungee.util.ActivityToolbox;
import io.github.lxgaming.ticket.common.manager.DataManager;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.*;

public class DiscordListener extends ListenerAdapter  {

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event){
        if(Objects.requireNonNull(event.getUser()).isBot())
            return;
        if(event.isFromType(ChannelType.TEXT)){
            long eventReferenceMessageId = event.getMessageIdLong();
            if(event.getChannel().getId().equals(BungeePlugin.getInstance().getDiscordToolbox().getDiscordCategory().getTextChannelTier1()) ||
                    event.getChannel().getId().equals(BungeePlugin.getInstance().getDiscordToolbox().getDiscordCategory().getTextChannelTier2()) ||
                    event.getChannel().getId().equals(BungeePlugin.getInstance().getDiscordToolbox().getDiscordCategory().getTextChannelTier3())) {
                Optional<TicketData> ticketOptional = DataManager.getByDiscordMessageID(eventReferenceMessageId);
                if(ticketOptional.isPresent() && ticketOptional.get().getDiscordMsgId() == eventReferenceMessageId){
                    TicketData ticket = ticketOptional.get();
                    UserData userData = new UserData();
                    userData.setUniqueId(UUID.nameUUIDFromBytes(event.getUser().getId().getBytes()));
                    userData.setName(event.getUser().getName());
                        // Reopen
                        if(event.getReactionEmote().getName().contains("\uD83D\uDD13")){
                            event.getReaction().removeReaction().queue();
                            ActivityToolbox.sendClosedReopen(ticket, userData, 0, false).whenComplete((result,ex) -> {
                                if(result == null || !result){
                                    BungeePlugin.getInstance().getLogger().info("Failed to reopen ticket!");
                                } else if (ex != null){
                                    ex.printStackTrace();
                                }
                            });
                        }
                    // Close
                    if(event.getReactionEmote().getName().contains("\uD83D\uDD12")){
                        event.getReaction().removeReaction().queue();
                        ActivityToolbox.sendClosedReopen(ticket, userData, 1, false).whenComplete((result,ex) -> {
                            if(result == null || !result){
                                BungeePlugin.getInstance().getLogger().info("Failed to close ticket!");
                            } else if (ex != null){
                                ex.printStackTrace();
                            }
                        });
                    }
                    // Escalate
                    else if(event.getReactionEmote().getName().contains("\u2B06")){
                        ActivityToolbox.sendEscalate(ticket,userData).whenComplete((result, ex) -> {
                            if(result == null || !result){
                                BungeePlugin.getInstance().getLogger().info("Failed to escalate ticket!");
                            } else if (ex != null){
                                ex.printStackTrace();
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if(event.getAuthor().isBot())
            return;
        if(event.isFromType(ChannelType.TEXT)){
            long eventReferenceMessageId = Objects.requireNonNull(event.getMessage().getMessageReference()).getMessageIdLong();
            if(event.getChannel().getId().equals(BungeePlugin.getInstance().getDiscordToolbox().getDiscordCategory().getTextChannelTier1()) ||
                    event.getChannel().getId().equals(BungeePlugin.getInstance().getDiscordToolbox().getDiscordCategory().getTextChannelTier2()) ||
                    event.getChannel().getId().equals(BungeePlugin.getInstance().getDiscordToolbox().getDiscordCategory().getTextChannelTier3())) {
                Optional<TicketData> ticketOptional = DataManager.getByDiscordMessageID(eventReferenceMessageId);
                if(ticketOptional.isPresent() && ticketOptional.get().getDiscordMsgId() == eventReferenceMessageId){
                    TicketData ticket = ticketOptional.get();
                    String msg = event.getMessage().getContentRaw();
                    event.getMessage().delete().complete();
                    UserData userData = new UserData();
                    userData.setUniqueId(UUID.nameUUIDFromBytes(event.getAuthor().getId().getBytes()));
                    userData.setName(event.getAuthor().getName());
                    ActivityToolbox.sendComment(ticket, userData, msg, true).whenComplete((result,ex) -> {
                        if(result == null || !result){
                            BungeePlugin.getInstance().getLogger().info("Failed to send comment!!!");
                            event.getMessage().reply("Failed to send comment!").complete();
                        } else if (ex != null){
                            ex.printStackTrace();
                        }
                    });
                }
            }
        }
    }

}
