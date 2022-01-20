/*
 * Copyright 2018 Alex Thomson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.lxgaming.ticket.bungee.command;

import io.github.lxgaming.ticket.api.Ticket;
import io.github.lxgaming.ticket.api.data.CommentData;
import io.github.lxgaming.ticket.api.data.SortByTicketTier;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.api.util.Reference;
import io.github.lxgaming.ticket.bungee.util.BungeeToolbox;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.configuration.Config;
import io.github.lxgaming.ticket.common.configuration.category.TicketCategory;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.protocol.packet.Chat;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ReadCommand extends AbstractCommand {
    
    public ReadCommand() {
        addAlias("read");
        addAlias("check");
        addAlias("list");
        setDescription("Lists Open, Unread tickets or provides details of a specific ticket");
        setPermission("ticket.read.base");
        setUsage("[Id]");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSender sender = (CommandSender) object;
        if (arguments.isEmpty()) {
            Collection<TicketData> openTickets = DataManager.getCachedOpenTickets();
            openTickets.removeIf(ticket -> {
                if(!BungeeToolbox.getUniqueId(sender).equals(ticket.getUser())){
                    if(ticket.getTier() == 1)
                        return !sender.hasPermission("ticket.read.others.tier1");
                    if(ticket.getTier() == 2)
                        return !sender.hasPermission("ticket.read.others.tier2");
                    if(ticket.getTier() == 3)
                        return !sender.hasPermission("ticket.read.others.tier3");
                }
                return false;
            });

            List<TicketData> sortedOpenList = openTickets.stream().sorted(new SortByTicketTier()).collect(Collectors.toList());

            if (!sortedOpenList.isEmpty()) {

                sender.sendMessage(new ComponentBuilder("")
                        .append("----------").color(ChatColor.GREEN).strikethrough(true)
                        .append(" " + openTickets.size()).color(ChatColor.YELLOW).strikethrough(false)
                        .append(" Open " + Toolbox.formatUnit(sortedOpenList.size(), "Ticket", "Tickets") + " ").color(ChatColor.GREEN)
                        .append("----------").color(ChatColor.GREEN).strikethrough(true)
                        .create());

                sortedOpenList.forEach(ticket -> sender.sendMessage(buildTicket(ticket)));
            }
            
            Collection<TicketData> unreadTickets = DataManager.getCachedUnreadTickets(BungeeToolbox.getUniqueId(sender));
            List<TicketData> sortedUnreadTickets = unreadTickets.stream().sorted(new SortByTicketTier()).collect(Collectors.toList());

            if (!sortedUnreadTickets.isEmpty()) {
                sender.sendMessage(new ComponentBuilder("")
                        .append("----------").color(ChatColor.GREEN).strikethrough(true)
                        .append(" " + unreadTickets.size()).color(ChatColor.YELLOW).strikethrough(false)
                        .append(" Unread " + Toolbox.formatUnit(sortedUnreadTickets.size(), "Ticket", "Tickets") + " ").color(ChatColor.GREEN)
                        .append("----------").color(ChatColor.GREEN).strikethrough(true)
                        .create());

                sortedUnreadTickets.forEach(ticket -> sender.sendMessage(buildTicket(ticket)));
            }
            
            if (openTickets.isEmpty() && unreadTickets.isEmpty()) {
                sender.sendMessage(BungeeToolbox.getTextPrefix().append("There are no open tickets").color(ChatColor.YELLOW).create());
            }
            
            return;
        }
        
        Integer ticketId = Toolbox.parseInteger(StringUtils.removeStart(arguments.remove(0), "#")).orElse(null);
        if (ticketId == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Failed to parse ticket id").color(ChatColor.RED).create());
            return;
        }
        
        TicketData ticket = DataManager.getTicket(ticketId).orElse(null);
        if (ticket == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Failed to find ticket").color(ChatColor.RED).create());
            return;
        }
        
        boolean owner = BungeeToolbox.getUniqueId(sender).equals(ticket.getUser());
        if (!owner && !sender.hasPermission("ticket.read.others")) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You are not the owner of that ticket").color(ChatColor.RED).create());
            return;
        }
        
        if (owner && !ticket.isRead()) {
            ticket.setRead(true);
            if (!TicketImpl.getInstance().getStorage().getQuery().updateTicket(ticket)) {
                sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
                return;
            }
        }

        ChatColor tierColor = ChatColor.BLUE;
        if(ticket.getTier() == 1)
            tierColor = ChatColor.GREEN;
        if(ticket.getTier() == 2)
            tierColor = ChatColor.AQUA;
        if(ticket.getTier() == 3)
            tierColor = ChatColor.RED;
        ComponentBuilder headerData = new ComponentBuilder("");
        headerData.append("----------").color(ChatColor.GREEN).strikethrough(true);
        headerData.append(" Ticket #" + ticket.getId() + " ").color(ChatColor.YELLOW).strikethrough(false).append("- Tier " + ticket.getTier() + " ").color(tierColor).strikethrough(false);
        headerData.append("----------").color(ChatColor.GREEN).strikethrough(true);
        headerData.append("\n", ComponentBuilder.FormatRetention.NONE);

        headerData.append("Time").color(ChatColor.AQUA).append(": ").color(ChatColor.WHITE);
        headerData.append(TicketImpl.getInstance().getConfig().map(Config::getTicket).map(TicketCategory::getDateFormat).flatMap(pattern -> Toolbox.formatInstant(pattern, ticket.getTimestamp())).orElse("Unknown"));

        ComponentBuilder statusData = new ComponentBuilder("");
        statusData.append("Status").color(ChatColor.AQUA).append(": ").color(ChatColor.WHITE);
        if (ticket.getStatus() == 0) {
            TextComponent openMessage = new TextComponent(ChatColor.GREEN + "Open");
            openMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket close " + ticket.getId()));
            openMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to close!").color(ChatColor.RED).create()));
            statusData.append(openMessage);
        } else if (ticket.getStatus() == 1) {
            TextComponent closeMessage = new TextComponent(ChatColor.RED + "Closed");
            closeMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket reopen " + ticket.getId()));
            closeMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to open!").color(ChatColor.GREEN).create()));
            statusData.append(closeMessage);
        }

        ComponentBuilder userData = new ComponentBuilder("");
        userData.append("User").color(ChatColor.AQUA).append(": ").color(ChatColor.WHITE);
        UserData user = DataManager.getUser(ticket.getUser()).orElse(null);
        if (user != null) {
            if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                userData.append(user.getName()).color(ChatColor.GREEN);
            } else {
                userData.append(user.getName()).color(ChatColor.RED);
            }
        } else {
            userData.append("Unknown").color(ChatColor.WHITE);
        }

        ComponentBuilder locationData = new ComponentBuilder("");
        locationData.append("Server").color(ChatColor.AQUA).append(": ").color(ChatColor.WHITE);
        
        if (ticket.getLocation().getX() != null && ticket.getLocation().getY() != null && ticket.getLocation().getZ() != null) {
            locationData.append("" + Toolbox.formatDecimal(ticket.getLocation().getX(), 3)).color(ChatColor.WHITE).append(", ").color(ChatColor.GRAY);
            locationData.append("" + Toolbox.formatDecimal(ticket.getLocation().getY(), 3)).color(ChatColor.WHITE).append(", ").color(ChatColor.GRAY);
            locationData.append("" + Toolbox.formatDecimal(ticket.getLocation().getZ(), 3)).color(ChatColor.WHITE).append(" @ ").color(ChatColor.GRAY);
        }

        locationData.append(StringUtils.defaultIfBlank(ticket.getLocation().getServer(), "Unknown")).color(ChatColor.WHITE);
        if (ticket.getLocation().getDimension() != null) {
            locationData.append(" (").color(ChatColor.GRAY).append("" + ticket.getLocation().getDimension()).color(ChatColor.WHITE).append(")").color(ChatColor.GRAY);
        }

        ComponentBuilder messageData = new ComponentBuilder("");
        messageData.append("Message").color(ChatColor.AQUA).append(": " + ticket.getText()).color(ChatColor.WHITE);

        if (!ticket.getComments().isEmpty()) {
            sender.sendMessage(headerData.create());
            sender.sendMessage(statusData.create());
            sender.sendMessage(userData.create());
            sender.sendMessage(locationData.create());
            messageData.append("\n");
            messageData.append("======= Comments ========").color(ChatColor.AQUA);
            sender.sendMessage(messageData.create());
            ticket.getComments().forEach(comment -> sender.sendMessage(buildComment(comment)));
        } else {
            sender.sendMessage(headerData.create());
            sender.sendMessage(statusData.create());
            sender.sendMessage(userData.create());
            sender.sendMessage(locationData.create());
            sender.sendMessage(messageData.create());
        }
        TextComponent replyButton = new TextComponent(ChatColor.GREEN + "[Reply]");
        replyButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ticket comment " + ticket.getId() + " "));
        replyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to reply!").color(ChatColor.GREEN).create()));
        TextComponent escalateButton = new TextComponent(ChatColor.RED + "[Escalate]");
        escalateButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket escalate " + ticket.getId() + " " + ticket.getTier()+1 ));
        escalateButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to escalate!").color(ChatColor.RED).create()));
        sender.sendMessage(replyButton);
        sender.sendMessage(escalateButton);
    }
    
    private BaseComponent[] buildComment(CommentData comment) {
        ComponentBuilder componentBuilder = new ComponentBuilder("");
        componentBuilder.append(Toolbox.getShortTimeString(System.currentTimeMillis() - comment.getTimestamp().toEpochMilli())).color(ChatColor.GREEN);
        componentBuilder.append(" by ").color(ChatColor.GOLD);
        
        UserData user = DataManager.getUser(comment.getUser()).orElse(null);
        if (user != null) {
            if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                componentBuilder.append(user.getName()).color(ChatColor.GREEN);
            } else {
                componentBuilder.append(user.getName()).color(ChatColor.RED);
            }
        } else {
            componentBuilder.append("Unknown").color(ChatColor.WHITE);
        }
        
        componentBuilder.append(" - ").color(ChatColor.GOLD);
        componentBuilder.append(comment.getText()).color(ChatColor.GRAY);
        return componentBuilder.create();
    }
    
    private BaseComponent[] buildTicket(TicketData ticket) {
        ChatColor tierColor = ChatColor.BLUE;
        if(ticket.getTier() == 1)
            tierColor = ChatColor.GREEN;
        if(ticket.getTier() == 2)
            tierColor = ChatColor.AQUA;
        if(ticket.getTier() == 3)
            tierColor = ChatColor.RED;
        ComponentBuilder componentBuilder = new ComponentBuilder("")
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + Reference.ID + " read " + ticket.getId()))
                .append("Tier " + ticket.getTier()).color(tierColor)
                .append(" #" + ticket.getId()).color(ChatColor.GOLD)
                .append(" " + Toolbox.getShortTimeString(System.currentTimeMillis() - ticket.getTimestamp().toEpochMilli())).color(ChatColor.GREEN)
                .append(" by ").color(ChatColor.GOLD);
        
        UserData user = DataManager.getCachedUser(ticket.getUser()).orElse(null);
        if (user != null) {
            if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                componentBuilder.append(user.getName()).color(ChatColor.GREEN);
            } else {
                componentBuilder.append(user.getName()).color(ChatColor.RED);
            }
        } else {
            componentBuilder.append("Unknown").color(ChatColor.WHITE);
        }
        
        componentBuilder.append(" - ").color(ChatColor.GOLD);
        componentBuilder.append(Toolbox.substring(ticket.getText(), 20)).color(ChatColor.GRAY);
        return componentBuilder.create();
    }
}