package com.yourfault.map;

import com.yourfault.system.Game;
import com.yourfault.utils.PerlinNoise;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.Set;

/**
 * Generates and clears PvE arenas that leverage Perlin noise heightmaps and biome themes.
 */
public class MapManager {
    private final JavaPlugin plugin;
    private final Game game;
    private final Random random = new Random();
    private final StructurePlacementHelper structureHelper;
    private PerlinNoise noise;
    private static final int GENERATION_SLICE_INTERVAL_TICKS = 10; // 0.5 seconds
    private static final int GENERATION_SLICE_WIDTH = 4;
    private static final int CLEAR_SLICE_INTERVAL_TICKS = 10;
    private static final int CLEAR_COLUMNS_PER_SLICE = 256;
    private static final int CLEAR_RADIUS_PADDING = 6;
    private static final double MIN_SPAWN_MARKER_SPACING = 6.0;

    private final Set<String> touchedBlocks = new HashSet<>();
    private final Map<Long, Integer> surfaceHeights = new HashMap<>();
    private final List<Location> spawnMarkerLocations = new ArrayList<>();
    private final Set<Long> reservedStructureColumns = new HashSet<>();

    private World activeWorld;
    private Location activeCenter;
    private MapTheme lastTheme;
    private int lastRadius;
    private double noiseOffsetX;
    private double noiseOffsetZ;
    private GenerationSession activeGeneration;
    private ClearSession activeClear;
    private int regionMinY;
    private int regionMaxY;

    public MapManager(JavaPlugin plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
        this.noise = new PerlinNoise(System.currentTimeMillis());
        this.structureHelper = new StructurePlacementHelper(plugin);
    }

    public synchronized boolean hasActiveMap() {
        return activeCenter != null && activeGeneration == null;
    }

    public synchronized boolean isGenerationRunning() {
        return activeGeneration != null;
    }

    public synchronized boolean isClearingRunning() {
        return activeClear != null;
    }

    public synchronized void generateMapAsync(Location center,
                                              Consumer<MapGenerationSummary> onComplete,
                                              Consumer<String> onError) {
        generateMapAsync(center, null, onComplete, onError);
    }

    public synchronized void generateMapAsync(Location center,
                                              MapTheme requestedTheme,
                                              Consumer<MapGenerationSummary> onComplete,
                                              Consumer<String> onError) {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(center.getWorld(), "World cannot be null");
        Objects.requireNonNull(onComplete, "onComplete");
        Objects.requireNonNull(onError, "onError");

        if (activeGeneration != null) {
            onError.accept("Map generation is already in progress.");
            return;
        }
        if (activeClear != null) {
            onError.accept("Map clearing is in progress. Please wait.");
            return;
        }
        if (hasActiveMap()) {
            onError.accept("A generated map already exists. Run /clearmap first.");
            return;
        }

        activeWorld = center.getWorld();
        activeCenter = center.clone();
        surfaceHeights.clear();
        spawnMarkerLocations.clear();
        touchedBlocks.clear();
        reservedStructureColumns.clear();
        regionMinY = Integer.MAX_VALUE;
        regionMaxY = Integer.MIN_VALUE;

        noise = new PerlinNoise(random.nextLong());
        noiseOffsetX = random.nextDouble() * 10_000d;
        noiseOffsetZ = random.nextDouble() * 10_000d;

        int playerCount = Math.max(1, Math.max(game.GetPlayerCount(), activeWorld.getPlayers().size()));
        int radius = computeRadius(playerCount);
        MapTheme theme = requestedTheme != null ? requestedTheme : MapTheme.pickRandom(random);

        GenerationSession session = new GenerationSession(
                theme,
                radius,
                center.getBlockY(),
                playerCount,
                onComplete,
                onError
        );
        activeGeneration = session;
        session.runTaskTimer(plugin, 1L, GENERATION_SLICE_INTERVAL_TICKS);
    }

