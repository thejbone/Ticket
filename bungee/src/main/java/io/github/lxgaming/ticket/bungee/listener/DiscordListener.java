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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageEmbedEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class DiscordListener extends ListenerAdapter  {

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if(event.getAuthor().isBot())
            return;
        if(event.isFromType(ChannelType.TEXT)){
            Collection<TicketData> openTickets = null;
            long eventReferenceMessageId = Objects.requireNonNull(event.getMessage().getMessageReference()).getMessageIdLong();
            boolean continueMessage = false;
            if(event.getMessage().getChannel().getId().equals(BungeePlugin.getInstance().getDiscordToolbox().getDiscordCategory().getTextChannelTier1())){
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
                        Optional<UserData> userData = DataManager.getOrCreateUser(UUID.nameUUIDFromBytes(event.getAuthor().getId().getBytes()));
                        UserData userDataPresent = new UserData();
                        if(userData.isPresent()){
                            if(userData.get().getName().isEmpty())
                                userData.get().setName(event.getAuthor().getName());
                            userDataPresent = userData.get();
                        }
                        ActivityToolbox.sendComment(a, userDataPresent, event.getMessage().getContentRaw(), false).whenComplete((result,ex) -> {
                            if(result == null || !result){
                                BungeePlugin.getInstance().getLogger().info("Failed to send comment!!!");
                                event.getMessage().reply("Failed to send comment!").complete();
                            } else if (ex != null){
                                ex.printStackTrace();
                            }
                        });
                    }
                });
            }
        }
    }

}
