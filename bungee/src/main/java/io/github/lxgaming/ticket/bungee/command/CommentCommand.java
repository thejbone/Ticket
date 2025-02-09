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

import io.github.lxgaming.ticket.api.data.CommentData;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.bungee.BungeePlugin;
import io.github.lxgaming.ticket.bungee.util.ActivityToolbox;
import io.github.lxgaming.ticket.bungee.util.BungeeToolbox;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.configuration.Config;
import io.github.lxgaming.ticket.common.configuration.category.TicketCategory;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class CommentCommand extends AbstractCommand {
    
    public CommentCommand() {
        addAlias("comment");
        addAlias("comments");
        setDescription("Adds a comment to requested ticket");
        setPermission("ticket.comment.base");
        setUsage("<Id> <Message>");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSender sender = (CommandSender) object;
        if (arguments.isEmpty()) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Invalid arguments: " + getUsage()).color(ChatColor.RED).create());
            return;
        }
        
        Integer ticketId = Toolbox.parseInteger(StringUtils.removeStart(arguments.remove(0), "#")).orElse(null);
        if (ticketId == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Failed to parse ticket id").color(ChatColor.RED).create());
            return;
        }
        
        String message = Toolbox.convertColor(String.join(" ", arguments));
        if (StringUtils.isBlank(message)) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Message cannot be blank").color(ChatColor.RED).create());
            return;
        }
        
        // Minecraft chat character limit
        // https://wiki.vg/Protocol#Chat_Message_.28serverbound.29
        if (message.length() > 256) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Message length may not exceed 256").color(ChatColor.RED).create());
            return;
        }
        
        UserData user = DataManager.getOrCreateUser(BungeeToolbox.getUniqueId(sender)).orElse(null);
        if (user == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }
        
        if (user.isBanned()) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You have been banned").color(ChatColor.RED).create());
            return;
        }
        
        TicketData ticket = DataManager.getTicket(ticketId).orElse(null);
        if (ticket == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Ticket doesn't exist").color(ChatColor.RED).create());
            return;
        }
        
        if (!user.getUniqueId().equals(ticket.getUser()) && !sender.hasPermission("ticket.comment.others")) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You are not the owner of that ticket").color(ChatColor.RED).create());
            return;
        }

        if(ticket.getTier() == 1 && !sender.hasPermission("ticket.comment.tier1")){
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You do not have permission to comment on tier 1!").color(ChatColor.RED).create());
            return;
        }
        if(ticket.getTier() == 1 && !sender.hasPermission("ticket.comment.tier2")){
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You do not have permission to comment on tier 2!").color(ChatColor.RED).create());
            return;
        }
        if(ticket.getTier() == 2 && !sender.hasPermission("ticket.comment.tier3")){
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You do not have permission to comment on tier 3!").color(ChatColor.RED).create());
            return;
        }

        if (!sender.hasPermission("ticket.comment.exempt.cooldown")) {
            long time = System.currentTimeMillis() - TicketImpl.getInstance().getConfig().map(Config::getTicket).map(TicketCategory::getCommentDelay).orElse(0L);
            for (CommentData comment : ticket.getComments()) {
                long duration = comment.getTimestamp().minusMillis(time).toEpochMilli();
                if (duration > 0) {
                    sender.sendMessage(BungeeToolbox.getTextPrefix().append("You need to wait " + (duration / 1000) + " seconds before adding another comment").color(ChatColor.RED).create());
                    return;
                }
            }
        }

        ActivityToolbox.sendComment(ticket, user, message, true).whenComplete((result,ex) -> {
           if(result == null || !result){
               BungeePlugin.getInstance().getLogger().info("Failed to send comment!!!");
           } else if (ex != null){
               ex.printStackTrace();
           }
        });
    }
}