    public synchronized void clearMapAsync(Consumer<Integer> onComplete, Consumer<String> onError) {
        Objects.requireNonNull(onComplete, "onComplete");
        Objects.requireNonNull(onError, "onError");

        if (activeClear != null) {
            onError.accept("Map clearing is already running.");
            return;
        }
        if (activeGeneration != null) {
            onError.accept("Cannot clear while map generation is running.");
            return;
        }
        if (!hasActiveMap() || activeWorld == null || activeCenter == null) {
            onError.accept("There is no map to clear.");
            return;
        }

        int radius = Math.max(8, lastRadius + CLEAR_RADIUS_PADDING);
        List<ColumnCoordinate> columns = buildClearColumns(radius);
        if (columns.isEmpty()) {
            resetRegionState();
            onComplete.accept(0);
            return;
        }

        int minY = regionMinY == Integer.MAX_VALUE ? Math.max(activeWorld.getMinHeight(), activeCenter.getBlockY() - 8) : regionMinY;
        int maxY = regionMaxY == Integer.MIN_VALUE ? Math.min(activeWorld.getMaxHeight(), activeCenter.getBlockY() + 24) : regionMaxY;
        minY = Math.max(activeWorld.getMinHeight(), minY);
        maxY = Math.min(activeWorld.getMaxHeight(), maxY);

        ClearSession session = new ClearSession(columns, minY, maxY, onComplete, onError);
        activeClear = session;
        session.runTaskTimer(plugin, 1L, CLEAR_SLICE_INTERVAL_TICKS);
    }

    public synchronized MapTheme getLastTheme() {
        return lastTheme;
    }

    public synchronized int getLastRadius() {
        return lastRadius;
    }

    public synchronized List<Location> getSpawnMarkers() {
        List<Location> copy = new ArrayList<>(spawnMarkerLocations.size());
        for (Location marker : spawnMarkerLocations) {
            copy.add(marker.clone());
        }
        return copy;
    }

