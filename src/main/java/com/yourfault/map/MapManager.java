package com.yourfault.map;

import com.yourfault.system.Game;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Generates and clears PvE arenas that leverage Perlin noise heightmaps and biome themes.
 */
public class MapManager {
    private final JavaPlugin plugin;
    private final Game game;
    private final Random random = new Random();
    private PerlinNoise noise;

    private final Map<String, BlockSnapshot> modifiedBlocks = new HashMap<>();
    private final Map<Long, Integer> surfaceHeights = new HashMap<>();
    private final List<Location> spawnMarkerLocations = new ArrayList<>();

    private World activeWorld;
    private Location activeCenter;
    private MapTheme lastTheme;
    private int lastRadius;
    private double noiseOffsetX;
    private double noiseOffsetZ;

    public MapManager(JavaPlugin plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
        this.noise = new PerlinNoise(System.currentTimeMillis());
    }

    public synchronized boolean hasActiveMap() {
        return !modifiedBlocks.isEmpty();
    }

    public synchronized MapGenerationSummary generateMap(Location center) {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(center.getWorld(), "World cannot be null");

        if (hasActiveMap()) {
            throw new IllegalStateException("A generated map already exists. Run /clearmap first.");
        }

        activeWorld = center.getWorld();
        activeCenter = center.clone();
        surfaceHeights.clear();
        spawnMarkerLocations.clear();
        modifiedBlocks.clear();

        noise = new PerlinNoise(random.nextLong());
        noiseOffsetX = random.nextDouble() * 10_000d;
        noiseOffsetZ = random.nextDouble() * 10_000d;

        int playerCount = Math.max(1, Math.max(game.GetPlayerCount(), activeWorld.getPlayers().size()));
        int radius = computeRadius(playerCount);
        MapTheme theme = MapTheme.pickRandom(random);

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        int radiusSquared = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > radiusSquared) {
                    continue;
                }

                double normalizedDistance = Math.sqrt(distanceSquared) / radius;
                boolean isEdge = normalizedDistance > 0.9;
                int x = centerX + dx;
                int z = centerZ + dz;

