package com.songoda.ultimaterifts.gui;

import com.songoda.core.gui.CustomizableGui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.gui.methods.Clickable;
import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.ultimaterifts.UltimateRifts;
import com.songoda.ultimaterifts.rift.Rift;
import com.songoda.ultimaterifts.settings.Settings;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class CustomizeGui extends CustomizableGui {
    private final UltimateRifts plugin;
    private final Rift rift;

    public CustomizeGui(UltimateRifts plugin, Rift rift) {
        super(plugin, "customize");
        this.rift = rift;
        this.plugin = plugin;
        this.setRows(6);
        this.setTitle(plugin.getLocale().getMessage("interface.customize.title").getMessage());

        ItemStack black = GuiUtils.getBorderItem(XMaterial.BLACK_STAINED_GLASS_PANE);

        // edges will be type 3
        setDefaultItem(black);

        ItemStack glass1 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_1.getMaterial());
        ItemStack glass2 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_2.getMaterial());
        ItemStack glass3 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_3.getMaterial());

        mirrorFill("mirrorfill_1", 0, 0, true, true, glass2);
        mirrorFill("mirrorfill_2", 0, 1, false, true, glass2);
        mirrorFill("mirrorfill_3", 0, 2, false, true, glass3);
        mirrorFill("mirrorfill_4", 0, 3, false, true, glass3);

        mirrorFill("mirrorfill_5", 1, 0, true, true, glass2);

        mirrorFill("mirrorfill_6", 2, 0, true, true, glass3);

        setItem(4, GuiUtils.createButtonItem(XMaterial.OAK_SIGN,
                plugin.getLocale().getMessage("interface.customize.info").getMessage(),
                plugin.getLocale().getMessage("interface.customize.infolore").getMessage()));

        setUnlockedRange(10, 12);
        setUnlockedRange(19, 21);
        setUnlockedRange(28, 30);

        setUnlockedRange(14, 16);
        setUnlockedRange(23, 25);
        setUnlockedRange(32, 34);

        ItemStack green = GuiUtils.getBorderItem(XMaterial.GREEN_STAINED_GLASS_PANE);
        ItemStack red = GuiUtils.getBorderItem(XMaterial.RED_STAINED_GLASS_PANE);

        Clickable click = (event) -> {
            ItemStack[] wallpaperItems = new ItemStack[9];
            int slot = 0;
            for (int i = 10; i <= 12; i++)
                wallpaperItems[slot++] = getItem(i);
            for (int i = 19; i <= 21; i++)
                wallpaperItems[slot++] = getItem(i);
            for (int i = 28; i <= 30; i++)
                wallpaperItems[slot++] = getItem(i);

            boolean updated = false;

            if (Arrays.stream(wallpaperItems).anyMatch(Objects::nonNull)) {
                updated = true;
                slot = 0;
                Material[] wallpaper = new Material[9];
                for (ItemStack item : wallpaperItems) {
                    if (item == null) {
                        this.plugin.getLocale().getMessage("event.wallpaper.air").sendPrefixedMessage(event.player);
                        return;
                    }
                    if (!item.getType().isBlock()) {
                        this.plugin.getLocale().getMessage("event.wallpaper.onlyblocks").sendPrefixedMessage(event.player);
                        return;
                    }

                    wallpaper[slot++] = item.getType();
                }

                rift.setWallpaper(wallpaper);
                rift.save("wallpaper");
            }

            ItemStack[] floorItems = new ItemStack[9];
            slot = 0;
            for (int i = 14; i <= 16; i++)
                floorItems[slot++] = getItem(i);
            for (int i = 23; i <= 25; i++)
                floorItems[slot++] = getItem(i);
            for (int i = 32; i <= 34; i++)
                floorItems[slot++] = getItem(i);

            if (Arrays.stream(floorItems).anyMatch(Objects::nonNull)) {
                updated = true;
                slot = 0;
                Material[] floor = new Material[9];
                for (ItemStack item : floorItems) {
                    if (item == null) {
                        this.plugin.getLocale().getMessage("event.floor.air").sendPrefixedMessage(event.player);
                        return;
                    }
                    if (!item.getType().isBlock()) {
                        this.plugin.getLocale().getMessage("event.floor.onlyblocks").sendPrefixedMessage(event.player);
                        return;
                    }

                    floor[slot++] = item.getType();
                }

                rift.setFloor(floor);
                rift.save("floor");
            }

            // Paint the floor and walls if updated
            if (updated)
                rift.updateWalls();

            guiManager.showGUI(event.player, rift.getOverviewGui(event.player));
        };

        Clickable redClick = (event) -> {
            guiManager.showGUI(event.player, rift.getOverviewGui(event.player));
        };

        setItem("wallpaper", 4, 2, GuiUtils.createButtonItem(XMaterial.BRICKS,
                plugin.getLocale().getMessage("interface.customize.wallpaper").getMessage(),
                plugin.getLocale().getMessage("interface.customize.wallpaperlore").getMessage()));
        setItem("floor", 4, 6, GuiUtils.createButtonItem(XMaterial.OAK_SLAB,
                plugin.getLocale().getMessage("interface.customize.floor").getMessage(),
                plugin.getLocale().getMessage("interface.customize.floorlore").getMessage()));

        setButton("red", 5, 1, red, redClick);
        setButton("red1", 5, 2, red, redClick);
        setButton("red2", 5, 3, red, redClick);

        setButton("green", 5, 5, green, click);
        setButton("green1", 5, 6, green, click);
        setButton("green2", 5, 7, green, click);

        setAcceptsItems(true);
    }
}