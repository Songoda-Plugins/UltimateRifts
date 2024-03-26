package com.songoda.ultimaterifts.commands;

import com.craftaro.core.commands.AbstractCommand;
import com.songoda.ultimaterifts.UltimateRifts;
import com.songoda.ultimaterifts.gui.RiftListGui;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandTele extends AbstractCommand {
    private final UltimateRifts plugin;

    public CommandTele(UltimateRifts plugin) {
        super(CommandType.PLAYER_ONLY, "list");
        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        Player player = (Player) sender;
        plugin.getGuiManager().showGUI(player, new RiftListGui(plugin, player));
        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "ultimaterifts.list";
    }

    @Override
    public String getSyntax() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Open the rift list GUI to view owned rifts.";
    }
}