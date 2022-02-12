package io.github.lxgaming.ticket.bungee.util;

import io.github.lxgaming.ticket.api.Ticket;
import io.github.lxgaming.ticket.api.data.CommentData;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.api.util.Reference;
import io.github.lxgaming.ticket.bungee.BungeePlugin;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.configuration.Configuration;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
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
            DataManager.getUserCache().put(user.getUniqueId(), user);
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
    public static CompletableFuture<Boolean> sendClosedReopen(TicketData ticket, UserData user, int status, boolean sendDiscordMessage) {
        return CompletableFuture.supplyAsync(() -> {
            ticket.setStatus(status);
            ticket.setRead(false);
            if (!TicketImpl.getInstance().getStorage().getQuery().updateTicket(ticket)) {
                return false;
            }

            DataManager.getUserCache().put(user.getUniqueId(), user);

            if(status == 0){
                BungeeToolbox.sendRedisMessage("TicketClose", jsonObject -> {
                    jsonObject.add("ticket", Configuration.getGson().toJsonTree(ticket));
                    jsonObject.add("user", Configuration.getGson().toJsonTree(user));
                });

                BaseComponent[] baseComponents = BungeeToolbox.getTextPrefix()
                        .append("Ticket #" + ticket.getId() + " was closed by ").color(ChatColor.GOLD)
                        .append(user.getName()).color(ChatColor.YELLOW).create();

                String command = "/" + Reference.ID + " read " + ticket.getId();

                // Forces the expiry to be recalculated
                DataManager.getCachedTicket(ticket.getId());
                ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(ticket.getUser());
                if (player != null) {
                    player.sendMessage(baseComponents);
                    player.sendMessage(BungeeToolbox.getTextPrefix()
                            .append("Use ").color(ChatColor.GOLD)
                            .append(command).color(ChatColor.GREEN).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                            .append(" to view your ticket").color(ChatColor.GOLD).create());
                }

                BungeePlugin.getInstance().getDiscordToolbox().closeTicket(ticket);

                BungeeToolbox.broadcast(player, "ticket.close.notify", baseComponents);
            } else {
                BungeeToolbox.sendRedisMessage("TicketReopen", jsonObject -> {
                    jsonObject.add("ticket", Configuration.getGson().toJsonTree(ticket));
                    jsonObject.addProperty("by", (user.getName()));
                });

                BaseComponent[] baseComponents = BungeeToolbox.getTextPrefix()
                        .append("Ticket #" + ticket.getId() + " was reopened by ").color(ChatColor.GOLD)
                        .append((user.getName())).color(ChatColor.YELLOW)
                        .create();

                ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(ticket.getUser());
                if (player != null) {
                    player.sendMessage(baseComponents);
                }
                BungeePlugin.getInstance().getDiscordToolbox().sendTicketData(ticket, false);

                BungeeToolbox.broadcast(player, "ticket.reopen.notify", baseComponents);
            }

            return true;
        });
    }

    public static CompletableFuture<Boolean> sendEscalate(TicketData ticket, UserData user, boolean sendDiscordMessage) {
        return CompletableFuture.supplyAsync(() -> {
            ticket.setTier(ticket.getTier()+1);
            if (!TicketImpl.getInstance().getStorage().getQuery().updateTicket(ticket)){
                return false;
            }

            BungeeToolbox.sendRedisMessage("TicketEscalate", jsonObject -> {
                jsonObject.add("ticket", Configuration.getGson().toJsonTree(ticket));
                jsonObject.addProperty("by ", user.getName());
            });

            BaseComponent[] baseComponents = BungeeToolbox.getTextPrefix()
                    .append("Ticket #" + ticket.getId() + " was escalated to tier " + ticket.getTier() + " by ").color(ChatColor.GOLD)
                    .append(user.getName()).color(ChatColor.YELLOW)
                    .create();

            ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(ticket.getUser());
            if (player != null) {
                player.sendMessage(baseComponents);
            }
            BungeePlugin.getInstance().getDiscordToolbox().sendTicketData(ticket,true).whenComplete((result, exception) -> {
                if(exception != null || result == null){
                    exception.printStackTrace();
                } else {
                    ticket.setDiscordMsgId(result);
                    if (!TicketImpl.getInstance().getStorage().getQuery().updateTicket(ticket)) {
                        return;
                    }
                }
            });

            return true;
        });
    }
}
