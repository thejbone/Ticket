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

import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.bungee.util.ActivityToolbox;
import io.github.lxgaming.ticket.bungee.util.BungeeToolbox;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class CloseCommand extends AbstractCommand {
    
    public CloseCommand() {
        addAlias("close");
        setDescription("Closes the requested ticket");
        setPermission("ticket.close.base");
        setUsage("<Id> [Message]");
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
        
        TicketData ticket = DataManager.getCachedTicket(ticketId).orElse(null);
        if (ticket == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Ticket never existed or it's no longer cached").color(ChatColor.RED).create());
            return;
        }
        
        if (!BungeeToolbox.getUniqueId(sender).equals(ticket.getUser()) && !sender.hasPermission("ticket.close.others")) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You are not the owner of that ticket").color(ChatColor.RED).create());
            return;
        }

        if(ticket.getTier() == 1 && !sender.hasPermission("ticket.close.tier1")){
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You do not have permission to close tier 1!").color(ChatColor.RED).create());
            return;
        }
        if(ticket.getTier() == 2 && !sender.hasPermission("ticket.close.tier2")){
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You do not have permission to close tier 2!").color(ChatColor.RED).create());
            return;
        }
        if(ticket.getTier() == 3 && !sender.hasPermission("ticket.close.tier3")){
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You do not have permission to close tier 3!").color(ChatColor.RED).create());
            return;
        }
        if (ticket.getStatus() == 1) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Ticket is already closed").color(ChatColor.RED).create());
            return;
        }
        
        ticket.setStatus(1);
        ticket.setRead(false);
        if (!TicketImpl.getInstance().getStorage().getQuery().updateTicket(ticket)) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }
        
        UserData user = DataManager.getOrCreateUser(BungeeToolbox.getUniqueId(sender)).orElse(null);
        if (user == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }

        String message = Toolbox.convertColor(String.join(" ", arguments));
        if (message.length() > 256) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Message length may not exceed 256").color(ChatColor.RED).create());
            return;
        }
        DataManager.getCachedTicket(ticketId);
        if(!message.isEmpty())
            ActivityToolbox.sendComment(ticket, user, message, true);
        ActivityToolbox.sendClosedReopen(ticket, user, 1, true);
    }
}