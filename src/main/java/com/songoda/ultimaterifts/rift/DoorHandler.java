package com.songoda.ultimaterifts.rift;

import com.songoda.ultimaterifts.UltimateRifts;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DoorHandler {

    private final Map<UUID, BukkitTask> selectionTasks = new HashMap<>();

    public boolean isInSelection(Player player) {
        return selectionTasks.containsKey(player.getUniqueId());
    }

    public void addSelection(Player player) {
        UUID playerId = player.getUniqueId();
        if (!isInSelection(player)) {
            // Start the timeout task
            BukkitTask task = Bukkit.getScheduler().runTaskLater(UltimateRifts.getInstance(), () -> {
                if (isInSelection(player)) {
                    removeSelection(player);
                    UltimateRifts.getInstance().getLocale().getMessage("event.door.move.timed-out").sendPrefixedMessage(player);
                }
            }, 20L * 15); // 20 ticks per second, so 20 * 15 = 300 ticks (15 seconds)

            selectionTasks.put(playerId, task);
        }
    }

    public void removeSelection(Player player) {
        UUID playerId = player.getUniqueId();
        if (isInSelection(player)) {
            // Cancel the timeout task
            BukkitTask task = selectionTasks.remove(playerId);
            if (task != null) {
                task.cancel();
            }
        }
    }
}