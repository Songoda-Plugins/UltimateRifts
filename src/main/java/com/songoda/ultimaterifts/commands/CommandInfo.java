package com.songoda.ultimaterifts.commands;

import com.craftaro.core.commands.AbstractCommand;
import com.songoda.ultimaterifts.UltimateRifts;
import com.songoda.ultimaterifts.rift.Rift;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandInfo extends AbstractCommand {
    private final UltimateRifts plugin;

    public CommandInfo(UltimateRifts plugin) {
        super(CommandType.PLAYER_ONLY, "info");
        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        Player player = (Player) sender;
        Rift rift = plugin.getRiftManager().getRiftByLocation(player.getLocation());
        if (rift == null) {
            plugin.getLocale().newMessage("&cYou are not standing in a rift rift.").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }

        player.sendMessage(ChatColor.YELLOW + "Rift Information:");
        player.sendMessage(ChatColor.YELLOW + "Rift ID: " + ChatColor.WHITE + rift.getRiftId());
        player.sendMessage(ChatColor.YELLOW + "Level: " + ChatColor.WHITE + rift.getLevel().getLevel());
        player.sendMessage(ChatColor.YELLOW + "Owner: " + ChatColor.WHITE + (rift.getOwner() != null ? rift.getOwner().getName() : "None"));

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "ultimaterifts.info";
    }

    @Override
    public String getSyntax() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Shows information about the current rift rift.";
    }
}