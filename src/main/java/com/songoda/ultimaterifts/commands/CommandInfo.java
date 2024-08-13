package com.songoda.ultimaterifts.commands;

import com.craftaro.core.commands.AbstractCommand;
import com.songoda.ultimaterifts.UltimateRifts;
import com.songoda.ultimaterifts.rift.Member;
import com.songoda.ultimaterifts.rift.Rift;
import org.bukkit.Bukkit;
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
            plugin.getLocale().getMessage("command.info.notinrift").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }

        player.sendMessage(plugin.getLocale().getMessage("command.info.title").toText());
        player.sendMessage(plugin.getLocale().getMessage("command.info.id").processPlaceholder("id", rift.getRiftId()).toText());
        player.sendMessage(plugin.getLocale().getMessage("command.info.level").processPlaceholder("level", rift.getLevel().getLevel()).toText());
        player.sendMessage(plugin.getLocale().getMessage("command.info.members").processPlaceholder("members", rift.getMembers().size()).toText());

        for (Member member : rift.getMembers().values()) {
            String ownerSuffix = member.isOwner() ? plugin.getLocale().getMessage("command.info.ownersuffix").toText() : "";
            player.sendMessage(plugin.getLocale().getMessage("command.info.memberformat")
                    .processPlaceholder("player", Bukkit.getOfflinePlayer(member.getPlayerId()).getName())
                    .processPlaceholder("owner", ownerSuffix)
                    .toText());
        }

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