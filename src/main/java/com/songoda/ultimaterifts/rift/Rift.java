package com.songoda.ultimaterifts.rift;

import com.craftaro.core.compatibility.ServerVersion;
import com.craftaro.core.data.SQLDelete;
import com.craftaro.core.data.SQLInsert;
import com.craftaro.core.data.SavesData;
import com.craftaro.core.hooks.EconomyManager;
import com.craftaro.third_party.com.cryptomorin.xseries.XSound;
import com.craftaro.third_party.org.jooq.DSLContext;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.songoda.ultimaterifts.UltimateRifts;
import com.songoda.ultimaterifts.generator.StonePlotGenerator;
import com.songoda.ultimaterifts.gui.OverviewGui;
import com.songoda.ultimaterifts.rift.levels.Level;
import com.songoda.ultimaterifts.schematic.SchematicManager;
import com.songoda.ultimaterifts.settings.Settings;
import com.songoda.ultimaterifts.utils.CostType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Rift implements SavesData {

    private final int riftId;
    private Location riftDoor;
    private Location placedDoor;
    private Level level = UltimateRifts.getInstance().getLevelManager().getLowestLevel();
    private boolean firstCarve = true;

    private boolean potionEffect = true;
    private boolean isLocked = false;

    private final Map<UUID, Member> members = new HashMap<>();
    private Instant lastDoorEnter;

    private final Map<UUID, Instant> recentTeleports = new HashMap<>();

    public Rift(int riftId) {
        this.riftId = riftId;
    }


    private void carveRift() {
        Location center = getCenter();

        SchematicManager schematicManager = UltimateRifts.getInstance().getSchematicManager();
        schematicManager.pasteSchematic(level.getLevel(), center);

        int radius = level.getSize() / 2;
        // Search for a door block along the walls
        Location doorLocation = findDoorInWalls(center);
        if (doorLocation != null) {
            setRiftDoor(doorLocation);
        } else {
            // If no door block was found, place a door block on the east wall
            doorLocation = getCenter().clone();
            doorLocation.setX(doorLocation.getX() + radius + 1);
            doorLocation.setY(1);
        }

        BlockFace doorFacing = getDirectionFacing(center, doorLocation);
        placeDoubleDoor(doorLocation.getBlock(), doorFacing, null);

        setRiftDoor(doorLocation);

        if (firstCarve) {
            firstCarve = false;
        }
    }

    private Location findDoorInWalls(Location center) {
        int radius = (level.getSize() / 2) + 1;
        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;
        int minY = center.getBlockY();
        int maxY = center.getBlockY() + 2; // Assuming the door is within 2 blocks above the center

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                Location loc1 = new Location(center.getWorld(), x, y, minZ);
                Location loc2 = new Location(center.getWorld(), x, y, maxZ);
                if (isDoorBlock(loc1)) {
                    return loc1;
                }
                if (isDoorBlock(loc2)) {
                    return loc2;
                }
            }
            for (int z = minZ; z <= maxZ; z++) {
                Location loc1 = new Location(center.getWorld(), minX, y, z);
                Location loc2 = new Location(center.getWorld(), maxX, y, z);
                if (isDoorBlock(loc1)) {
                    return loc1;
                }
                if (isDoorBlock(loc2)) {
                    return loc2;
                }
            }
        }
        return null;
    }

    private boolean isDoorBlock(Location location) {
        Block block = location.getBlock();
        return block.getType().toString().contains("DOOR");
    }

    private World getRiftWorld() {
        String worldName = Settings.RIFT_WORLD.getString();
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            // Create the world if it doesn't exist
            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new StonePlotGenerator());
            world = creator.createWorld();
        }
        return world;
    }

    private void placeDoubleDoor(Block doorBlock, BlockFace facing, Runnable callback) {
        Level startLevel = level;
        Material doorType = level.getDoor();

        // Get the coordinates of the bottom door block
        int xWorld = doorBlock.getX();
        int yWorld = doorBlock.getY();
        int zWorld = doorBlock.getZ();

        // Get the bottom and top door blocks
        Block bottom = doorBlock.getWorld().getBlockAt(xWorld, yWorld, zWorld);
        Block top = bottom.getRelative(BlockFace.UP);

        // Determine the hinge side based on the facing direction
        Door.Hinge hinge = facing == BlockFace.EAST || facing == BlockFace.SOUTH ? Door.Hinge.RIGHT : Door.Hinge.LEFT;

        top.setType(doorType, false);
        Door topDoor = (Door) top.getBlockData();
        topDoor.setHalf(Bisected.Half.TOP);
        topDoor.setFacing(facing);
        topDoor.setHinge(hinge);
        top.setBlockData(topDoor);

        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("UltimateRifts"), () -> {
            if (startLevel != level)
                return;

            bottom.setType(doorType, false);
            Door bottomDoor = (Door) bottom.getBlockData();
            bottomDoor.setHalf(Bisected.Half.BOTTOM);
            bottomDoor.setFacing(facing);
            bottomDoor.setHinge(hinge);
            bottom.setBlockData(bottomDoor);
            if (callback != null)
                callback.run();
        }, 1L);
    }

    public OverviewGui getOverviewGui(Player player) {
        return new OverviewGui(this, player);
    }

    public void upgradeAndCarve(Player player, CostType type) {
        // Scan the user-interactable area for non-air blocks
        List<BlockState> savedBlocks = scanUserArea();

        // Perform the upgrade
        boolean upgradeSuccessful = upgrade(player, type);

        if (upgradeSuccessful) {
            // Paste the schematic as usual
            carveRift();

            // Restore the saved blocks
            restoreSavedBlocks(savedBlocks);
        }
    }

    private List<BlockState> scanUserArea() {
        List<BlockState> savedBlocks = new ArrayList<>();

        Location center = getCenter();
        int radius = level.getSize() / 2;
        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;
        int minY = 1;
        int maxY = Settings.RIFT_HEIGHT.getInt();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = center.getWorld().getBlockAt(x, y, z);
                    if (!block.getType().isAir()) {
                        savedBlocks.add(block.getState());
                    }
                }
            }
        }

        return savedBlocks;
    }

    private void restoreSavedBlocks(List<BlockState> savedBlocks) {
        for (BlockState state : savedBlocks) {
            Block block = state.getBlock();
            Material blockType = state.getType();

            if (blockType == Material.WALL_TORCH ||
                    blockType.name().contains("_SIGN") ||
                    blockType == Material.LADDER ||
                    blockType.name().equals("WALL_BANNER") ||
                    blockType == Material.ITEM_FRAME ||
                    blockType == Material.PAINTING) {
                // Drop the block as an item
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(blockType));
            } else {
                // Restore the block state
                state.update(true, false);
            }
        }
    }

    private boolean upgrade(Player player, CostType type) {
        UltimateRifts plugin = UltimateRifts.getInstance();
        if (!plugin.getLevelManager().getLevels().containsKey(this.level.getLevel() + 1)) {
            return false;
        }

        Level level = plugin.getLevelManager().getLevel(this.level.getLevel() + 1);
        int cost = type == CostType.ECONOMY ? level.getCostEconomy() : level.getCostExperience();

        if (type == CostType.ECONOMY) {
            if (!EconomyManager.isEnabled()) {
                player.sendMessage("Economy not enabled.");
                return false;
            }
            if (!EconomyManager.hasBalance(player, cost)) {
                plugin.getLocale().getMessage("event.upgrade.cannotafford").sendPrefixedMessage(player);
                return false;
            }
            EconomyManager.withdrawBalance(player, cost);
            upgradeFinal(player);
            return true;
        } else if (type == CostType.EXPERIENCE) {
            if (player.getLevel() >= cost || player.getGameMode() == GameMode.CREATIVE) {
                if (player.getGameMode() != GameMode.CREATIVE) {
                    player.setLevel(player.getLevel() - cost);
                }
                upgradeFinal(player);
                return true;
            } else {
                plugin.getLocale().getMessage("event.upgrade.cannotafford").sendPrefixedMessage(player);
                return false;
            }
        }
        return false;
    }

    private void upgradeFinal(Player player) {
        UltimateRifts plugin = UltimateRifts.getInstance();
        levelUp();
        if (plugin.getLevelManager().getHighestLevel() != this.level) {
            plugin.getLocale().getMessage("event.upgrade.success")
                    .processPlaceholder("level", this.level.getLevel()).sendPrefixedMessage(player);

        } else {
            plugin.getLocale().getMessage("event.upgrade.maxed")
                    .processPlaceholder("level", this.level.getLevel()).sendPrefixedMessage(player);
        }
        Location loc = getCenter().add(.5, .5, .5);

        setup();
        save();

        if (!ServerVersion.isServerVersionAtLeast(ServerVersion.V1_12)) {
            return;
        }

        player.getWorld().spawnParticle(org.bukkit.Particle.valueOf(plugin.getConfig().getString("Main.Upgrade Particle Type")), loc, 200, .5, .5, .5);

        if (plugin.getLevelManager().getHighestLevel() != this.level) {
            XSound.ENTITY_PLAYER_LEVELUP.play(player, .6f, 15);
        } else {
            XSound.ENTITY_PLAYER_LEVELUP.play(player, 2, 25);

            if (!ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)) {
                return;
            }

            XSound.BLOCK_NOTE_BLOCK_CHIME.play(player, 2, 25);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> XSound.BLOCK_NOTE_BLOCK_CHIME.play(player, 1.2f, 35), 5);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> XSound.BLOCK_NOTE_BLOCK_CHIME.play(player, 1.8f, 35), 10);
        }
    }

    public void levelUp() {
        UltimateRifts plugin = UltimateRifts.getInstance();
        this.level = plugin.getLevelManager().getLevel(this.level.getLevel() + 1);
    }

    public void removeDoubleDoor(Block doorBlock) {
        doorBlock.setType(Material.AIR);
        doorBlock.getRelative(BlockFace.UP).setType(Material.AIR);
    }

    private BlockFace getDirectionFacing(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();

        if (Math.abs(dx) > Math.abs(dz)) {
            if (dx > 0) {
                return BlockFace.EAST;
            } else {
                return BlockFace.WEST;
            }
        } else {
            if (dz > 0) {
                return BlockFace.SOUTH;
            } else {
                return BlockFace.NORTH;
            }
        }
    }

    public void setup() {
        carveRift();
    }

    public Location getCenter() {
        World world = getRiftWorld();
        int riftSize = Settings.RIFT_SIZE.getInt();

        // Calculate the spiral coordinates
        int x = 0;
        int z = 0;
        int dx = 0;
        int dz = -1;
        int segment = 1;
        int segmentLength = 1;
        int segmentPassed = 0;

        for (int i = 0; i < riftId; i++) {
            if (segmentPassed == segmentLength) {
                segmentPassed = 0;
                segment++;
                if (segment % 2 == 0) {
                    segmentLength++;
                }
                int temp = dx;
                dx = -dz;
                dz = temp;
            }
            x += dx;
            z += dz;
            segmentPassed++;
        }

        int worldX = x * riftSize + riftSize / 2;
        int worldZ = z * riftSize + riftSize / 2;

        return new Location(world, worldX, 1, worldZ);
    }

    public int getRiftId() {
        return riftId;
    }

    public Location getRiftDoor() {
        return riftDoor;
    }

    public void setRiftDoor(Location riftDoor) {
        this.riftDoor = riftDoor;
    }

    public Location getPlacedDoor() {
        return placedDoor;
    }

    public void setPlacedDoor(Location placedDoor) {
        this.placedDoor = placedDoor;
    }

    public Level getLevel() {
        return level;
    }

    public boolean isInBounds(Location location) {
        if (!location.getWorld().equals(getRiftWorld())) {
            return false;
        }

        Location center = getCenter();
        int radius = level.getSize() / 2;
        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;
        int minY = 1;
        int maxY = Settings.RIFT_HEIGHT.getInt();

        return location.getBlockX() >= minX && location.getBlockX() <= maxX &&
                location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ &&
                location.getBlockY() >= minY && location.getBlockY() <= maxY;
    }

    public void setLevel(Level riftLevel) {
        this.level = riftLevel;
    }

    public boolean canTeleport(Player player) {
        Instant lastTeleport = recentTeleports.get(player.getUniqueId());
        if (lastTeleport == null) {
            return true;
        }
        Instant now = Instant.now();
        return lastTeleport.plusSeconds(2).isBefore(now);
    }

    public void exit(Player player) {
        if (!canTeleport(player) || placedDoor == null)
            return;

        placeDoubleDoor(placedDoor.getBlock(), getDirectionFacing(getCenter(), placedDoor), () -> {
            // Open the placed door
            openDoor(placedDoor.getBlock());
        });

        // Teleport the player to the location in front of the placed door
        Location teleportLocation = getPlayerLocationForDoor(getPlacedDoor());
        player.teleport(teleportLocation);
        recentTeleports.put(player.getUniqueId(), Instant.now());

        // Send a title message to the player
        player.sendTitle(ChatColor.GOLD + "Exiting Rift", ChatColor.YELLOW + "Come back soon!", 10, 70, 20);
        UltimateRifts.getInstance().getLocale().getMessage("event.exit.success").sendPrefixedMessage(player);
        XSound.ENTITY_ENDERMAN_TELEPORT.play(player);

        lastDoorEnter = Instant.now();
        save("last_door_enter", "owner");
    }

    private Location getPlayerLocationForDoor(Location doorLocation) {
        BlockData doorData = doorLocation.getBlock().getBlockData();
        if (doorData instanceof Door) {
            Door door = (Door) doorData;
            BlockFace doorFacing = door.getFacing();

            // Get the location in front of the door
            Location frontLocation = doorLocation.clone();
            frontLocation.add(0.5, 0, 0.5);

            // Adjust the location based on the door's facing direction
            switch (doorFacing) {
                case NORTH:
                    frontLocation.setYaw(0);
                    break;
                case EAST:
                    frontLocation.setYaw(90);
                    break;
                case SOUTH:
                    frontLocation.setYaw(180);
                    break;
                case WEST:
                    frontLocation.setYaw(-90);
                    break;
            }

            return frontLocation;
        }
        return doorLocation;
    }

    public boolean isOnDoorCooldown(Player player) {
        if (lastDoorEnter == null || members.containsKey(player.getUniqueId())
                && members.get(player.getUniqueId()).isOwner())
            return false;
        Instant cooldownExpiry = lastDoorEnter.plusSeconds(level.getBreakDelay() * 60L);
        return Instant.now().isBefore(cooldownExpiry);
    }

    public String getTimeUntilDoorCooldownExpires() {
        if (lastDoorEnter == null) {
            return "0";
        }
        Instant cooldownExpiry = lastDoorEnter.plusSeconds(level.getBreakDelay() * 60L);
        Duration remainingTime = Duration.between(Instant.now(), cooldownExpiry);

        long minutes = remainingTime.toMinutes();
        long seconds = remainingTime.minusMinutes(minutes).getSeconds();

        return String.format("%d:%02d", minutes, seconds);
    }

    private void openDoor(Block doorBlock) {
        Door door = (Door) doorBlock.getBlockData();
        door.setOpen(true);
        doorBlock.setBlockData(door);

        Block topDoorBlock = doorBlock.getRelative(BlockFace.UP);
        Door topDoor = (Door) topDoorBlock.getBlockData();
        topDoor.setOpen(true);
        topDoorBlock.setBlockData(topDoor);
    }

    public void enter(Player player) {
        if (!canTeleport(player))
            return;

        // Teleport the player to the location in front of the rift door
        Location teleportLocation = getPlayerLocationForDoor(getRiftDoor());
        player.teleport(teleportLocation);
        recentTeleports.put(player.getUniqueId(), Instant.now());

        // Set the owner if not already set
        if (getOwner() == null)
            setOwner(player).save();

        // Open the rift door
        openDoor(getRiftDoor().getBlock());

        // Send a title message to the player
        player.sendTitle(ChatColor.GOLD + "Entering Rift", ChatColor.YELLOW + "Rift ID: " + getRiftId(), 10, 70, 20);
        UltimateRifts.getInstance().getLocale().getMessage("event.enter.success")
                .processPlaceholder("id", getRiftId()).sendPrefixedMessage(player);
        XSound.ENTITY_ENDERMAN_TELEPORT.play(player);
    }

    public boolean isPotionEffect() {
        return potionEffect;
    }

    public void setPotionEffect(boolean potionEffect) {
        this.potionEffect = potionEffect;
    }

    public void setFirstCarve(boolean firstCarve) {
        this.firstCarve = firstCarve;
    }

    @Override
    public void saveImpl(DSLContext ctx, String... columns) {
        SQLInsert.create(ctx).insertInto("rift")
                .withField("rift_id", riftId)
                .withField("rift_door_world", riftDoor.getWorld().getName())
                .withField("rift_door_x", riftDoor.getBlockX())
                .withField("rift_door_y", riftDoor.getBlockY())
                .withField("rift_door_z", riftDoor.getBlockZ())
                .withField("placed_door_world", placedDoor.getWorld().getName())
                .withField("placed_door_x", placedDoor.getBlockX())
                .withField("placed_door_y", placedDoor.getBlockY())
                .withField("placed_door_z", placedDoor.getBlockZ())
                .withField("level", level.getLevel())
                .withField("first_carve", firstCarve)
                .withField("potion_effect", potionEffect)
                .withField("last_door_enter", lastDoorEnter != null ? lastDoorEnter.toEpochMilli() : null)
                .withField("is_locked", isLocked)
                .onDuplicateKeyUpdate(columns)
                .execute();
    }

    @Override
    public void deleteImpl(DSLContext ctx) {
        SQLDelete.create(ctx).delete("rift", "rift_id", riftId);
    }

    public Member addMember(UUID playerId) {
        Member member = new Member(playerId, this, System.currentTimeMillis());
        members.put(playerId, member);
        return member;
    }

    public Member removeMember(UUID playerId) {
        return members.remove(playerId);
    }

    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public Member setOwner(OfflinePlayer newOwner) {
        for (Member member : members.values()) {
            member.setOwner(false);
        }
        Member member = addMember(newOwner.getUniqueId());
        member.setOwner(true);
        return member;
    }

    public OfflinePlayer getOwner() {
        for (Member member : members.values()) {
            if (member.isOwner()) {
                return Bukkit.getOfflinePlayer(member.getPlayerId());
            }
        }
        return null;
    }

    public Map<UUID, Member> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public void setLastDoorEnter(Instant lastDoorEnter) {
        this.lastDoorEnter = lastDoorEnter;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public boolean hasAccess(Player player) {
        return isMember(player.getUniqueId());
    }

    public void expelGuests(boolean all) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInBounds(player.getLocation()) && (!hasAccess(player) || all)) {
                exit(player);
                UltimateRifts.getInstance().getLocale().getMessage("event.exit.expelled")
                        .sendPrefixedMessage(player);
            }
        }
    }

    public void destroy() {
        // Expel all guests
        expelGuests(true);

        // Remove the rift doors
        removeDoubleDoor(riftDoor.getBlock());
        if (placedDoor != null)
            removeDoubleDoor(placedDoor.getBlock());

        // Fill in the rift area
        fillRiftArea();

        // Remove the rift from the database
        delete();
    }

    private void fillRiftArea() {
        Location center = getCenter();
        int radius = level.getSize() / 2;
        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;
        int minY = center.getBlockY();
        int maxY = center.getBlockY() + Settings.RIFT_HEIGHT.getInt();

        com.sk89q.worldedit.world.World worldEditWorld = BukkitAdapter.adapt(center.getWorld());
        BlockVector3 min = BlockVector3.at(minX, minY, minZ);
        BlockVector3 max = BlockVector3.at(maxX, maxY, maxZ);
        CuboidRegion region = new CuboidRegion(worldEditWorld, min, max);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(worldEditWorld)) {
            // Fill the rift area with air
            try {
                editSession.setBlocks(region, BlockTypes.STONE.getDefaultState());
            } catch (MaxChangedBlocksException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isFirstCarve() {
        return firstCarve;
    }

    public boolean canBreak(Location location) {
        if (!isInBounds(location))
            return false;
        Block block = location.getBlock();
        return block.getLocation().getY() != 1 || block.getRelative(BlockFace.DOWN).getType() != Material.BEDROCK;
    }
}