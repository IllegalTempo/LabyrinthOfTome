package com.yourfault.map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.Material;
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
            logger.log(Level.WARNING, "placeStructure called with null world for resource {0}", resourcePath);
            return false;
        }
        logger.log(Level.INFO, "placeStructure start: resource={0} at {1},{2},{3}", new Object[]{resourcePath, centerX, baseY, centerZ});
        Optional<LoadedStructure> template = load(resourcePath);
        if (template.isEmpty()) {
            logger.log(Level.WARNING, "Template not found: {0}", resourcePath);
            return false;
        }
        BlockVector size = template.get().size();
        StructureRotation rotation = toBukkitRotation(rotationOption);
        BlockVector rotatedSize = rotatedSize(size, rotation);
        int offsetX = rotatedSize.getBlockX() / 2;
        int offsetZ = rotatedSize.getBlockZ() / 2;
        Location origin = new Location(world, centerX - offsetX, baseY, centerZ - offsetZ);

        // Snapshot non-air blocks inside the structure footprint so AIR in the template doesn't erase them.
        Map<String, BlockData> preExisting = new HashMap<>();
        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();
        int rx = Math.max(1, rotatedSize.getBlockX());
        int ry = Math.max(1, rotatedSize.getBlockY());
        int rz = Math.max(1, rotatedSize.getBlockZ());
        for (int dx = 0; dx < rx; dx++) {
            for (int dy = 0; dy < ry; dy++) {
                int wy = oy + dy;
                if (wy < world.getMinHeight() || wy > world.getMaxHeight()) continue;
                for (int dz = 0; dz < rz; dz++) {
                    int wx = ox + dx;
                    int wz = oz + dz;
                    org.bukkit.block.Block b = world.getBlockAt(wx, wy, wz);
                    if (b != null && !b.isEmpty() && b.getType() != Material.AIR) {
                        preExisting.put(wx + ":" + wy + ":" + wz, b.getBlockData().clone());
                    }
                }
            }
        }
        logger.log(Level.INFO, "placeStructure: origin={0},{1},{2} rotatedSize={3}x{4}x{5} preExistingCount={6}",
                new Object[]{ox, oy, oz, rx, ry, rz, preExisting.size()});

        try {
            template.get().structure().place(
                    origin,
                    includeEntities,
                    rotation,
                    Mirror.NONE,
                    0,
                    1.0f,
                    random
            );
        } catch (Exception ex) {
            logger.log(Level.WARNING, "structure.place threw for {0} at {1},{2},{3}: {4}", new Object[]{resourcePath, ox, oy, oz, ex.toString()});
            return false;
        }

        // Restore any pre-existing non-air blocks that were replaced by AIR during placement
        int restored = 0;
        for (Map.Entry<String, BlockData> e : preExisting.entrySet()) {
            String[] parts = e.getKey().split(":");
            int wx = Integer.parseInt(parts[0]);
            int wy = Integer.parseInt(parts[1]);
            int wz = Integer.parseInt(parts[2]);
            if (wy < world.getMinHeight() || wy > world.getMaxHeight()) continue;
            org.bukkit.block.Block after = world.getBlockAt(wx, wy, wz);
            if (shouldRestore(after)) {
                after.setBlockData(e.getValue().clone(), false);
                restored++;
            }
        }
        logger.log(Level.INFO, "placeStructure finished: resource={0} restored={1}", new Object[]{resourcePath, restored});

        return true;
    }

    private boolean shouldRestore(org.bukkit.block.Block block) {
        if (block == null) {
            return true;
        }
        Material type = block.getType();
        return type.isAir() || type == Material.STRUCTURE_VOID;
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
