package com.songoda.ultimaterifts.commands;

import com.songoda.core.commands.AbstractCommand;
import com.songoda.ultimaterifts.UltimateRifts;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandPaste extends AbstractCommand {
    private final UltimateRifts plugin;

    public CommandPaste(UltimateRifts plugin) {
        super(CommandType.PLAYER_ONLY, "paste");
        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        Player player = (Player) sender;

        if (args.length != 1) {
            plugin.getLocale().newMessage("&cUsage: /rift paste <level>").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }

        int level;
        try {
            level = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            plugin.getLocale().newMessage("&cInvalid level. Please enter a valid number.").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }

        Location location = player.getLocation();
        if (plugin.getSchematicManager().pasteSchematic(level, location)) {
            plugin.getLocale().newMessage("&aSchematic pasted for level " + level).sendPrefixedMessage(sender);
            return ReturnType.SUCCESS;
        } else {
            plugin.getLocale().newMessage("&cFailed to paste schematic for level " + level).sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "ultimaterifts.paste";
    }

    @Override
    public String getSyntax() {
        return "paste <level>";
    }

    @Override
    public String getDescription() {
        return "Pastes the schematic for the specified level at your current location.";
    }
}