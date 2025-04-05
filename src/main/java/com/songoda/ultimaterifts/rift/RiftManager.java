package com.songoda.ultimaterifts.rift;

import com.songoda.core.data.LoadsData;
import com.songoda.core.data.SQLSelect;
import com.songoda.third_party.org.jooq.DSLContext;
import com.songoda.third_party.org.jooq.impl.DSL;
import com.songoda.third_party.org.jooq.impl.SQLDataType;
import com.songoda.ultimaterifts.UltimateRifts;
import com.songoda.ultimaterifts.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;

public class RiftManager implements LoadsData {

    private final Map<Integer, Rift> rifts;

    public RiftManager() {
        rifts = new HashMap<>();
    }

    public void addRift(Rift rift) {
        rifts.put(rift.getRiftId(), rift);
    }

    public void removeRift(Rift rift) {
        rifts.remove(rift.getRiftId());
    }

    public Rift getRiftById(int riftId) {
        return rifts.get(riftId);
    }

    public Rift getRiftByDoor(Location doorLocation) {
        for (Rift rift : rifts.values()) {
            if (isDoorLocation(doorLocation, rift.getRiftDoor()) ||
                    isDoorLocation(doorLocation, rift.getPlacedDoor())) {
                return rift;
            }
        }
        return null;
    }

    public static boolean isDoorLocation(Location location1, Location location2) {
        if (location1 == null || location2 == null) {
            return false;
        }
        return location1.getBlockX() == location2.getBlockX() &&
                (location1.getBlockY() == location2.getBlockY() ||
                        location1.getBlockY() == location2.getBlockY() - 1) &&
                location1.getBlockZ() == location2.getBlockZ();
    }

    public Rift getRiftByLocation(Location location) {
        for (Rift rift : rifts.values()) {
            Location center = rift.getCenter();
            int riftSize = Settings.RIFT_SIZE.getInt();
            int riftRadius = riftSize / 2;
            int minX = center.getBlockX() - riftRadius;
            int maxX = center.getBlockX() + riftRadius;
            int minZ = center.getBlockZ() - riftRadius;
            int maxZ = center.getBlockZ() + riftRadius;

            if (location.getWorld().equals(center.getWorld()) &&
                    location.getBlockX() >= minX && location.getBlockX() <= maxX &&
                    location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ) {
                return rift;
            }
        }
        return null;
    }

    public Collection<Rift> getRifts() {
        return Collections.unmodifiableCollection(rifts.values());
    }

    public Rift getOrCreateRiftById(Integer riftId) {
        Rift rift = getRiftById(riftId);
        if (rift == null) {
            rift = new Rift(riftId);
            addRift(rift);
        }
        return rift;
    }

    public int getNextAvailableRiftId() {
        int id = 1;
        while (getRiftById(id) != null)
            id++;
        return id;
    }

    @Override
    public void loadDataImpl(DSLContext ctx) {
        SQLSelect.create(ctx).select("rift_id", "rift_door_world", "rift_door_x", "rift_door_y", "rift_door_z",
                        "placed_door_world", "placed_door_x", "placed_door_y", "placed_door_z", "level",
                        "first_carve", "potion_effect", "last_door_enter", "is_locked", "wallpaper", "floor")
                .from("rift", result -> {
                    int riftId = result.get("rift_id").asInt();
                    World riftDoorWorld = Bukkit.getWorld(result.get("rift_door_world").asString());
                    int riftDoorX = result.get("rift_door_x").asInt();
                    int riftDoorY = result.get("rift_door_y").asInt();
                    int riftDoorZ = result.get("rift_door_z").asInt();
                    Location riftDoor = new Location(riftDoorWorld, riftDoorX, riftDoorY, riftDoorZ);
                    World placedDoorWorld = Bukkit.getWorld(result.get("placed_door_world").asString());
                    int placedDoorX = result.get("placed_door_x").asInt();
                    int placedDoorY = result.get("placed_door_y").asInt();
                    int placedDoorZ = result.get("placed_door_z").asInt();
                    Location placedDoor = new Location(placedDoorWorld, placedDoorX, placedDoorY, placedDoorZ);
                    int level = result.get("level").asInt();
                    boolean firstCarve = result.get("first_carve").asBoolean();
                    boolean potionEffect = result.get("potion_effect").asBoolean();
                    boolean isLocked = result.get("is_locked").asBoolean();

                    String wallpaper = result.get("wallpaper").asString();

                    Instant lastDoorEnter = result.get("last_door_enter").asInstant();

                    Rift rift = new Rift(riftId);
                    rift.setRiftDoor(riftDoor);
                    rift.setPlacedDoor(placedDoor);
                    rift.setLevel(UltimateRifts.getInstance().getLevelManager().getLevel(level));
                    rift.setFirstCarve(firstCarve);
                    rift.setPotionEffect(potionEffect);
                    rift.setLastDoorEnter(lastDoorEnter);
                    rift.setLocked(isLocked);

                    Material[] wallpaperMaterials = new Material[9];
                    int i = 0;
                    for (String materialStr : wallpaper.split(",")) {
                        Material material = Material.getMaterial(materialStr);
                        if (material != null) {
                            if (!material.isAir())
                                wallpaperMaterials[i] = material;
                        }
                        i++;
                    }
                    rift.setWallpaper(wallpaperMaterials);

                    String floor = result.get("floor").asString();

                    Material[] floorMaterials = new Material[9];
                    i = 0;
                    for (String materialStr : floor.split(",")) {
                        Material material = Material.getMaterial(materialStr);
                        if (material != null) {
                            if (!material.isAir())
                                floorMaterials[i] = material;
                        }
                        i++;
                    }
                    rift.setFloor(floorMaterials);

                    addRift(rift);
                });

        SQLSelect.create(ctx).select("rift_id", "player_id", "is_owner", "timestamp")
                .from("rift_members", result -> {
                    int riftId = result.get("rift_id").asInt();
                    UUID playerId = UUID.fromString(result.get("player_id").asString());
                    boolean isOwner = result.get("is_owner").asBoolean();
                    long timestamp = result.get("timestamp").asLong();

                    Rift rift = getRiftById(riftId);
                    if (rift != null) {
                        Member member = rift.addMember(playerId);
                        member.setOwner(isOwner);
                        member.setTimestamp(timestamp);
                    }
                });
    }

