package com.yourfault.map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StructurePlacementHelper {
    private final JavaPlugin plugin;
    private final StructureManager structureManager;
    private final Logger logger;
    private final Map<String, LoadedStructure> cache = new HashMap<>();

    public StructurePlacementHelper(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();
        this.structureManager = plugin.getServer().getStructureManager();
    }

    public boolean hasStructure(String resourcePath) {
        return load(resourcePath).isPresent();
    }

    public BlockVector getStructureSize(String resourcePath) {
        return load(resourcePath)
                .map(LoadedStructure::size)
                .map(BlockVector::clone)
                .orElse(null);
    }

    public Optional<Structure> loadStructureTemplate(String resourcePath) {
        return load(resourcePath).map(LoadedStructure::structure);
    }

    public int estimateFootprintRadius(BlockVector size, int fallbackRadius) {
        if (size == null) {
            return Math.max(1, fallbackRadius);
        }
        return Math.max(1, Math.max(size.getBlockX(), size.getBlockZ()) / 2 + 1);
    }

    public boolean placeStructure(String resourcePath,
                                  World world,
                                  int centerX,
                                  int baseY,
                                  int centerZ,
                                  Random random,
                                  boolean includeEntities,
                                  MapTheme.StructureTemplate.Rotation rotationOption) {
        if (world == null) {
            return false;
        }
        Optional<LoadedStructure> template = load(resourcePath);
        if (template.isEmpty()) {
            return false;
        }
        BlockVector size = template.get().size();
        StructureRotation rotation = toBukkitRotation(rotationOption);
        BlockVector rotatedSize = rotatedSize(size, rotation);
        int offsetX = rotatedSize.getBlockX() / 2;
        int offsetZ = rotatedSize.getBlockZ() / 2;
        Location origin = new Location(world, centerX - offsetX, baseY, centerZ - offsetZ);
        template.get().structure().place(
                origin,
                includeEntities,
                rotation,
                Mirror.NONE,
                0,
                1.0f,
                random
        );
        return true;
    }

    private StructureRotation toBukkitRotation(MapTheme.StructureTemplate.Rotation rotation) {
        if (rotation == null) {
            return StructureRotation.NONE;
        }
        switch (rotation) {
            case CLOCKWISE_90:
                return StructureRotation.CLOCKWISE_90;
            case CLOCKWISE_180:
                return StructureRotation.CLOCKWISE_180;
            case COUNTERCLOCKWISE_90:
                return StructureRotation.COUNTERCLOCKWISE_90;
            default:
                return StructureRotation.NONE;
        }
    }

    private BlockVector rotatedSize(BlockVector size, StructureRotation rotation) {
        if (size == null) {
            return new BlockVector(1, 1, 1);
        }
        int width = Math.max(1, size.getBlockX());
        int height = Math.max(1, size.getBlockY());
        int depth = Math.max(1, size.getBlockZ());
        if (rotation == StructureRotation.CLOCKWISE_90 || rotation == StructureRotation.COUNTERCLOCKWISE_90) {
            return new BlockVector(depth, height, width);
        }
        return size.clone();
    }

    private Optional<LoadedStructure> load(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return Optional.empty();
        }
        LoadedStructure cached = cache.get(resourcePath);
        if (cached != null) {
            return Optional.of(cached);
        }
        if (structureManager == null) {
            return Optional.empty();
        }
        try (InputStream input = plugin.getResource(resourcePath)) {
            if (input == null) {
                logger.log(Level.WARNING, "Could not find structure resource: {0}", resourcePath);
                return Optional.empty();
            }
            Structure structure = structureManager.loadStructure(input);
            BlockVector size = structure.getSize();
            LoadedStructure loaded = new LoadedStructure(structure, size.clone());
            cache.put(resourcePath, loaded);
            logger.info(String.format(
                    "Loaded structure template '%s' (%d x %d x %d)",
                    resourcePath,
                    size.getBlockX(),
                    size.getBlockY(),
                    size.getBlockZ()
            ));
            return Optional.of(loaded);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to load structure resource " + resourcePath, ex);
            return Optional.empty();
        }
    }

    private record LoadedStructure(Structure structure, BlockVector size) {
    }
}
