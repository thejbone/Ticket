package io.github.lxgaming.ticket.bungee.command;

import io.github.lxgaming.ticket.api.Ticket;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.bungee.BungeePlugin;
import io.github.lxgaming.ticket.bungee.util.BungeeToolbox;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.configuration.Configuration;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
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

        if(ticket.getTier() == 1 && !sender.hasPermission("ticket.escalate.tier1")){
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You do not have permission to escalate to tier 2!").color(ChatColor.RED).create());
            return;
        }
        if(ticket.getTier() == 2 && !sender.hasPermission("ticket.escalate.tier2")){
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You do not have permission to escalate to tier 3!").color(ChatColor.RED).create());
            return;
        }

        if (ticket.getTier() >= 3) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Ticket is at the max tier").color(ChatColor.RED).create());
            return;
        }

        ticket.setTier(ticket.getTier()+1);

        if (!TicketImpl.getInstance().getStorage().getQuery().updateTicket(ticket)) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }

        UserData user = DataManager.getOrCreateUser(BungeeToolbox.getUniqueId(sender)).orElse(null);
        if (user == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
            return;
        }

        BungeeToolbox.sendRedisMessage("TicketEscalate", jsonObject -> {
            jsonObject.add("ticket", Configuration.getGson().toJsonTree(ticket));
            jsonObject.addProperty("by", Ticket.getInstance().getPlatform().getUsername(BungeeToolbox.getUniqueId(sender)).orElse("Unknown"));
        });

        BaseComponent[] baseComponents = BungeeToolbox.getTextPrefix()
                .append("Ticket #" + ticket.getId() + " was escalated to tier " + ticket.getTier() + " by ").color(ChatColor.GOLD)
                .append(Ticket.getInstance().getPlatform().getUsername(BungeeToolbox.getUniqueId(sender)).orElse("Unknown")).color(ChatColor.YELLOW)
                .create();

        ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(ticket.getUser());
        if (player != null) {
            player.sendMessage(baseComponents);
        }

        BungeeToolbox.broadcast(player, "ticket.escalate.notify", baseComponents);
    }
}