    @Override
    public void setupTables(DSLContext ctx) {
        ctx.createTableIfNotExists("rift")
                .column("rift_id", SQLDataType.INTEGER.nullable(false))
                .column("rift_door_world", SQLDataType.VARCHAR(36).nullable(false))
                .column("rift_door_x", SQLDataType.INTEGER.nullable(false))
                .column("rift_door_y", SQLDataType.INTEGER.nullable(false))
                .column("rift_door_z", SQLDataType.INTEGER.nullable(false))
                .column("placed_door_world", SQLDataType.VARCHAR(36).nullable(false))
                .column("placed_door_x", SQLDataType.INTEGER.nullable(false))
                .column("placed_door_y", SQLDataType.INTEGER.nullable(false))
                .column("placed_door_z", SQLDataType.INTEGER.nullable(false))
                .column("level", SQLDataType.INTEGER.nullable(false))
                .column("first_carve", SQLDataType.BOOLEAN.nullable(false))
                .column("potion_effect", SQLDataType.BOOLEAN.nullable(false))
                .column("owner", SQLDataType.VARCHAR(36).nullable(true))
                .column("last_door_enter", SQLDataType.BIGINT.nullable(true))
                .column("is_locked", SQLDataType.BOOLEAN.nullable(false).defaultValue(false))
                .constraint(DSL.constraint().primaryKey("rift_id"))
                .execute();

        ctx.alterTable("rift")
                .addColumnIfNotExists("wallpaper", SQLDataType.VARCHAR(255).nullable(false).defaultValue(""))
                .execute();

        ctx.alterTable("rift")
                .addColumnIfNotExists("floor", SQLDataType.VARCHAR(255).nullable(false).defaultValue(""))
                .execute();

        ctx.createTableIfNotExists("rift_members")
                .column("rift_id", SQLDataType.INTEGER.nullable(false))
                .column("player_id", SQLDataType.VARCHAR(36).nullable(false))
                .column("is_owner", SQLDataType.BOOLEAN.nullable(false))
                .column("timestamp", SQLDataType.BIGINT.nullable(false))
                .constraint(DSL.constraint().primaryKey("rift_id", "player_id"))
                .execute();
    }

    public List<Rift> getOwnedRifts(Player player) {
        List<Rift> ownedRifts = new ArrayList<>();
        for (Rift rift : rifts.values()) {
            if (rift.getOwner() != null && rift.getOwner().getUniqueId().equals(player.getUniqueId())) {
                ownedRifts.add(rift);
            }
        }
        return ownedRifts;
    }

    public List<Rift> getAssociatedRifts(Player player) {
        List<Rift> associatedRifts = new ArrayList<>();
        for (Rift rift : rifts.values())
            if (rift.isMember(player.getUniqueId()))
                associatedRifts.add(rift);
        return associatedRifts;
    }
}