    private int computeRadius(int playerCount) {
        int minRadius = 30;
        int maxRadius = 94;
        double base = 24 + playerCount * 5.5;
        if (playerCount <= 3) {
            base += 6;
        }
        if (playerCount >= 10) {
            base += 6;
        }
        return Math.max(minRadius, Math.min(maxRadius, (int) Math.round(base)));
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
        regionMinY = Math.min(regionMinY, minHeight);
        regionMaxY = Math.max(regionMaxY, surfaceY + 2);
        for (int y = minHeight; y <= surfaceY; y++) {
            Material material;
            if (y == surfaceY) {
                boolean useBorderMaterial = edge && theme.hasBorder();
                material = useBorderMaterial ? resolveBorderMaterial(theme) : theme.getTopMaterial();
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

    private Material resolveBorderMaterial(MapTheme theme) {
        if (theme == null) {
            return Material.STONE;
        }
        Material material = theme.getBorderMaterial();
        if (material == null || material == Material.AIR) {
            return Material.STONE;
        }
        return material;
    }

    private Material resolveTopCoverMaterial(MapTheme theme) {
        if (theme == null) {
            return Material.GLASS;
        }
        Material material = theme.getTopCoverMaterial();
        if (material == null || material == Material.AIR) {
            return Material.GLASS;
        }
        return material;
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
        if (!theme.hasBorder()) {
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
        spawnMarkerLocations.clear();
        int markerCount = Math.max(4, playerCount * 2);
        double innerRadius = radius * 0.35;
        double outerRadius = radius * 0.8;
        double spacing = MIN_SPAWN_MARKER_SPACING;
        while (spawnMarkerLocations.size() < markerCount && spacing >= 3.0) {
            int attempts = markerCount * 12;
            while (spawnMarkerLocations.size() < markerCount && attempts-- > 0) {
                double distance = innerRadius + random.nextDouble() * (outerRadius - innerRadius);
                double angle = random.nextDouble() * Math.PI * 2;
                int x = activeCenter.getBlockX() + (int) Math.round(distance * Math.cos(angle));
                int z = activeCenter.getBlockZ() + (int) Math.round(distance * Math.sin(angle));
                if (isStructureBlocked(x, z)) {
                    continue;
                }
                if (!isMarkerFarEnough(x, z, spacing)) {
                    continue;
                }
                Integer surfaceY = surfaceHeights.get(columnKey(x, z));
                if (surfaceY == null) {
                    surfaceY = findNearestSurfaceY(x, z, activeCenter.getBlockY());
                }
                if (surfaceY == null) {
                    continue;
                }
                int markerY = surfaceY + 1;
                setBlock(activeWorld, x, markerY, z, Material.YELLOW_WOOL);
                spawnMarkerLocations.add(new Location(activeWorld, x + 0.5, markerY, z + 0.5));
            }
            spacing -= 1.0;
        }
    }

    private boolean isMarkerFarEnough(int x, int z, double spacing) {
        double spacingSquared = spacing * spacing;
        double candidateX = x + 0.5;
        double candidateZ = z + 0.5;
        for (Location marker : spawnMarkerLocations) {
            if (marker.getWorld() != activeWorld) {
                continue;
            }
            double dx = marker.getX() - candidateX;
            double dz = marker.getZ() - candidateZ;
            if ((dx * dx) + (dz * dz) < spacingSquared) {
                return false;
            }
        }
        return true;
    }

    private void placePools(MapTheme theme, int radius) {
        if (activeWorld == null || activeCenter == null) {
            return;
        }
        boolean allowWater = theme.allowsWaterPools();
        boolean allowLava = theme.allowsLavaPools();
        if (!allowWater && !allowLava) {
            return;
        }
        int attempts = Math.max(1, radius / 20);
        if (allowWater) {
            spawnFluidPools(Material.WATER, attempts, radius, theme);
        }
        if (allowLava) {
            spawnFluidPools(Material.LAVA, Math.max(1, attempts / 2), radius, theme);
        }
    }

    private void placeWatchtowers(MapTheme theme, int radius) {
        if (activeWorld == null || activeCenter == null) {
            return;
        }
        MapTheme.StructureSettings structureSettings = theme.getStructureSettings();
        if (structureSettings == null || !structureSettings.enabled()) {
            return;
        }
        if (radius < 24) {
            return;
        }
        double spawnChance = 0.45;
        if (random.nextDouble() > spawnChance) {
            return;
        }

        MapTheme.StructureTemplate template = structureSettings.pickTemplate(random);
        if (template == null) {
            plugin.getLogger().log(Level.WARNING, "Structure settings provided no template for theme {0}", theme.name());
            return;
        }

        String resourcePath = template.resourcePath();
        if (resourcePath == null || resourcePath.isBlank()) {
            plugin.getLogger().log(Level.WARNING, "Structure template path is not set for theme {0}", theme.name());
            return;
        }
        if (!structureHelper.hasStructure(resourcePath)) {
            plugin.getLogger().log(Level.WARNING, "Structure resource {0} could not be loaded; skipping placement.", resourcePath);
            return;
        }

        List<Location> placed = new ArrayList<>(1);
        int attempts = 12;
        int safeRadiusSquared = (radius - 6) * (radius - 6);
        BlockVector templateSize = structureHelper.getStructureSize(resourcePath);
        int footprintRadius = structureHelper.estimateFootprintRadius(templateSize, template.fallbackFootprintRadius());
        int structureHeight = templateSize != null ? templateSize.getBlockY() : template.estimatedHeight();

        while (placed.isEmpty() && attempts-- > 0) {
            double distance = radius * (0.35 + random.nextDouble() * 0.4);
            double angle = random.nextDouble() * Math.PI * 2;
            int x = activeCenter.getBlockX() + (int) Math.round(distance * Math.cos(angle));
            int z = activeCenter.getBlockZ() + (int) Math.round(distance * Math.sin(angle));
            int dx = x - activeCenter.getBlockX();
            int dz = z - activeCenter.getBlockZ();
            if ((dx * dx) + (dz * dz) > safeRadiusSquared) {
                continue;
            }
            if (!isTowerFarEnough(placed, x + 0.5, z + 0.5, 12.0)) {
                continue;
            }
            Integer surfaceY = surfaceHeights.get(columnKey(x, z));
            if (surfaceY == null) {
                surfaceY = findNearestSurfaceY(x, z, activeCenter.getBlockY());
            }
            if (surfaceY == null) {
                continue;
            }
            if (surfaceY + 24 >= activeWorld.getMaxHeight()) {
                continue;
            }
            if (!canPlaceWatchtower(x, z, surfaceY, footprintRadius)) {
                continue;
            }

            boolean placedStructure = structureHelper.placeStructure(
                    resourcePath,
                    activeWorld,
                    x,
                    surfaceY + 1,
                    z,
                    random,
                    template.includeEntities()
            );
            if (!placedStructure) {
                continue;
            }

            markStructureFootprint(x, z, footprintRadius);

            placed.add(new Location(activeWorld, x + 0.5, surfaceY, z + 0.5));
            regionMinY = Math.min(regionMinY, surfaceY - 2);
            regionMaxY = Math.max(regionMaxY, surfaceY + 1 + structureHeight + 2);
        }
    }

    private boolean canPlaceWatchtower(int centerX, int centerZ, int baseY, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx * dx) + (dz * dz) > radius * radius) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                long key = columnKey(x, z);
                if (reservedStructureColumns.contains(key)) {
                    return false;
                }
                Integer surface = surfaceHeights.get(key);
                if (surface != null && Math.abs(surface - baseY) > 2) {
                    return false;
                }
            }
        }
        return true;
    }

