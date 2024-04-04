package com.songoda.ultimaterifts.commands;

import com.craftaro.core.commands.AbstractCommand;
import com.songoda.ultimaterifts.UltimateRifts;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CommandReload extends AbstractCommand {
    private final UltimateRifts plugin;

    public CommandReload(UltimateRifts plugin) {
        super(CommandType.PLAYER_ONLY, "reload");
        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        this.plugin.reloadConfig();
        this.plugin.getLocale().getMessage("&7Configuration and Language files reloaded.").sendPrefixedMessage(sender);
        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "ultimaterifts.admin";
    }

    @Override
    public String getSyntax() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reload the Configuration and Language files.";
    }
}
