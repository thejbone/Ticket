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

import com.google.common.collect.Lists;
import io.github.lxgaming.ticket.api.Ticket;
import io.github.lxgaming.ticket.api.util.Reference;
import io.github.lxgaming.ticket.bungee.util.BungeeToolbox;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.manager.CommandManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class TicketCommand extends Command {
    
    public TicketCommand() {
        super("ticket", "", "tickets");
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        List<String> arguments = Lists.newArrayList(args);
        AbstractCommand command = CommandManager.getChildCommand(arguments).orElse(null);
        if (command == null) {
            ComponentBuilder componentBuilder = new ComponentBuilder("");
            componentBuilder.append("Use ").color(ChatColor.BLUE);
            componentBuilder.append("/" + Reference.ID + " open ").color(ChatColor.GREEN);
            componentBuilder.append("to open a ticket!").color(ChatColor.BLUE);
            sender.sendMessage(componentBuilder.create());
            return;
        }
        
        if (StringUtils.isBlank(command.getPermission()) || !sender.hasPermission(command.getPermission())) {
            sender.sendMessage(new ComponentBuilder("You do not have permission to execute this command!").color(ChatColor.RED).create());
            return;
        }
        
        Ticket.getInstance().getLogger().debug("Processing {}", command.getPrimaryAlias().orElse("Unknown"));
        
        try {
            command.execute(sender, arguments);
        } catch (Throwable throwable) {
            Ticket.getInstance().getLogger().error("Encountered an error while executing {}", command.getClass().getSimpleName(), throwable);
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
        }
    }
}