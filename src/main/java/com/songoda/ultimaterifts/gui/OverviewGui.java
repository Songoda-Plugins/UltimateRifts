package com.songoda.ultimaterifts.gui;

import com.songoda.core.gui.CustomizableGui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.utils.NumberUtils;
import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.ultimaterifts.UltimateRifts;
import com.songoda.ultimaterifts.rift.Rift;
import com.songoda.ultimaterifts.rift.levels.Level;
import com.songoda.ultimaterifts.settings.Settings;
import com.songoda.ultimaterifts.utils.CostType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class OverviewGui extends CustomizableGui {
    private final UltimateRifts plugin;
    private final Rift rift;
    private final Player player;

    private int task;
    static int[][] infoIconOrder = new int[][]{{22}, {21, 23}, {21, 22, 23}, {12, 21, 22, 23}, {12, 14, 21, 22, 23}, {3, 12, 14, 21, 22, 23}, {3, 5, 12, 14, 21, 22, 23}};

    public OverviewGui(Rift rift, Player player) {
        super(UltimateRifts.getPlugin(UltimateRifts.class), "main");
        this.plugin = UltimateRifts.getPlugin(UltimateRifts.class);

        this.rift = rift;
        this.player = player;

        showPage();
        setRows(3);
        setUnlockedRange(3, 0, 5, 8);

        setTitle(plugin.formatName(rift.getLevel().getLevel()));
        setAcceptsItems(true);
    }

    private void showPage() {
        ItemStack glass1 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_1.getMaterial());
        ItemStack glass2 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_2.getMaterial());
        ItemStack glass3 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_3.getMaterial());


        mirrorFill("mirrorfill_1", 0, 0, false, true, glass2);
        mirrorFill("mirrorfill_2", 0, 1, false, true, glass2);
        mirrorFill("mirrorfill_3", 0, 2, false, true, glass3);
        mirrorFill("mirrorfill_4", 1, 0, false, true, glass2);
        mirrorFill("mirrorfill_5", 1, 1, false, true, glass3);
        mirrorFill("mirrorfill_6", 2, 0, false, true, glass2);
        mirrorFill("mirrorfill_7", 2, 1, false, true, glass2);
        mirrorFill("mirrorfill_8", 2, 2, false, true, glass3);

        mirrorFill("mirrorfill_9", 0, 3, false, true, glass1);
        mirrorFill("mirrorfill_10", 0, 4, false, false, glass1);
        mirrorFill("mirrorfill_11", 1, 3, false, true, glass1);
        mirrorFill("mirrorfill_12", 1, 2, false, true, glass1);
        mirrorFill("mirrorfill_13", 2, 3, false, true, glass1);
        mirrorFill("mirrorfill_14", 2, 4, false, false, glass1);


        Level level = rift.getLevel();
        Level nextLevel = plugin.getLevelManager().getHighestLevel().getLevel() > level.getLevel() ? this.plugin.getLevelManager().getLevel(level.getLevel() + 1) : null;

        // main rift information icon
        setItem("information", 1, 4, GuiUtils.createButtonItem(
                Settings.HUB_ITEM.getMaterial(XMaterial.BEACON),
                plugin.getLocale().getMessage("interface.rift.currentlevel")
                        .processPlaceholder("level", level.getLevel()).toText(),
                getRiftDescription(nextLevel)));

        if (Settings.UPGRADE_WITH_XP.getBoolean()
                && level.getCostExperience() != -1
                && player.hasPermission("Ultimaterifts.Upgrade.XP")) {
            setButton("upgrade_xp", 1, 2, GuiUtils.createButtonItem(
                            Settings.XP_ICON.getMaterial(XMaterial.EXPERIENCE_BOTTLE),
                            plugin.getLocale().getMessage("interface.rift.upgradewithxp").toText(),
                            nextLevel != null
                                    ? plugin.getLocale().getMessage("interface.rift.upgradewithxplore")
                                    .processPlaceholder("cost", nextLevel.getCostExperience()).toText()
                                    : plugin.getLocale().getMessage("interface.rift.alreadymaxed").toText()),
                    (event) -> {
                        rift.upgradeAndCarve(this.player, CostType.EXPERIENCE);
                        showPage();
                    });
        }

        if (Settings.UPGRADE_WITH_ECONOMY.getBoolean()
                && level.getCostEconomy() != -1
                && this.player.hasPermission("Ultimaterifts.Upgrade.ECO")) {
            setButton("upgrade_economy", 1, 6, GuiUtils.createButtonItem(
                            Settings.ECO_ICON.getMaterial(XMaterial.SUNFLOWER),
                            this.plugin.getLocale().getMessage("interface.rift.upgradewitheconomy").toText(),
                            nextLevel != null
                                    ? this.plugin.getLocale().getMessage("interface.rift.upgradewitheconomylore")
                                    .processPlaceholder("cost", NumberUtils.formatNumber(nextLevel.getCostEconomy())).toText()
                                    : this.plugin.getLocale().getMessage("interface.rift.alreadymaxed").toText()),
                    (event) -> {
                        rift.upgradeAndCarve(this.player, CostType.ECONOMY);
                        showPage();
                    });
        }

        if (Settings.ALLOW_CUSTOMIZING.getBoolean()) {
            setButton("customize", 0, 0, GuiUtils.createButtonItem(
                            Settings.CUSTOMIZE_ICON.getMaterial(XMaterial.PAINTING),
                            this.plugin.getLocale().getMessage("interface.rift.customize").toText(),
                            this.plugin.getLocale().getMessage("interface.rift.customizelore").toText().split("\\|")),
                    event -> {
                        this.plugin.getGuiManager().showGUI(this.player, new CustomizeGui(plugin, this.rift));
                        showPage();
                    });
        }

        if (Settings.ALLOW_LOCKED.getBoolean()) {
            setButton("locked", 0, 8, GuiUtils.createButtonItem(
                            Settings.LOCK_ICON.getMaterial(XMaterial.REDSTONE_BLOCK),
                            rift.isLocked() ? this.plugin.getLocale().getMessage("interface.rift.locked").toText().split("\\|")
                                    : this.plugin.getLocale().getMessage("interface.rift.unlocked").toText().split("\\|")),
                    event -> {
                        rift.setLocked(!rift.isLocked());
                        if (rift.isLocked())
                            rift.expelGuests(false);
                        rift.save("is_locked");
                        showPage();
                    });
        }

        if (Settings.ALLOW_INVITES.getBoolean()) {
            setButton("invite", 0, 7, GuiUtils.createButtonItem(
                            Settings.INVITE_ICON.getMaterial(XMaterial.PAPER),
                            this.plugin.getLocale().getMessage("interface.rift.members").toText(),
                            this.plugin.getLocale().getMessage("interface.rift.memberslore")
                                    .processPlaceholder("members", rift.getMembers().size()).toText().split("\\|")),
                    event -> {
                        this.plugin.getGuiManager().showGUI(this.player, new MembersGui(plugin, this.rift));
                    });
        }

        if (Settings.ALLOW_MOVE_DOOR.getBoolean()) {
            setButton("move_door", 0, 6, GuiUtils.createButtonItem(
                            XMaterial.OAK_DOOR.parseItem(),
                            this.plugin.getLocale().getMessage("interface.rift.movedoor").toText(),
                            this.plugin.getLocale().getMessage("interface.rift.movedoorlore").toText().split("\\|")),
                    event -> {
                        this.plugin.getDoorHandler().addSelection(this.player);
                        exit();
                    });
        }

        int num = 0;
        if (!level.getPotionEffects().isEmpty())
            num++;

        int current = 0;

        if (!level.getPotionEffects().isEmpty()) {
            setButton("potion_effect", infoIconOrder[num][current++], GuiUtils.createButtonItem(
                            Settings.POTION_EFFECT_ICON.getMaterial(XMaterial.POTION),
                            this.plugin.getLocale().getMessage("interface.rift.potioneffecttitle").toText(),
                            this.plugin.getLocale().getMessage("interface.rift.potioneffectinfo")
                                    .processPlaceholder("status", rift.isPotionEffect() ? plugin.getLocale().getMessage("on").toText()
                                            : plugin.getLocale().getMessage("off").toText()).toText().split("\\|")),
                    event -> {
                        rift.setPotionEffect(!rift.isPotionEffect());
                        showPage();
                    });
        }
    }

    private List<String> getRiftDescription(Level nextLevel) {
        Level level = rift.getLevel();
        ArrayList<String> lore = new ArrayList<>();
        lore.addAll(level.getDescription());
        lore.add("");
        if (nextLevel == null) {
            lore.add(plugin.getLocale().getMessage("interface.rift.alreadymaxed").toText());
        } else {
            lore.add(plugin.getLocale().getMessage("interface.rift.level")
                    .processPlaceholder("level", nextLevel.getLevel()).toText());
            lore.addAll(nextLevel.getDescription());
        }
        return lore;
    }
}
