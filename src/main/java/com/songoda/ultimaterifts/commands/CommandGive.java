package com.songoda.ultimaterifts.commands;

import com.craftaro.core.commands.AbstractCommand;
import com.songoda.ultimaterifts.UltimateRifts;
import com.songoda.ultimaterifts.rift.levels.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public class CommandGive extends AbstractCommand {
    private final UltimateRifts plugin;

    public CommandGive(UltimateRifts plugin) {
        super(CommandType.CONSOLE_OK, "give");
        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        if (args.length < 1 || args.length > 3) {
            return ReturnType.SYNTAX_ERROR;
        }

        Level level = this.plugin.getLevelManager().getLowestLevel();
        Player player;

        if ((args.length == 2 || args.length == 3) && Bukkit.getPlayer(args[0]) == null) {
            this.plugin.getLocale().getMessage("command.give.playernotfound").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        } else if (args.length == 1 || args.length == 2) {
            if (!(sender instanceof Player)) {
                this.plugin.getLocale().getMessage("command.give.notaplayer").sendPrefixedMessage(sender);
                return ReturnType.FAILURE;
            }
            player = (Player) sender;
        } else {
            player = Bukkit.getPlayer(args[0]);
        }

        if ((args.length == 2 || args.length == 3)) {
            try {
                int levelId = Integer.parseInt(args[args.length - 1]);
                if (!this.plugin.getLevelManager().isLevel(levelId)) {
                    this.plugin.getLocale().getMessage("command.give.invalidlevel")
                            .processPlaceholder("lowestlevel", this.plugin.getLevelManager().getLowestLevel().getLevel())
                            .processPlaceholder("highestlevel", this.plugin.getLevelManager().getHighestLevel().getLevel())
                            .sendPrefixedMessage(sender);
                    return ReturnType.FAILURE;
                }
                level = this.plugin.getLevelManager().getLevel(levelId);
            } catch (NumberFormatException e) {
                this.plugin.getLocale().getMessage("command.give.invalidlevel")
                        .processPlaceholder("lowestlevel", this.plugin.getLevelManager().getLowestLevel().getLevel())
                        .processPlaceholder("highestlevel", this.plugin.getLevelManager().getHighestLevel().getLevel())
                        .sendPrefixedMessage(sender);
                return ReturnType.FAILURE;
            }
        }

        int riftId = -1;
        if (args.length == 3) {
            try {
                riftId = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                this.plugin.getLocale().getMessage("command.give.invalidriftid").sendPrefixedMessage(sender);
                return ReturnType.FAILURE;
            }
        }

        ItemStack riftItem = plugin.createLeveledRift(riftId, level.getLevel());

        if (player.getInventory().firstEmpty() == -1) {
            // Inventory is full, drop the item on the ground
            player.getWorld().dropItem(player.getLocation(), riftItem);
            this.plugin.getLocale().getMessage("command.give.successdropped")
                    .processPlaceholder("level", level.getLevel()).sendPrefixedMessage(sender);
        } else {
            // Add the item to the player's inventory
            player.getInventory().addItem(riftItem);
            this.plugin.getLocale().getMessage("command.give.success")
                    .processPlaceholder("level", level.getLevel()).sendPrefixedMessage(sender);
        }
        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "ultimaterifts.admin.give";
    }

    @Override
    public String getSyntax() {
        return "give [player] [riftId] <level>";
    }

    @Override
    public String getDescription() {
        return "Give a leveled rift to a player.";
    }
}