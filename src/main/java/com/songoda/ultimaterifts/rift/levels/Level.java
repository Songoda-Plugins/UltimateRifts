package com.songoda.ultimaterifts.rift.levels;

import com.craftaro.core.locale.Locale;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.ultimaterifts.UltimateRifts;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Level {
    private final List<String> description = new ArrayList<>();
    private final int level;
    private final int costExperience;
    private final int costEconomy;
    private final int size;
    private final String door;
    private List<PotionEffect> potionEffects;
    private final String[] recipe;
    private final int breakDelay;

    public Level(int level, int costExperience, int costEconomy, int size, String door, List<PotionEffect> potionEffects, String[] recipe, int breakDelay) {
        this.level = level;
        this.costExperience = costExperience;
        this.costEconomy = costEconomy;
        this.size = size;
        this.door = door;
        this.potionEffects = potionEffects;
        this.recipe = recipe;
        this.breakDelay = breakDelay;

        buildDescription();
    }

    public void buildDescription() {
        Locale locale = UltimateRifts.getInstance().getLocale();

        this.description.add(locale.getMessage("interface.rift.size")
                .processPlaceholder("size", size).getMessage());

        this.description.add(locale.getMessage("interface.rift.breakdelay")
                .processPlaceholder("delay", breakDelay).getMessage());

        if (!potionEffects.isEmpty()) {
            for (PotionEffect potionEffect : potionEffects) {
                PotionEffectType effectType = potionEffect.getType();
                int amplifier = potionEffect.getAmplifier();
                int duration = potionEffect.getDuration();

                String effectName = effectType.getName().toLowerCase().replace("_", " ");
                String romanNumeral = getRomanNumeral(amplifier + 1);
                int seconds = duration / 20;

                this.description.add(locale.getMessage("interface.rift.potioneffect")
                        .processPlaceholder("effect", effectName)
                        .processPlaceholder("amplifier", romanNumeral)
                        .processPlaceholder("duration", seconds)
                        .getMessage());
            }
        }
    }

    private String getRomanNumeral(int number) {
        String[] romanNumerals = {"I", "II", "III", "IV", "V"};
        return number >= 1 && number <= 5 ? romanNumerals[number - 1] : String.valueOf(number);
    }

    public List<String> getDescription() {
        return new ArrayList<>(this.description);
    }

    public int getLevel() {
        return this.level;
    }

    public int getCostExperience() {
        return this.costExperience;
    }

    public int getCostEconomy() {
        return this.costEconomy;
    }

    public int getSize() {
        return this.size;
    }

    public Material getDoor() {
        return XMaterial.valueOf(this.door + "_DOOR").parseMaterial();
    }

    public List<PotionEffect> getPotionEffects() {
        return Collections.unmodifiableList(this.potionEffects);
    }

    public String[] getRecipe() {
        return recipe;
    }

    public int getBreakDelay() {
        return breakDelay;
    }
}