                int surfaceY = calculateSurfaceY(centerY, x, z, normalizedDistance, theme);
                buildColumn(x, z, surfaceY, theme, isEdge);
            }
        }

        placeSpawnMarkers(playerCount, radius);

        lastTheme = theme;
        lastRadius = radius;

        plugin.getLogger().info(String.format("Generated %s PvE map (radius=%d, blocks=%d)", theme.name(), radius, modifiedBlocks.size()));

        return new MapGenerationSummary(theme, radius, modifiedBlocks.size(), spawnMarkerLocations.size());
    }

    public synchronized int clearMap() {
        int restoredBlocks = 0;
        for (BlockSnapshot snapshot : modifiedBlocks.values()) {
            snapshot.restore();
            restoredBlocks++;
        }
        modifiedBlocks.clear();
        surfaceHeights.clear();
        spawnMarkerLocations.clear();
        activeWorld = null;
        activeCenter = null;
        lastTheme = null;
        lastRadius = 0;
        return restoredBlocks;
    }

    public synchronized MapTheme getLastTheme() {
        return lastTheme;
    }

    public synchronized int getLastRadius() {
        return lastRadius;
    }

    private int computeRadius(int playerCount) {
        int minRadius = 24;
        int maxRadius = 80;
        return Math.max(minRadius, Math.min(maxRadius, 18 + playerCount * 6));
    }

    private int calculateSurfaceY(int baseY, int x, int z, double normalizedDistance, MapTheme theme) {
        double falloff = 1.0 - Math.min(1.0, normalizedDistance * normalizedDistance);
        double effectiveFalloff = adjustFalloffForProfile(theme, falloff);
        double noiseSample = noise.sample((x + noiseOffsetX) * theme.getNoiseScale(), (z + noiseOffsetZ) * theme.getNoiseScale());
        double profileBonus = computeProfileBonus(theme, normalizedDistance, noiseSample);
        int heightOffset = (int) Math.round(noiseSample * theme.getHeightVariance() * effectiveFalloff + profileBonus);
        int rawHeight = baseY + heightOffset;
        int minHeight = Math.max(activeWorld.getMinHeight() + 5, baseY - 6);
        int maxBoost = theme.getTerrainProfile() == MapTheme.TerrainProfile.REAL_MOUNTAIN ? 18 : 10;
        int maxHeight = Math.min(activeWorld.getMaxHeight() - 6, baseY + maxBoost);
        int surfaceY = Math.max(minHeight, Math.min(maxHeight, rawHeight));
        surfaceY = enforceMountainRim(baseY, surfaceY, normalizedDistance, maxHeight, theme);
        surfaceHeights.put(columnKey(x, z), surfaceY);
        return surfaceY;
    }

    private int enforceMountainRim(int baseY, int currentHeight, double normalizedDistance, int maxHeight, MapTheme theme) {
        if (normalizedDistance < 0.65 || activeWorld == null) {
            return currentHeight;
        }
        if (theme.getTerrainProfile() == MapTheme.TerrainProfile.REAL_MOUNTAIN) {
            double rimFactor = Math.min(1.0, Math.max(0.0, (normalizedDistance - 0.65) / 0.35));
            int baseline = baseY + 8;
            double randomBoost = 2 + random.nextDouble() * 2;
            int variable = (int) Math.round((6 + randomBoost) * rimFactor);
            int target = baseline + variable;
            target = Math.min(target, maxHeight - 1);
            return Math.max(currentHeight, target);
        }

        double rimFactor = Math.min(1.0, Math.max(0.0, (normalizedDistance - 0.7) / 0.3));
        if (rimFactor <= 0.0) {
            return currentHeight;
        }
        int baseline = baseY + 2;
        int target = baseline + (int) Math.round(rimFactor * 3);
        target = Math.min(target, maxHeight - 1);
        return Math.max(currentHeight, target);
    }

    private void buildColumn(int x, int z, int surfaceY, MapTheme theme, boolean edge) {
        if (activeWorld == null) {
            return;
        }
        int fillerDepth = theme.getFillerDepth();
        int minHeight = Math.max(activeWorld.getMinHeight(), surfaceY - fillerDepth - 2);
        for (int y = minHeight; y <= surfaceY; y++) {
            Material material;
            if (y == surfaceY) {
                material = edge ? theme.getBorderMaterial() : theme.getTopMaterial();
            } else if (y >= surfaceY - fillerDepth) {
                material = theme.getFillerMaterial();
            } else {
                material = Material.STONE;
            }
            setBlock(activeWorld, x, y, z, material);
        }

        clearAboveSurface(x, surfaceY + 1, z);
        maybePlaceDecoration(x, surfaceY + 1, z, theme, edge);
        applyEdgeAccent(x, surfaceY, z, theme, edge);
    }

    private double adjustFalloffForProfile(MapTheme theme, double falloff) {
        if (theme.getTerrainProfile() == MapTheme.TerrainProfile.REAL_MOUNTAIN) {
            return Math.max(0.45, falloff);
        }
        return falloff;
    }

    private double computeProfileBonus(MapTheme theme, double normalizedDistance, double noiseSample) {
        if (theme.getTerrainProfile() != MapTheme.TerrainProfile.REAL_MOUNTAIN) {
            return 0.0;
        }
        double clamped = Math.min(1.0, Math.max(0.0, normalizedDistance));
        double ridge = Math.cos(clamped * (Math.PI / 2.0));
        double undulation = Math.abs(Math.sin((clamped * 3.0) + (noiseSample * Math.PI)));
        double harmonic = Math.abs(Math.sin(clamped * Math.PI * 4.0));
        double centerLift = (1.0 - clamped) * 3.0;
        return ridge * 7.0 + undulation * 4.0 + harmonic * 2.5 + centerLift;
    }

    private void maybePlaceDecoration(int x, int y, int z, MapTheme theme, boolean edge) {
        if (edge) {
            return;
        }
        if (random.nextDouble() > theme.getDecorationChance()) {
            return;
        }
        Material candidate = theme.pickRandomDecoration(random);
        if (candidate == null) {
            return;
        }
        setBlock(activeWorld, x, y, z, candidate);
    }

    private void applyEdgeAccent(int x, int surfaceY, int z, MapTheme theme, boolean edge) {
        if (!edge) {
            return;
        }
        if (theme.getTerrainProfile() != MapTheme.TerrainProfile.REAL_MOUNTAIN) {
            return;
        }
        Material accent = theme.getAccentMaterial();
        if (accent == null || accent == Material.AIR) {
            return;
        }
        setBlock(activeWorld, x, surfaceY + 1, z, accent);
    }

    private void clearAboveSurface(int x, int y, int z) {
        if (activeWorld == null) {
            return;
        }
        if (y > activeWorld.getMaxHeight()) {
            return;
        }
        Block block = activeWorld.getBlockAt(x, y, z);
        if (!block.isEmpty()) {
            setBlock(activeWorld, x, y, z, Material.AIR);
        }
    }

    private void placeSpawnMarkers(int playerCount, int radius) {
        if (activeWorld == null || activeCenter == null) {
            return;
        }
        double markerRadius = radius * 0.65;
        int markerCount = Math.max(4, playerCount * 2);
        double angleStep = (Math.PI * 2) / markerCount;
        for (int i = 0; i < markerCount; i++) {
            double angle = angleStep * i;
            int x = activeCenter.getBlockX() + (int) Math.round(markerRadius * Math.cos(angle));
            int z = activeCenter.getBlockZ() + (int) Math.round(markerRadius * Math.sin(angle));
            Integer surfaceY = surfaceHeights.get(columnKey(x, z));
            if (surfaceY == null) {
                surfaceY = findNearestSurfaceY(x, z, activeCenter.getBlockY());
            }
            if (surfaceY == null) {
                continue;
            }
            int markerY = surfaceY + 1;
            setBlock(activeWorld, x, markerY, z, Material.YELLOW_WOOL);
            spawnMarkerLocations.add(new Location(activeWorld, x, markerY, z));
        }
    }

    private Integer findNearestSurfaceY(int x, int z, int referenceY) {
        if (activeWorld == null) {
            return null;
        }
        int maxUp = Math.min(activeWorld.getMaxHeight(), referenceY + 8);
        int minDown = Math.max(activeWorld.getMinHeight(), referenceY - 12);
        for (int y = maxUp; y >= minDown; y--) {
            Block block = activeWorld.getBlockAt(x, y, z);
            if (!block.isEmpty() && block.getType().isSolid()) {
                return y;
            }
        }
        return null;
    }

    private void setBlock(World world, int x, int y, int z, Material material) {
        if (world == null) {
            return;
        }
        if (y < world.getMinHeight() || y > world.getMaxHeight()) {
            return;
        }
        Block block = world.getBlockAt(x, y, z);
        String key = blockKey(world.getUID(), x, y, z);
        if (!modifiedBlocks.containsKey(key)) {
            modifiedBlocks.put(key, new BlockSnapshot(world, x, y, z, block.getBlockData().clone()));
        }
        block.setType(material, false);
    }

    private static String blockKey(UUID worldId, int x, int y, int z) {
        return worldId + ":" + x + ":" + y + ":" + z;
    }

    private static long columnKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private static class BlockSnapshot {
        private final World world;
        private final int x;
        private final int y;
        private final int z;
        private final BlockData blockData;

        private BlockSnapshot(World world, int x, int y, int z, BlockData blockData) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockData = blockData;
        }

        private void restore() {
            Block block = world.getBlockAt(x, y, z);
            block.setBlockData(blockData, false);
        }
    }
}
