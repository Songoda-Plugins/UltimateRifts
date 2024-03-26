package com.songoda.ultimaterifts.task;

import com.songoda.ultimaterifts.UltimateRifts;
import com.songoda.ultimaterifts.rift.RiftManager;
import com.songoda.ultimaterifts.rift.Rift;
import com.songoda.ultimaterifts.rift.levels.Level;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class RiftEffectTask extends BukkitRunnable {

    private final UltimateRifts plugin;
    private final RiftManager riftManager;

    public RiftEffectTask(UltimateRifts plugin, RiftManager riftManager) {
        this.plugin = plugin;
        this.riftManager = riftManager;
        runTaskTimer(plugin, 0L, 40L); // Run every 40 ticks (2 seconds)
    }

    @Override
    public void run() {
        for (Rift rift : riftManager.getRifts()) {
            if (rift.isPotionEffect()) {
                Level level = rift.getLevel();
                List<PotionEffect> potionEffects = level.getPotionEffects();
                Location center = rift.getCenter();
                for (Player player : center.getWorld().getPlayers()) {
                    Location playerLocation = player.getLocation();
                    if (rift.isInBounds(playerLocation)) {
                        for (PotionEffect effect : potionEffects) {
                            player.addPotionEffect(effect);
                        }
                    }
                }
            }
        }
    }
}