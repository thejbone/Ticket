package io.github.lxgaming.ticket.bungee.util;

import io.github.lxgaming.ticket.api.data.CommentData;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.api.util.Reference;
import io.github.lxgaming.ticket.bungee.BungeePlugin;
import io.github.lxgaming.ticket.common.configuration.Configuration;
import io.github.lxgaming.ticket.common.manager.DataManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ActivityToolbox {

    public static CompletableFuture<Boolean> sendComment(TicketData ticket, UserData user, String text, boolean sendDiscordMessage){
        return CompletableFuture.supplyAsync(() -> {
            CommentData comment = DataManager.createComment(ticket.getId(), user.getUniqueId(), Instant.now(), text).orElse(null);

            BungeeToolbox.sendRedisMessage("TicketComment", jsonObject -> {
                jsonObject.add("ticket", Configuration.getGson().toJsonTree(ticket));
                jsonObject.add("user", Configuration.getGson().toJsonTree(user));
            });

            BaseComponent[] baseComponents = BungeeToolbox.getTextPrefix()
                    .append(user.getName()).color(ChatColor.YELLOW)
                    .append(" added a comment to Ticket #" + ticket.getId()).color(ChatColor.GOLD).create();

            ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(ticket.getUser());
            if (player != null) {
                player.sendMessage(baseComponents);

                String command = "/" + Reference.ID + " read " + ticket.getId();
                player.sendMessage(BungeeToolbox.getTextPrefix()
                        .append("Use ").color(ChatColor.GOLD)
                        .append(command).color(ChatColor.GREEN).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .append(" to view your ticket").color(ChatColor.GOLD).create());
            }
            if(sendDiscordMessage)
                BungeePlugin.getInstance().getDiscordToolbox().sendTicketComment(ticket, comment);

            BungeeToolbox.broadcast(player, "ticket.comment.notify", baseComponents);
            return true;
        });
    }
}
