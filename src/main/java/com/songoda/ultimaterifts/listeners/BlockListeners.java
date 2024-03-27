package com.songoda.ultimaterifts.listeners;

import com.craftaro.core.third_party.de.tr7zw.nbtapi.NBTItem;
import com.craftaro.third_party.com.cryptomorin.xseries.XSound;
import com.songoda.ultimaterifts.UltimateRifts;
import com.songoda.ultimaterifts.rift.DoorHandler;
import com.songoda.ultimaterifts.rift.Rift;
import com.songoda.ultimaterifts.rift.RiftManager;
import com.songoda.ultimaterifts.rift.levels.Level;
import com.songoda.ultimaterifts.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class BlockListeners implements Listener {
    private final UltimateRifts plugin;
    private final RiftManager riftManager;
    private final DoorHandler doorHandler;

    public BlockListeners(UltimateRifts plugin, RiftManager riftManager) {
        this.plugin = plugin;
        this.riftManager = riftManager;
        this.doorHandler = plugin.getDoorHandler();
    }


    @EventHandler
    public void onDoorPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType().name().contains("DOOR")) {
            NBTItem nbtItem = new NBTItem(item);
            if (nbtItem.hasKey("level")) {
                if (event.getBlock().getLocation().getWorld().getName().equals(Settings.RIFT_WORLD.getString())) {
                    plugin.getLocale().getMessage("event.place.world").sendPrefixedMessage(event.getPlayer());
                    XSound.ENTITY_VILLAGER_NO.play(event.getPlayer());
                    event.setCancelled(true);
                    return;
                }
                int level = nbtItem.getInteger("level");
                Location doorLocation = event.getBlockPlaced().getLocation();

                Level riftLevel = plugin.getLevelManager().getLevel(level);
                if (riftLevel == null) {
                    Bukkit.getLogger().warning("Level " + level + " not found.");
                    return;
                }

                int id = nbtItem.getInteger("riftId");
                if (id == -1)
                    id = riftManager.getNextAvailableRiftId();

                Rift rift = riftManager.getRiftById(id);
                Player player = event.getPlayer();

                if (rift != null
                        && rift.getPlacedDoor() == null
                        && !rift.getOwner().getUniqueId().equals(player.getUniqueId())
                        && plugin.getRiftManager().getOwnedRifts(player).size() >= Settings.MAX_RIFTS_PER_PLAYER.getInt()

                        || rift == null && plugin.getRiftManager().getOwnedRifts(player).size() >= Settings.MAX_RIFTS_PER_PLAYER.getInt()) {
                    plugin.getLocale().getMessage("event.place.riftlimit").sendPrefixedMessage(player);
                    XSound.ENTITY_VILLAGER_NO.play(player);
                    event.setCancelled(true);
                    return;
                }

                if (rift == null)
                    rift = riftManager.getOrCreateRiftById(id);

                if (rift.getOwner() != null && !rift.getOwner().getUniqueId().equals(player.getUniqueId()))
                    plugin.getLocale().getMessage("event.place.ownerchange").sendPrefixedMessage(player);

                rift.setOwner(player);

                rift.setLevel(riftLevel);

                Location existingDoorLocation = rift.getPlacedDoor();
                if (existingDoorLocation != null)
                    rift.removeDoubleDoor(existingDoorLocation.getBlock());

                rift.setPlacedDoor(doorLocation);
                if (rift.isFirstCarve())
                    rift.setup();
                rift.save();
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Location explosionLocation = event.getLocation();
        Rift rift = riftManager.getRiftByLocation(explosionLocation);
        if (rift != null) {
            event.blockList().removeIf(block -> !rift.canBreak(block.getLocation()));
        }
    }

    @EventHandler
    public void onDoorBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        BlockData doorData = block.getBlockData();
        if (doorData instanceof Door) {
            Door door = (Door) doorData;
            Block bottomBlock = door.getHalf() == Bisected.Half.TOP ? block.getRelative(BlockFace.DOWN) : block;

            Rift rift = riftManager.getRiftByDoor(bottomBlock.getLocation());

            if (rift != null) {
                if (rift.isOnDoorCooldown(event.getPlayer())) {
                    // Cancel breaking the door if on cooldown
                    event.setCancelled(true);
                    plugin.getLocale().getMessage("event.break.doorcooldown")
                            .processPlaceholder("time", rift.getTimeUntilDoorCooldownExpires())
                            .sendPrefixedMessage(event.getPlayer());
                    XSound.ENTITY_VILLAGER_NO.play(event.getPlayer());
                    return;
                }

                if (rift.getPlacedDoor().equals(bottomBlock.getLocation())) {
                    rift.expelGuests(true);
                    rift.setPlacedDoor(null);
                    event.setDropItems(false);
                    bottomBlock.getWorld().dropItemNaturally(event.getBlock().getLocation().add(.5, .5, .5), plugin.createLeveledRift(rift.getRiftId(), rift.getLevel().getLevel()));
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location blockLocation = block.getLocation();

        Rift rift = riftManager.getRiftByLocation(blockLocation);
        if (rift != null) {
            if (block.getLocation().equals(rift.getRiftDoor())) {
                plugin.getLocale().getMessage("event.break.door").sendPrefixedMessage(event.getPlayer());
                XSound.ENTITY_VILLAGER_NO.play(event.getPlayer());
                event.setCancelled(true);
                return;
            }

            if (!Settings.ALLOW_STEALING.getBoolean() && !rift.isOwner(event.getPlayer()))
                event.setCancelled(true);

            if (!rift.canBreak(blockLocation)) {
                event.setCancelled(true);
                plugin.getLocale().getMessage("event.break.outside").sendPrefixedMessage(event.getPlayer());
                XSound.ENTITY_VILLAGER_NO.play(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onBeaconInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            Player player = event.getPlayer();
            if (plugin.getDoorHandler().isInSelection(player)) {
                Rift rift = riftManager.getRiftByLocation(clickedBlock.getLocation());
                if (rift != null) {
                    rift.attemptMoveDoor(player, clickedBlock.getLocation());
                }

                doorHandler.removeSelection(player);
            }
            if (clickedBlock != null && clickedBlock.getType() == Material.BEACON) {
                Location beaconLocation = clickedBlock.getLocation();
                Rift rift = riftManager.getRiftByLocation(beaconLocation);
                if (rift == null)
                    return;

                event.setCancelled(true);
                if (!rift.hasAccess(player)) {
                    plugin.getLocale().getMessage("event.interact.notowner").sendPrefixedMessage(player);
                    XSound.ENTITY_VILLAGER_NO.play(player);
                    return;
                }
                plugin.getGuiManager().showGUI(player, rift.getOverviewGui(player));
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();

        if (to.getBlock().getType().name().contains("DOOR") && from.getBlock().getType().name().contains("DOOR")) {
            Block doorBlock = to.getBlock();
            Rift rift = riftManager.getRiftByDoor(doorBlock.getLocation());
            if (rift == null)
                return;

            BlockData doorData = doorBlock.getBlockData();
            if (doorData instanceof Door) {
                Door door = (Door) doorData;
                if (door.isOpen()) {
                    if (RiftManager.isDoorLocation(doorBlock.getLocation(), rift.getPlacedDoor())) {
                        if (rift.isLocked() && !rift.hasAccess(player)) {
                            plugin.getLocale().getMessage("event.enter.locked").sendPrefixedMessage(player);
                            XSound.ENTITY_VILLAGER_NO.play(player);
                            return;
                        }
                        rift.enter(player);
                    } else if (RiftManager.isDoorLocation(doorBlock.getLocation(), rift.getRiftDoor())) {
                        rift.exit(player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            Rift rift = riftManager.getRiftByLocation(block.getLocation());
            if (rift != null && !rift.canBreak(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }
}