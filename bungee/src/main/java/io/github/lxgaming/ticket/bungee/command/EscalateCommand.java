package io.github.lxgaming.ticket.bungee.command;

import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.bungee.util.ActivityToolbox;
import io.github.lxgaming.ticket.bungee.util.BungeeToolbox;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class EscalateCommand extends AbstractCommand {

    public EscalateCommand() {
        addAlias("escalate");
        addAlias("raise");
        addAlias("elevate");
        setDescription("Escalate the ticket to the next tier");
        setPermission("ticket.escalate.base");
        setUsage("<Id>");
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

        TicketData ticket = DataManager.getTicket(ticketId).orElse(null);
        if (ticket == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Ticket doesn't exist").color(ChatColor.RED).create());
            return;
        }

        if(ticket.getTier() == 1 && !sender.hasPermission("ticket.escalate.tier2")){
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You do not have permission to escalate to tier 2!").color(ChatColor.RED).create());
            return;
        }
        if(ticket.getTier() == 2 && !sender.hasPermission("ticket.escalate.tier3")){
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You do not have permission to escalate to tier 3!").color(ChatColor.RED).create());
            return;
        }

        if (ticket.getTier() >= 3) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Ticket is at the max tier").color(ChatColor.RED).create());
            return;
        }

        UserData user = DataManager.getOrCreateUser(BungeeToolbox.getUniqueId(sender)).orElse(null);
        if (user == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }

        ActivityToolbox.sendEscalate(ticket, user);
    }
}
