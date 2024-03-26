package com.songoda.ultimaterifts;

import com.craftaro.core.SongodaCore;
import com.craftaro.core.SongodaPlugin;
import com.craftaro.core.commands.CommandManager;
import com.craftaro.core.configuration.Config;
import com.craftaro.core.data.DatabaseManager;
import com.craftaro.core.gui.GuiManager;
import com.craftaro.core.third_party.de.tr7zw.nbtapi.NBTItem;
import com.craftaro.core.utils.TextUtils;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.ultimaterifts.commands.*;
import com.songoda.ultimaterifts.generator.StonePlotGenerator;
import com.songoda.ultimaterifts.listeners.BlockListeners;
import com.songoda.ultimaterifts.rift.RiftManager;
import com.songoda.ultimaterifts.rift.levels.Level;
import com.songoda.ultimaterifts.rift.levels.LevelManager;
import com.songoda.ultimaterifts.schematic.SchematicManager;
import com.songoda.ultimaterifts.settings.Settings;
import com.songoda.ultimaterifts.task.RiftEffectTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UltimateRifts extends SongodaPlugin {

    private final Config levelsFile = new Config(this, "levels.yml");

    private CommandManager commandManager;

    private DatabaseManager databaseManager;
    private SchematicManager schematicManager;

    private RiftManager riftManager;
    private LevelManager levelManager;

    private final GuiManager guiManager = new GuiManager(this);

    public static UltimateRifts INSTANCE;

    @Override
    public void onPluginLoad() {
    }

    @Override
    public void onPluginEnable() {
        // Run Songoda Updater
        SongodaCore.registerPlugin(this, -1, XMaterial.REDSTONE);

        INSTANCE = this;

        // Config
        Settings.setupConfig();
        setLocale(Settings.LANGUGE_MODE.getString(), false);

        // Register commands
        this.commandManager = new CommandManager(this);
        this.commandManager.addMainCommand("UltimateRifts")
                .addSubCommands(
                        new CommandGive(this),
                        new CommandInfo(this),
                        new CommandSave(this),
                        new CommandPaste(this),
                        new CommandTele(this));

        loadLevelManager();
        registerCraftingRecipes();

        riftManager = new RiftManager();

        // Generate the stone rift world
        generateStonePlotWorld();

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new BlockListeners(this, riftManager), this);

        databaseManager = new DatabaseManager(this);

        new RiftEffectTask(this, riftManager);

        databaseManager = new DatabaseManager(this);
        schematicManager = new SchematicManager(this);

        databaseManager.load("Rifts", () -> riftManager.loadData());
    }

    @Override
    public void onPluginDisable() {
    }

    @Override
    public void onDataLoad() {
    }

    @Override
    public void onConfigReload() {
        this.setLocale(getConfig().getString("System.Language Mode"), true);
        this.locale.reloadMessages();
    }

    @Override
    public List<Config> getExtraConfig() {
        return Arrays.asList(this.levelsFile, databaseManager.getConfig());
    }

    private void loadLevelManager() {
        if (!new File(this.getDataFolder(), "levels.yml").exists()) {
            this.saveResource("levels.yml", false);
        }
        this.levelsFile.load();

        // Load an instance of LevelManager
        this.levelManager = new LevelManager();

        /*
         * Register Levels into LevelManager from configuration.
         */
        for (String levelName : this.levelsFile.getKeys(false)) {
            ConfigurationSection levels = this.levelsFile.getConfigurationSection(levelName);

            int level = Integer.parseInt(levelName.split("-")[1]);
            int costExperience = levels.getInt("Cost-xp");
            int costEconomy = levels.getInt("Cost-eco");
            int size = levels.getInt("Size");
            String door = levels.getString("Door").toUpperCase();
            int breakDelay = levels.getInt("Break Delay");

            List<PotionEffect> potionEffects = new ArrayList<>();
            ConfigurationSection effectsSection = levels.getConfigurationSection("Potion Effects");
            if (effectsSection != null) {
                for (String effectKey : effectsSection.getKeys(false)) {
                    ConfigurationSection effectConfig = effectsSection.getConfigurationSection(effectKey);
                    if (effectConfig != null) {
                        String effectType = effectConfig.getString("Effect");
                        int amplifier = effectConfig.getInt("Amplifier");
                        int duration = effectConfig.getInt("Duration");
                        PotionEffectType potionEffectType = PotionEffectType.getByName(effectType);
                        if (potionEffectType != null)
                            potionEffects.add(new PotionEffect(potionEffectType, duration, amplifier));
                    }
                }
            }

            String[] recipe = new String[3];
            ConfigurationSection recipeSection = levels.getConfigurationSection("Recipe");
            if (recipeSection != null) {
                recipe[0] = recipeSection.getString("Top");
                recipe[1] = recipeSection.getString("Middle");
                recipe[2] = recipeSection.getString("Bottom");
            }

            Level levelObj = new Level(level, costExperience, costEconomy, size, door, potionEffects, recipe, breakDelay);

            this.levelManager.addLevel(levelObj);
        }
        this.levelsFile.saveChanges();
    }

    private void registerCraftingRecipes() {
        for (Level level : getLevelManager().getLevels().values()) {
            String[] recipe = level.getRecipe();
            if (recipe != null && recipe.length == 3) {
                NamespacedKey key = new NamespacedKey(this, "rift_level_" + level.getLevel());
                ShapedRecipe shapedRecipe = new ShapedRecipe(key, createLeveledRift(level.getLevel(), level.getLevel()));

                String[] shape = new String[3];
                shape[0] = "ABC";
                shape[1] = "DEF";
                shape[2] = "GHI";
                shapedRecipe.shape(shape);

                String[] ingredients = recipe[0].split(",");
                shapedRecipe.setIngredient('A', Material.getMaterial(ingredients[0]));
                shapedRecipe.setIngredient('B', Material.getMaterial(ingredients[1]));
                shapedRecipe.setIngredient('C', Material.getMaterial(ingredients[2]));

                ingredients = recipe[1].split(",");
                shapedRecipe.setIngredient('D', Material.getMaterial(ingredients[0]));
                shapedRecipe.setIngredient('E', Material.getMaterial(ingredients[1]));
                shapedRecipe.setIngredient('F', Material.getMaterial(ingredients[2]));

                ingredients = recipe[2].split(",");
                shapedRecipe.setIngredient('G', Material.getMaterial(ingredients[0]));
                shapedRecipe.setIngredient('H', Material.getMaterial(ingredients[1]));
                shapedRecipe.setIngredient('I', Material.getMaterial(ingredients[2]));

                Bukkit.addRecipe(shapedRecipe);
            }
        }
    }

    private void generateStonePlotWorld() {
        WorldCreator creator = new WorldCreator(Settings.RIFT_WORLD.getString());
        creator.generator(new StonePlotGenerator());
        creator.createWorld();
    }

    public ItemStack createLeveledRift(int id, int level) {
        ItemStack item = new ItemStack(levelManager.getLevel(level).getDoor(), 1);

        ItemMeta itemmeta = item.getItemMeta();
        itemmeta.setDisplayName(TextUtils.formatText(formatName(level)));
        if (id != -1)
            itemmeta.setLore(Arrays.asList(TextUtils.formatText("&8Rift #" + id)));
        item.setItemMeta(itemmeta);

        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setInteger("riftId", id);
        nbtItem.setInteger("level", level);

        return nbtItem.getItem();
    }

    public String formatName(int level) {
        return getLocale()
                .getMessage("general.nametag.rift")
                .processPlaceholder("level", level)
                .getMessage();
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public static UltimateRifts getInstance() {
        return INSTANCE;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public RiftManager getRiftManager() {
        return riftManager;
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

}