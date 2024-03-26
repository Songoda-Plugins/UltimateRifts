package com.songoda.ultimaterifts.commands;

import com.craftaro.core.commands.AbstractCommand;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.songoda.ultimaterifts.UltimateRifts;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandSave extends AbstractCommand {
    private final UltimateRifts plugin;

    public CommandSave(UltimateRifts plugin) {
        super(CommandType.PLAYER_ONLY, "save");
        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        Player player = (Player) sender;

        if (args.length != 1) {
            plugin.getLocale().newMessage("&cUsage: /rift save <level>").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }

        int level;
        try {
            level = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            plugin.getLocale().newMessage("&cInvalid level. Please enter a valid number.").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }

        Region region;
        try {
            region = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getSelection();
        } catch (IncompleteRegionException e) {
            plugin.getLocale().newMessage("&cPlease make a valid WorldEdit selection first.").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }

        World world = BukkitAdapter.adapt(player.getWorld());


        plugin.getSchematicManager().saveSchematic(region, world, level);
        plugin.getLocale().newMessage("&aSchematic saved for level " + level).sendPrefixedMessage(sender);
        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "ultimaterifts.save";
    }

    @Override
    public String getSyntax() {
        return "save <level>";
    }

    @Override
    public String getDescription() {
        return "Saves the current WorldEdit selection as a schematic for the specified level.";
    }
}