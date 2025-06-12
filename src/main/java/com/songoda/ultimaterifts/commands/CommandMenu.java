package com.songoda.ultimaterifts.commands;

import com.songoda.core.commands.AbstractCommand;
import com.songoda.third_party.com.cryptomorin.xseries.XSound;
import com.songoda.ultimaterifts.UltimateRifts;
import com.songoda.ultimaterifts.rift.Rift;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandMenu extends AbstractCommand {
    private final UltimateRifts plugin;

    public CommandMenu(UltimateRifts plugin) {
        super(CommandType.PLAYER_ONLY, "menu");
        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        Player player = (Player) sender;
        Rift rift = plugin.getRiftManager().getRiftByLocation(player.getLocation());
        if (rift == null) {
            plugin.getLocale().getMessage("command.info.notinrift").sendPrefixedMessage(sender);
            XSound.ENTITY_VILLAGER_NO.play(player);
            return ReturnType.FAILURE;
        }

        if (!rift.hasAccess(player)) {
            plugin.getLocale().getMessage("event.interact.notowner").sendPrefixedMessage(player);
            XSound.ENTITY_VILLAGER_NO.play(player);
            return ReturnType.FAILURE;
        }

        // Open the rift menu for the player
        plugin.getGuiManager().showGUI(player, rift.getOverviewGui(player));

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "ultimaterifts.menu";
    }

    @Override
    public String getSyntax() {
        return "menu";
    }

    @Override
    public String getDescription() {
        return "Opens the rift menu for the current rift.";
    }
}