    private void markStructureFootprint(int centerX, int centerZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx * dx) + (dz * dz) > radius * radius) {
                    continue;
                }
                reservedStructureColumns.add(columnKey(centerX + dx, centerZ + dz));
            }
        }
    }

    private boolean isTowerFarEnough(List<Location> towers, double candidateX, double candidateZ, double minDistance) {
        double minSquared = minDistance * minDistance;
        for (Location tower : towers) {
            if (tower.getWorld() != activeWorld) {
                continue;
            }
            double dx = tower.getX() - candidateX;
            double dz = tower.getZ() - candidateZ;
            if ((dx * dx) + (dz * dz) < minSquared) {
                return false;
            }
        }
        return true;
    }

    private boolean isStructureBlocked(int x, int z) {
        return reservedStructureColumns.contains(columnKey(x, z));
    }

    private void spawnFluidPools(Material fluid, int attempts, int radius, MapTheme theme) {
        for (int i = 0; i < attempts; i++) {
            tryPlacePool(fluid, radius, theme);
        }
    }

    private void buildContainmentRidge(MapTheme theme, int radius, int baseY) {
        if (activeWorld == null || activeCenter == null) {
            return;
        }
        if (!theme.hasBorder()) {
            return;
        }
        int ridgeRadius = radius + 2;
        int samples = Math.max(32, (int) Math.round(ridgeRadius * 6.0));
        Material ridgeMaterial = resolveBorderMaterial(theme);
        for (int i = 0; i < samples; i++) {
            double angle = (Math.PI * 2 * i) / samples;
            int x = activeCenter.getBlockX() + (int) Math.round(ridgeRadius * Math.cos(angle));
            int z = activeCenter.getBlockZ() + (int) Math.round(ridgeRadius * Math.sin(angle));
            Integer surface = findNearestSurfaceY(x, z, baseY);
            int surfaceY = surface != null ? surface + 1 : baseY + 1;
            buildRidgeColumn(x, z, surfaceY, ridgeMaterial);
        }

        int minBarrierY = Math.max(activeWorld.getMinHeight(), baseY - 2);
        int maxBarrierY = Math.min(activeWorld.getMaxHeight(), baseY + 22);
        buildBarrierRing(ridgeRadius + 1, minBarrierY, maxBarrierY);
    }

    private void buildBorderWall(MapTheme theme, int radius, int baseY) {
        if (activeWorld == null || activeCenter == null) {
            return;
        }
        if (!theme.hasBorder()) {
            return;
        }
        int centerX = activeCenter.getBlockX();
        int centerZ = activeCenter.getBlockZ();
        int floorRadius = radius + 1;
        int innerRadiusSquared = radius * radius;
        int floorRadiusSquared = floorRadius * floorRadius;
        Material floorMaterial = theme.getTopMaterial();
        if (floorMaterial == null || floorMaterial == Material.AIR) {
            floorMaterial = resolveBorderMaterial(theme);
        }
        Material fillerMaterial = theme.getFillerMaterial();
        if (fillerMaterial == null || fillerMaterial == Material.AIR) {
            fillerMaterial = Material.STONE;
        }
        Material wallMaterial = resolveBorderMaterial(theme);
        int wallHeight = theme.getTerrainProfile() == MapTheme.TerrainProfile.REAL_MOUNTAIN ? 8 : 3;

        for (int dx = -floorRadius; dx <= floorRadius; dx++) {
            for (int dz = -floorRadius; dz <= floorRadius; dz++) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared <= innerRadiusSquared || distanceSquared > floorRadiusSquared) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;

                int innerX = centerX + (int) Math.round(dx * (radius / (double) floorRadius));
                int innerZ = centerZ + (int) Math.round(dz * (radius / (double) floorRadius));
                Integer innerSurface = surfaceHeights.get(columnKey(innerX, innerZ));

                Integer surfaceY = surfaceHeights.get(columnKey(x, z));
                if (surfaceY == null) {
                    surfaceY = findNearestSurfaceY(x, z, baseY);
                }
                if (innerSurface != null) {
                    if (surfaceY == null) {
                        surfaceY = innerSurface;
                    } else {
                        surfaceY = Math.max(surfaceY, innerSurface);
                    }
                }
                if (surfaceY == null) {
                    surfaceY = baseY;
                }
                buildBorderColumn(x, z, surfaceY, fillerMaterial, floorMaterial, wallMaterial, wallHeight);
            }
        }
    }

    private void buildBorderColumn(int x,
                                   int z,
                                   int surfaceY,
                                   Material fillerMaterial,
                                   Material floorMaterial,
                                   Material wallMaterial,
                                   int wallHeight) {
        if (activeWorld == null) {
            return;
        }
        int minY = Math.max(activeWorld.getMinHeight(), surfaceY - 4);
        for (int y = minY; y < surfaceY; y++) {
            setBlock(activeWorld, x, y, z, fillerMaterial);
        }
        setBlock(activeWorld, x, surfaceY, z, floorMaterial);
        int wallTop = Math.min(activeWorld.getMaxHeight() - 1, surfaceY + Math.max(1, wallHeight));
        for (int y = surfaceY + 1; y <= wallTop; y++) {
            setBlock(activeWorld, x, y, z, wallMaterial);
        }
        clearAboveSurface(x, wallTop + 1, z);
        surfaceHeights.put(columnKey(x, z), surfaceY);
        regionMinY = Math.min(regionMinY, minY);
        regionMaxY = Math.max(regionMaxY, wallTop + 1);
    }

    private void buildRidgeColumn(int centerX, int centerZ, int baseSurfaceY, Material ridgeMaterial) {
        if (activeWorld == null) {
            return;
        }
        int height = 9 + random.nextInt(5);
        int topY = Math.min(activeWorld.getMaxHeight() - 1, baseSurfaceY + height);
        regionMinY = Math.min(regionMinY, baseSurfaceY);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                double distance = Math.sqrt((dx * dx) + (dz * dz));
                if (distance > 1.5) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                int taper = distance < 0.25 ? 0 : 1;
                int columnTop = topY - taper;
                for (int y = baseSurfaceY; y <= columnTop; y++) {
                    Material material = y >= columnTop - 1 ? Material.COBBLESTONE : ridgeMaterial;
                    setBlock(activeWorld, x, y, z, material);
                    regionMaxY = Math.max(regionMaxY, y);
                }
            }
        }
    }

    private void buildBarrierRing(int radius, int minY, int maxY) {
        if (activeWorld == null || activeCenter == null) {
            return;
        }
        int samples = Math.max(16, (int) Math.round(radius * 6.0));
        regionMinY = Math.min(regionMinY, minY);
        regionMaxY = Math.max(regionMaxY, maxY);
        for (int i = 0; i < samples; i++) {
            double angle = (Math.PI * 2 * i) / samples;
            int x = activeCenter.getBlockX() + (int) Math.round(radius * Math.cos(angle));
            int z = activeCenter.getBlockZ() + (int) Math.round(radius * Math.sin(angle));
            for (int y = minY; y <= maxY; y++) {
                setBlock(activeWorld, x, y, z, Material.BARRIER);
            }
        }
    }

    private void buildTopCover(MapTheme theme, int radius, int baseY) {
        if (activeWorld == null || activeCenter == null) {
            return;
        }
        if (!theme.hasTopCover()) {
            return;
        }
        Material coverMaterial = resolveTopCoverMaterial(theme);
        if (coverMaterial == null || coverMaterial == Material.AIR) {
            return;
        }
        int coverHeightBoost = theme.getTerrainProfile() == MapTheme.TerrainProfile.REAL_MOUNTAIN ? 24 : 18;
        int coverY = Math.min(activeWorld.getMaxHeight() - 2, baseY + coverHeightBoost);
        int centerX = activeCenter.getBlockX();
        int centerZ = activeCenter.getBlockZ();
        int radiusSquared = (radius + 1) * (radius + 1);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx * dx) + (dz * dz) > radiusSquared) {
                    continue;
                }
                setBlock(activeWorld, centerX + dx, coverY, centerZ + dz, coverMaterial);
            }
        }
        regionMaxY = Math.max(regionMaxY, coverY);
    }

    private void buildRoadNetwork(MapTheme theme, int radius) {
        if (activeWorld == null || activeCenter == null) {
            return;
        }
        if (!theme.roadPathsEnabled()) {
            return;
        }
        Material material = theme.getRoadMaterial();
        if (material == null || material == Material.AIR) {
            return;
        }
        int halfWidth = Math.max(1, (int) Math.round(radius * 0.04));
        double angle = random.nextDouble() * Math.PI * 2.0;
        carveRoadLine(radius, halfWidth, angle, material);
    }

    private void carveRoadLine(int radius, int halfWidth, double angle, Material material) {
        if (activeCenter == null) {
            return;
        }
        int centerX = activeCenter.getBlockX();
        int centerZ = activeCenter.getBlockZ();
        double dirX = Math.cos(angle);
        double dirZ = Math.sin(angle);
        double perpX = -dirZ;
        double perpZ = dirX;
        int radiusSquared = radius * radius;
        for (int t = -radius; t <= radius; t++) {
            int baseX = centerX + (int) Math.round(dirX * t);
            int baseZ = centerZ + (int) Math.round(dirZ * t);
            for (int offset = -halfWidth; offset <= halfWidth; offset++) {
                int x = baseX + (int) Math.round(perpX * offset);
                int z = baseZ + (int) Math.round(perpZ * offset);
                int dx = x - centerX;
                int dz = z - centerZ;
                if ((dx * dx) + (dz * dz) > radiusSquared) {
                    continue;
                }
                paintRoadTile(x, z, material);
            }
        }
    }

    private void paintRoadTile(int x, int z, Material material) {
        if (activeWorld == null) {
            return;
        }
        Integer surfaceY = surfaceHeights.get(columnKey(x, z));
        if (surfaceY == null && activeCenter != null) {
            surfaceY = findNearestSurfaceY(x, z, activeCenter.getBlockY());
        }
        if (surfaceY == null) {
            return;
        }
        setBlock(activeWorld, x, surfaceY, z, material);
        clearAboveSurface(x, surfaceY + 1, z);
        regionMinY = Math.min(regionMinY, surfaceY);
        regionMaxY = Math.max(regionMaxY, surfaceY + 1);
    }

    private void tryPlacePool(Material fluid, int radius, MapTheme theme) {
        if (activeCenter == null) {
            return;
        }
        int tries = 10;
        while (tries-- > 0) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = radius * 0.2 + random.nextDouble() * radius * 0.4;
            int x = activeCenter.getBlockX() + (int) Math.round(distance * Math.cos(angle));
            int z = activeCenter.getBlockZ() + (int) Math.round(distance * Math.sin(angle));
            Integer surfaceY = surfaceHeights.get(columnKey(x, z));
            if (surfaceY == null) {
                surfaceY = findNearestSurfaceY(x, z, activeCenter.getBlockY());
            }
            if (surfaceY == null) {
                continue;
            }
            carvePoolAt(x, surfaceY, z, fluid, theme);
            break;
        }
    }

    private void carvePoolAt(int centerX, int surfaceY, int centerZ, Material fluid, MapTheme theme) {
        if (activeWorld == null) {
            return;
        }
        int poolRadius = 2 + random.nextInt(2);
        int radiusSquared = poolRadius * poolRadius;
        int innerRadiusSquared = Math.max(1, (poolRadius - 1) * (poolRadius - 1));
        for (int dx = -poolRadius; dx <= poolRadius; dx++) {
            for (int dz = -poolRadius; dz <= poolRadius; dz++) {
                int distSq = dx * dx + dz * dz;
                if (distSq > radiusSquared) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                int depth = distSq <= innerRadiusSquared ? 2 : 1;
                placeFluidColumn(x, surfaceY, z, depth, fluid, theme);
                clearAboveSurface(x, surfaceY + 1, z);
            }
        }
        reinforcePoolRim(centerX, centerZ, surfaceY, poolRadius, theme);
        regionMinY = Math.min(regionMinY, surfaceY - 3);
    }

    private void reinforcePoolRim(int centerX, int centerZ, int surfaceY, int poolRadius, MapTheme theme) {
        if (activeWorld == null) {
            return;
        }
        int rimRadius = poolRadius + 1;
        int innerRadiusSquared = poolRadius * poolRadius;
        int outerRadiusSquared = rimRadius * rimRadius;
        Material rimMaterial = theme.getTopMaterial();
        if (rimMaterial == null || rimMaterial == Material.AIR) {
            rimMaterial = resolveBorderMaterial(theme);
        }
        Material fillerMaterial = theme.getFillerMaterial();
        if (fillerMaterial == null || fillerMaterial == Material.AIR) {
            fillerMaterial = Material.STONE;
        }
        for (int dx = -rimRadius; dx <= rimRadius; dx++) {
            for (int dz = -rimRadius; dz <= rimRadius; dz++) {
                int distSq = dx * dx + dz * dz;
                if (distSq <= innerRadiusSquared || distSq > outerRadiusSquared) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                int minY = Math.max(activeWorld.getMinHeight(), surfaceY - 2);
                for (int y = minY; y < surfaceY; y++) {
                    setBlock(activeWorld, x, y, z, fillerMaterial);
                }
                setBlock(activeWorld, x, surfaceY, z, rimMaterial);
                clearAboveSurface(x, surfaceY + 1, z);
                surfaceHeights.put(columnKey(x, z), surfaceY);
                regionMinY = Math.min(regionMinY, minY);
                regionMaxY = Math.max(regionMaxY, surfaceY + 1);
            }
        }
    }

    private void placeFluidColumn(int x, int surfaceY, int z, int depth, Material fluid, MapTheme theme) {
        if (activeWorld == null) {
            return;
        }
        int minY = activeWorld.getMinHeight();
        setBlock(activeWorld, x, surfaceY, z, fluid);
        if (surfaceY - 1 >= minY && depth >= 1) {
            setBlock(activeWorld, x, surfaceY - 1, z, fluid);
        }
        if (surfaceY - 2 >= minY && depth >= 2) {
            setBlock(activeWorld, x, surfaceY - 2, z, theme.getFillerMaterial());
        }
        int baseY = surfaceY - (depth + 1);
        if (baseY >= minY) {
            setBlock(activeWorld, x, baseY, z, theme.getFillerMaterial());
            regionMinY = Math.min(regionMinY, baseY);
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
        touchedBlocks.add(key);
        block.setType(material, false);
    }

    private void processColumn(ColumnWork work, int baseY, MapTheme theme) {
        int surfaceY = calculateSurfaceY(baseY, work.x(), work.z(), work.normalizedDistance(), theme);
        buildColumn(work.x(), work.z(), surfaceY, theme, work.edge());
    }

    private List<List<ColumnWork>> buildSlices(int radius) {
        List<List<ColumnWork>> slices = new ArrayList<>();
        if (activeCenter == null) {
            return slices;
        }
        int centerX = activeCenter.getBlockX();
        int centerZ = activeCenter.getBlockZ();
        int radiusSquared = radius * radius;
        for (int startDx = -radius; startDx <= radius; startDx += GENERATION_SLICE_WIDTH) {
            int endDx = Math.min(radius, startDx + GENERATION_SLICE_WIDTH - 1);
            List<ColumnWork> slice = new ArrayList<>();
            for (int dx = startDx; dx <= endDx; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distanceSquared = dx * dx + dz * dz;
                    if (distanceSquared > radiusSquared) {
                        continue;
                    }
                    double normalizedDistance = Math.sqrt(distanceSquared) / radius;
                    boolean edge = normalizedDistance > 0.9;
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    slice.add(new ColumnWork(x, z, normalizedDistance, edge));
                }
            }
            if (!slice.isEmpty()) {
                slices.add(slice);
            }
        }
        return slices;
    }

    private List<ColumnCoordinate> buildClearColumns(int radius) {
        List<ColumnCoordinate> columns = new ArrayList<>();
        if (activeCenter == null) {
            return columns;
        }
        int centerX = activeCenter.getBlockX();
        int centerZ = activeCenter.getBlockZ();
        int radiusSquared = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSquared) {
                    continue;
                }
                columns.add(new ColumnCoordinate(centerX + dx, centerZ + dz));
            }
        }
        return columns;
    }

    private void resetRegionState() {
        touchedBlocks.clear();
        surfaceHeights.clear();
        spawnMarkerLocations.clear();
        reservedStructureColumns.clear();
        activeCenter = null;
        activeWorld = null;
        lastTheme = null;
        lastRadius = 0;
        regionMinY = Integer.MAX_VALUE;
        regionMaxY = Integer.MIN_VALUE;
    }

    private final class GenerationSession extends BukkitRunnable {
        private final MapTheme theme;
        private final int radius;
        private final int baseY;
        private final int playerCount;
        private final Consumer<MapGenerationSummary> completion;
        private final Consumer<String> error;
        private final List<List<ColumnWork>> slices;
        private int sliceCursor = 0;

        private GenerationSession(MapTheme theme,
                                  int radius,
                                  int baseY,
                                  int playerCount,
                                  Consumer<MapGenerationSummary> completion,
                                  Consumer<String> error) {
            this.theme = theme;
            this.radius = radius;
            this.baseY = baseY;
            this.playerCount = playerCount;
            this.completion = completion;
            this.error = error;
            this.slices = buildSlices(radius);
        }

        @Override
        public void run() {
            try {
                if (sliceCursor >= slices.size()) {
                    finishSuccessfully();
                    return;
                }
                List<ColumnWork> slice = slices.get(sliceCursor++);
                for (ColumnWork work : slice) {
                    processColumn(work, baseY, theme);
                }
                if (sliceCursor >= slices.size()) {
                    finishSuccessfully();
                }
            } catch (Exception ex) {
                cancel();
                handleFailure(ex);
            }
        }

        private void finishSuccessfully() {
            cancel();
            activeGeneration = null;
            buildBorderWall(theme, radius, baseY);
            buildContainmentRidge(theme, radius, baseY);
            buildRoadNetwork(theme, radius);
            buildTopCover(theme, radius, baseY);
            placePools(theme, radius);
            placeWatchtowers(theme, radius);
            placeSpawnMarkers(playerCount, radius);
            lastTheme = theme;
            lastRadius = radius;
            plugin.getLogger().info(String.format("Generated %s PvE map (radius=%d, blocks=%d)", theme.name(), radius, touchedBlocks.size()));
            MapGenerationSummary summary = new MapGenerationSummary(theme, radius, touchedBlocks.size(), spawnMarkerLocations.size());
            completion.accept(summary);
        }

        private void handleFailure(Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to generate map", ex);
            activeGeneration = null;
            surfaceHeights.clear();
            spawnMarkerLocations.clear();
            touchedBlocks.clear();
            activeCenter = null;
            activeWorld = null;
            lastTheme = null;
            lastRadius = 0;
            error.accept("Failed to generate map. Check server logs.");
        }
    }

    private record ColumnWork(int x, int z, double normalizedDistance, boolean edge) {
    }

    private final class ClearSession extends BukkitRunnable {
        private final List<ColumnCoordinate> columns;
        private final int minY;
        private final int maxY;
        private final Consumer<Integer> completion;
        private final Consumer<String> error;
        private int cursor = 0;
        private int clearedBlocks = 0;

        private ClearSession(List<ColumnCoordinate> columns,
                             int minY,
                             int maxY,
                             Consumer<Integer> completion,
                             Consumer<String> error) {
            this.columns = columns;
            this.minY = minY;
            this.maxY = maxY;
            this.completion = completion;
            this.error = error;
        }

        @Override
        public void run() {
            try {
                int processed = 0;
                while (cursor < columns.size() && processed < CLEAR_COLUMNS_PER_SLICE) {
                    ColumnCoordinate column = columns.get(cursor++);
                    clearedBlocks += clearColumn(column);
                    processed++;
                }
                if (cursor >= columns.size()) {
                    finishSuccessfully();
                }
            } catch (Exception ex) {
                cancel();
                handleFailure(ex);
            }
        }

        private int clearColumn(ColumnCoordinate column) {
            if (activeWorld == null) {
                return 0;
            }
            int cleared = 0;
            for (int y = minY; y <= maxY; y++) {
                Block block = activeWorld.getBlockAt(column.x(), y, column.z());
                if (!block.isEmpty()) {
                    block.setType(Material.AIR, false);
                    cleared++;
                }
            }
            return cleared;
        }

        private void finishSuccessfully() {
            cancel();
            activeClear = null;
            resetRegionState();
            completion.accept(clearedBlocks);
        }

        private void handleFailure(Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to clear map region", ex);
            activeClear = null;
            resetRegionState();
            error.accept("Failed to clear map region. Check server logs.");
        }
    }

    private record ColumnCoordinate(int x, int z) {
    }

    private static String blockKey(UUID worldId, int x, int y, int z) {
        return worldId + ":" + x + ":" + y + ":" + z;
    }

    private static long columnKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

}
