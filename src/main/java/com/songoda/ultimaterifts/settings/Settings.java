package com.songoda.ultimaterifts.settings;

import com.craftaro.core.compatibility.CompatibleMaterial;
import com.craftaro.core.configuration.Config;
import com.craftaro.core.configuration.ConfigSetting;
import com.craftaro.core.hooks.EconomyManager;
import com.craftaro.core.nms.nbt.NBTCompound;
import com.songoda.ultimaterifts.UltimateRifts;
import org.bukkit.Material;

public class Settings {
    static final Config CONFIG = UltimateRifts.getPlugin(UltimateRifts.class).getCoreConfig();

    public static final ConfigSetting RIFT_SIZE = new ConfigSetting(CONFIG, "Main.Rift Size",
    25, "This is the maximum size of a rift.",
            "Do not change this unless you regenerate the world.");

    public static final ConfigSetting RIFT_HEIGHT = new ConfigSetting(CONFIG, "Main.Rift Height",
    3, "This is the height of a rift.");

    public static final ConfigSetting RIFT_WORLD = new ConfigSetting(CONFIG, "Main.Rift World",
    "RiftWorld", "The world where rifts are generated.");

    public static final ConfigSetting HUB_ITEM = new ConfigSetting(CONFIG, "Main.Hub Item",
            Material.BEACON.name());

    public static final ConfigSetting ALLOW_LOCKED = new ConfigSetting(CONFIG, "Main.Allow Locked", true,
            "Should locked rifts be allowed?");
    public static final ConfigSetting ALLOW_INVITES = new ConfigSetting(CONFIG, "Main.Allow Invites", true,
            "Should an owner be able to invite players to their rift?");

    public static final ConfigSetting ALLOW_MOVE_DOOR = new ConfigSetting(CONFIG, "Main.Allow Move Door", true,
            "Should players be able to move the door of a rift?");
    public static final ConfigSetting DANGEROUS_RIFTS = new ConfigSetting(CONFIG, "Main.Dangerous Rifts", false,
            "Should rifts allow players to break blocks and attack other players in other people's rifts?");

    public static final ConfigSetting ALLOW_CUSTOMIZING = new ConfigSetting(CONFIG, "Main.Allow Customizing", true,
            "Should players be able to customize their rifts wallpaper and floor?");

    public static final ConfigSetting ALLOW_STEALING = new ConfigSetting(CONFIG, "Main.Allow Stealing", false,
            "Should players be able to steal another players rift by breaking the door?");

    public static final ConfigSetting MAX_RIFT_MEMBERS = new ConfigSetting(CONFIG, "Main.Max Rift Members", 5,
            "The maximum number of members allowed in a rift (including the owner).");

    public static final ConfigSetting MAX_RIFTS_PER_PLAYER = new ConfigSetting(CONFIG, "Main.Max Rifts Per Player", 3,
            "The maximum number of rifts a player can own.");

    public static final ConfigSetting LOCK_ICON = new ConfigSetting(CONFIG, "Main.Lock Icon", "REDSTONE_BLOCK",
            "The icon used in the overview interface that represents a locked rift.");

    public static final ConfigSetting INVITE_ICON = new ConfigSetting(CONFIG, "Main.Invite Icon", "PAPER",
            "The icon used in the overview interface that represents player invites.");

    public static final ConfigSetting CUSTOMIZE_ICON = new ConfigSetting(CONFIG, "Main.Customize Icon", "PAINTING",
            "The icon used in the overview interface that represents customizing the rift.");

    public static final ConfigSetting PARTICLE_TYPE = new ConfigSetting(CONFIG, "Main.Upgrade Particle Type", "SPELL_WITCH",
            "The type of particle shown when a rift is upgraded.");

    public static final ConfigSetting ECONOMY_PLUGIN = new ConfigSetting(CONFIG, "Main.Economy", EconomyManager.getEconomy() == null ? "Vault" : EconomyManager.getEconomy().getName(),
            "Which economy plugin should be used?",
            "Supported plugins you have installed: \"" + String.join("\", \"", EconomyManager.getManager().getRegisteredPlugins()) + "\".");

    public static final ConfigSetting UPGRADE_WITH_ECONOMY = new ConfigSetting(CONFIG, "Main.Upgrade With Economy", true,
            "Should you be able to upgrade rifts with economy?");

    public static final ConfigSetting UPGRADE_WITH_XP = new ConfigSetting(CONFIG, "Main.Upgrade With XP", true,
            "Should you be able to upgrade rifts with experience?");

    public static final ConfigSetting ECO_ICON = new ConfigSetting(CONFIG, "Interfaces.Economy Icon", "SUNFLOWER",
            "The icon used in the overview interface that represents the economy upgrade.");
    public static final ConfigSetting XP_ICON = new ConfigSetting(CONFIG, "Interfaces.XP Icon", "EXPERIENCE_BOTTLE",
            "The icon used in the overview interface that represents the experience upgrade.");

    public static final ConfigSetting POTION_EFFECT_ICON = new ConfigSetting(CONFIG, "Interfaces.Potion Effect Icon", "POTION",
            "The icon used in the overview interface that represents the potion effect upgrade.");

    public static final ConfigSetting GLASS_TYPE_1 = new ConfigSetting(CONFIG, "Interfaces.Glass Type 1", "GRAY_STAINED_GLASS_PANE",
            "The type of glass pane used in the interfaces.");
    public static final ConfigSetting GLASS_TYPE_2 = new ConfigSetting(CONFIG, "Interfaces.Glass Type 2", "BLUE_STAINED_GLASS_PANE",
            "The type of glass pane used in the interfaces.");
    public static final ConfigSetting GLASS_TYPE_3 = new ConfigSetting(CONFIG, "Interfaces.Glass Type 3", "LIGHT_BLUE_STAINED_GLASS_PANE",
            "The type of glass pane used in the interfaces.");


    public static final ConfigSetting LANGUGE_MODE = new ConfigSetting(CONFIG, "System.Language Mode", "en_US",
            "The enabled language file.",
            "More language files (if available) can be found in the plugins data folder.");

    /**
     * In order to set dynamic economy comment correctly, this needs to be
     * called after EconomyManager load
     */
    public static void setupConfig() {
        CONFIG.load();
        CONFIG.setAutoremove(true).setAutosave(true);

        // convert glass pane settings
        int color;
        if ((color = GLASS_TYPE_1.getInt(-1)) != -1) {
            CONFIG.set(GLASS_TYPE_1.getKey(), CompatibleMaterial.getGlassPaneForColor(color).name());
        }
        if ((color = GLASS_TYPE_2.getInt(-1)) != -1) {
            CONFIG.set(GLASS_TYPE_2.getKey(), CompatibleMaterial.getGlassPaneForColor(color).name());
        }
        if ((color = GLASS_TYPE_3.getInt(-1)) != -1) {
            CONFIG.set(GLASS_TYPE_3.getKey(), CompatibleMaterial.getGlassPaneForColor(color).name());
        }

        CONFIG.saveChanges();
    }
}
