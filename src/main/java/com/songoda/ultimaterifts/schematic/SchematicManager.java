package com.songoda.ultimaterifts.schematic;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.songoda.ultimaterifts.UltimateRifts;
import org.bukkit.Location;

import java.io.*;
import java.nio.file.Files;

public class SchematicManager {
    private final UltimateRifts plugin;

    public SchematicManager(UltimateRifts plugin) {
        this.plugin = plugin;

        // Copy default schematics from resources to schematics folder
        File schematicFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicFolder.exists()) {
            schematicFolder.mkdirs();
        }

        try {
            String[] defaultSchematics = {"1.schem", "2.schem", "3.schem"}; // Add the names of your default schematics
            for (String schematicName : defaultSchematics) {
                File destinationFile = new File(schematicFolder, schematicName);
                if (!destinationFile.exists()) {
                    try (InputStream inputStream = getClass().getResourceAsStream("/schematics/" + schematicName)) {
                        Files.copy(inputStream, destinationFile.toPath());
                        plugin.getLogger().info("Copied default schematic: " + schematicName);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to copy default schematics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveSchematic(Region region, World world, int level) {
        File schematicFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicFolder.exists()) {
            schematicFolder.mkdirs();
        }

        File schematicFile = new File(schematicFolder, level + ".schem");

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(region.getMinimumPoint());

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
            forwardExtentCopy.setCopyingEntities(true);
            Operations.complete(forwardExtentCopy);
        } catch (WorldEditException e) {
            plugin.getLogger().severe("Failed to save schematic for level " + level);
            e.printStackTrace();
            return;
        }

        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(schematicFile))) {
            writer.write(clipboard);
            plugin.getLogger().info("Schematic saved for level " + level);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save schematic for level " + level);
            e.printStackTrace();
        }
    }

    public boolean pasteSchematic(int level, Location location) {
        File schematicFolder = new File(plugin.getDataFolder(), "schematics");
        File schematicFile = new File(schematicFolder, level + ".schem");

        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Schematic not found for level " + level);
            return false;
        }

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(location.getWorld());
        Clipboard clipboard;
        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(new FileInputStream(schematicFile))) {
            clipboard = reader.read();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load schematic for level " + level);
            e.printStackTrace();
            return false;
        }

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(location.getX() - (clipboard.getDimensions().getBlockX() / 2),
                            location.getY() - 1,
                            location.getZ() - (clipboard.getDimensions().getBlockZ() / 2))
                    )
                    .ignoreAirBlocks(false)
                    .build();
            Operations.complete(operation);
            return true;
        } catch (WorldEditException e) {
            plugin.getLogger().severe("Error pasting schematic: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}