package com.songoda.ultimaterifts.gui;

import com.craftaro.core.gui.CustomizableGui;
import com.craftaro.core.gui.GuiUtils;
import com.craftaro.core.input.ChatPrompt;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.third_party.com.cryptomorin.xseries.XSound;
import com.songoda.ultimaterifts.UltimateRifts;
import com.songoda.ultimaterifts.rift.Rift;
import com.songoda.ultimaterifts.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

public class RiftListGui extends CustomizableGui {
    private final UltimateRifts plugin;
    private final Player player;

    public RiftListGui(UltimateRifts plugin, Player player) {
        super(plugin, "rift_list");
        this.plugin = plugin;
        this.player = player;

        // Set up the GUI
        setTitle("Rift List");
        setRows(6);

        // Populate the GUI with the player's owned rifts
        List<Rift> ownedRifts = plugin.getRiftManager().getAssociatedRifts(player);
        for (int i = 0; i < ownedRifts.size(); i++) {
            Rift rift = ownedRifts.get(i);
            setButton(i, GuiUtils.createButtonItem(
                            XMaterial.matchXMaterial(rift.getLevel().getDoor()),
                            plugin.getLocale().getMessage("interface.list.rift")
                                    .processPlaceholder("id", String.valueOf(rift.getRiftId()))
                                    .getMessage(),
                            plugin.getLocale().getMessage("interface.list.info")
                                    .processPlaceholder("level", String.valueOf(rift.getLevel().getLevel()))
                                    .processPlaceholder("members", String.valueOf(rift.getMembers().size()))
                                    .processPlaceholder("owner", rift.getOwner().getName())
                                    .getMessage().split("\\|")),
                    event -> {
                        if (event.clickType == ClickType.LEFT && rift.hasAccess(player)) {
                            if (player.hasPermission("ultimaterifts.teleport")) {
                                rift.enter(player);
                            } else {
                                plugin.getLocale().getMessage("event.teleport.nopermission").sendPrefixedMessage(player);
                            }
                        } else if (event.clickType == ClickType.RIGHT && rift.getOwner().getUniqueId() == player.getUniqueId()) {
                            if (rift.isOnDoorCooldown(player)) {
                                plugin.getLocale().getMessage("event.break.doorcooldown")
                                        .processPlaceholder("time", rift.getTimeUntilDoorCooldownExpires())
                                        .sendPrefixedMessage(event.player);
                                XSound.ENTITY_VILLAGER_NO.play(event.player);
                                return;
                            }
                            player.closeInventory();
                            ChatPrompt.showPrompt(plugin, player,
                                            plugin.getLocale().getMessage("interface.list.confirmdelete")
                                                    .processPlaceholder("id", String.valueOf(rift.getRiftId()))
                                                    .getMessage(),
                                            event1 -> {
                                                String input = event1.getMessage().replace("#", "");
                                                if (input.equals(String.valueOf(rift.getRiftId()))) {
                                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                                        rift.destroy();
                                                        plugin.getRiftManager().removeRift(rift);
                                                        plugin.getLocale().getMessage("event.destroy.success")
                                                                .processPlaceholder("id", String.valueOf(rift.getRiftId()))
                                                                .sendPrefixedMessage(player);
                                                    });
                                                } else {
                                                    plugin.getLocale().getMessage("event.destroy.invalidid")
                                                            .sendPrefixedMessage(player);
                                                }
                                            })
                                    .setOnClose(() -> plugin.getGuiManager().showGUI(player, new RiftListGui(plugin, player)))
                                    .setOnCancel(() -> plugin.getGuiManager().showGUI(player, new RiftListGui(plugin, player)));
                        }
                    });
        }
    }
}