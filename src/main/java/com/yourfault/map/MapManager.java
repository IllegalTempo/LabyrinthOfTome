package com.yourfault.map;

import com.yourfault.map.build.*;
import com.yourfault.system.Game;
import com.yourfault.utils.PerlinNoise;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Generates and clears PvE arenas that leverage Perlin noise heightmaps and biome themes.
 */
public class MapManager {
    private final JavaPlugin plugin;
    private final Game game;
    private final Random random = new Random();
    private final StructurePlacementHelper structureHelper;
    private final RoadBuilder roadBuilder;
    private PerlinNoise noise;
    private static final int GENERATION_SLICE_INTERVAL_TICKS = 10; // 0.5 seconds
    private static final int GENERATION_COLUMNS_PER_SLICE = 256;
    private static final int CLEAR_SLICE_INTERVAL_TICKS = 10;
    private static final int CLEAR_COLUMNS_PER_SLICE = 512;
    private static final int CLEAR_RADIUS_PADDING = 6;
    private static final double MIN_SPAWN_MARKER_SPACING = 6.0;
    private static final int FIXED_SPAWN_MARKER_COUNT = 9;
    private static final int POOL_FLOOR_LIFT_BLOCKS = 5;
    private static final int BEACON_NUDGE_STEPS = 12;

    private final Set<String> touchedBlocks = new HashSet<>();
    private final Map<Long, Integer> surfaceHeights = new HashMap<>();
    private final List<Location> spawnMarkerLocations = new ArrayList<>();
    private final Set<Long> reservedStructureColumns = new HashSet<>();
    private final Set<Long> roadColumns = new HashSet<>();
    private final Set<Long> mountainColumns = new HashSet<>();
    private final Set<Long> poolColumns = new HashSet<>();
    private List<Material> activeRoadPalette = new ArrayList<>();

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
    private int lastRoadHalfWidth;

    public MapManager(JavaPlugin plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
        this.noise = new PerlinNoise(System.currentTimeMillis());
        this.structureHelper = new StructurePlacementHelper(plugin);
        this.roadBuilder = new RoadBuilder();
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
        roadColumns.clear();
        mountainColumns.clear();
        poolColumns.clear();
        activeRoadPalette = new ArrayList<>();
        lastRoadHalfWidth = 0;
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
        int radius = Math.max(minRadius, Math.min(maxRadius, (int) Math.round(base)));

        final int lowThreshold = 4;
        final int decayEnd = 16;
        final double maxMultiplier = 2.5;
        final double minMultiplier = 1.0;

        double multiplier;
        if (playerCount <= lowThreshold) {
            multiplier = maxMultiplier;
        } else if (playerCount >= decayEnd) {
            multiplier = minMultiplier;
        } else {
            double scaledPlayerCount = Math.log(playerCount) / Math.log(decayEnd);
            multiplier = minMultiplier + (maxMultiplier - minMultiplier) * (1 - scaledPlayerCount);
        }

        return Math.max(1, (int) Math.round(radius*multiplier));
    }


    private int calculateSurfaceY(int baseY, int x, int z, double normalizedDistance, MapTheme theme) {
        //Main Terrain Generation Logic
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
        double desiredHeight = baseY + 0.6 + (rimFactor * 0.8);
        int target = (int) Math.round(desiredHeight);
        target = Math.min(target, maxHeight - 1);
        int limitedTarget = Math.min(target, currentHeight + 1);
        return Math.max(currentHeight, limitedTarget);
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
        if (theme.getDecorationChance() <= 0.0) {
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

    private TemplateResolved resolveTemplate(MapTheme theme, MapTheme.StructureTemplate template) {
        return resolveTemplate(theme, template, "structure");
    }

    private TemplateResolved resolveTemplate(MapTheme theme,
                                             MapTheme.StructureTemplate template,
                                             String usageLabel) {
        if (template == null) {
            return null;
        }
        String resourcePath = template.resourcePath();
        if (resourcePath == null || resourcePath.isBlank()) {
            plugin.getLogger().log(Level.WARNING,
                    "Structure template path is not set for theme {0} ({1})",
                    new Object[]{theme.name(), usageLabel});
            return null;
        }
        if (!structureHelper.hasStructure(resourcePath)) {
            plugin.getLogger().log(Level.WARNING,
                    "Structure resource {0} could not be loaded for theme {1} ({2}); skipping placement.",
                    new Object[]{resourcePath, theme.name(), usageLabel});
            return null;
        }
        BlockVector templateSize = structureHelper.getStructureSize(resourcePath);
        int footprintRadius = structureHelper.estimateFootprintRadius(templateSize, template.fallbackFootprintRadius());
        int structureHeight = templateSize != null ? templateSize.getBlockY() : template.estimatedHeight();
        return new TemplateResolved(template, resourcePath, footprintRadius, structureHeight);
    }

    private boolean placeResolvedStructure(TemplateResolved resolved, int x, int z, int surfaceY, MapTheme theme) {
        if (resolved == null || activeWorld == null) {
            return false;
        }
        prepareStructurePad(x, z, surfaceY, resolved.footprintRadius(), theme);
        MapTheme.StructureTemplate.Rotation rotation = resolved.template().pickRotation(random);
        boolean placedStructure = structureHelper.placeStructure(
                resolved.resourcePath(),
                activeWorld,
                x,
                surfaceY + 1,
                z,
                random,
                resolved.template().includeEntities(),
                rotation
        );
        if (!placedStructure) {
            return false;
        }
        markStructureFootprint(x, z, resolved.footprintRadius());
        applyStructureBlend(theme, x, z, surfaceY, resolved.footprintRadius());
        restoreStructureGround(x, z, surfaceY, resolved.footprintRadius(), theme);
        regionMinY = Math.min(regionMinY, surfaceY - 2);
        regionMaxY = Math.max(regionMaxY, surfaceY + 1 + resolved.structureHeight() + 2);
        return true;
    }

    private void prepareStructurePad(int centerX,
                                     int centerZ,
                                     int targetSurface,
                                     int radius,
                                     MapTheme theme) {
        if (activeWorld == null) {
            return;
        }
        Material filler = theme.getFillerMaterial();
        if (filler == null || filler == Material.AIR) {
            filler = Material.DIRT;
        }
        Material top = theme.getTopMaterial();
        if (top == null || top == Material.AIR) {
            top = filler;
        }
        int radiusSquared = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx * dx) + (dz * dz) > radiusSquared) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                long key = columnKey(x, z);
                Integer surface = surfaceHeights.get(key);
                if (surface == null) {
                    surface = findNearestSurfaceY(x, z, targetSurface);
                }
                if (surface == null) {
                    continue;
                }
                if (surface > targetSurface) {
                    lowerColumn(x, z, surface, targetSurface, filler, top);
                    surface = targetSurface;
                } else if (surface < targetSurface) {
                    raiseColumn(x, z, surface, targetSurface, filler, top);
                    surface = targetSurface;
                }
                surfaceHeights.put(key, surface);
            }
        }
    }

    private void restoreStructureGround(int centerX,
                                        int centerZ,
                                        int surfaceY,
                                        int radius,
                                        MapTheme theme) {
        if (activeWorld == null) {
            return;
        }
        Material top = theme.getTopMaterial();
        if (top == null || top == Material.AIR) {
            top = Material.GRASS_BLOCK;
        }
        Material filler = theme.getFillerMaterial();
        if (filler == null || filler == Material.AIR) {
            filler = Material.DIRT;
        }
        int radiusSquared = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx * dx) + (dz * dz) > radiusSquared) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                Block ground = activeWorld.getBlockAt(x, surfaceY, z);
                if (!ground.isEmpty()) {
                    continue;
                }
                Block above = activeWorld.getBlockAt(x, surfaceY + 1, z);
                if (above.getType().isSolid()) {
                    continue;
                }
                setBlock(activeWorld, x, surfaceY, z, top);
                int fillerY = surfaceY - 1;
                if (fillerY >= activeWorld.getMinHeight()) {
                    setBlock(activeWorld, x, fillerY, z, filler);
                }
                surfaceHeights.put(columnKey(x, z), surfaceY);
                regionMinY = Math.min(regionMinY, fillerY);
                regionMaxY = Math.max(regionMaxY, surfaceY + 1);
            }
        }
    }

    private void placeRoadsideStructures(MapTheme theme, RoadPath roadPath, int radius) {
        if (activeWorld == null || activeCenter == null) {
            return;
        }
        if (roadPath == null || roadPath.isEmpty()) {
            return;
        }
        if (!theme.lampPostsEnabled()) {
            return;
        }
        List<MapTheme.StructureTemplate> lampTemplates = theme.getLampPostTemplates();
        if (lampTemplates == null || lampTemplates.isEmpty()) {
            return;
        }
        List<TemplateResolved> resolvedTemplates = new ArrayList<>();
        for (MapTheme.StructureTemplate template : lampTemplates) {
            TemplateResolved resolved = resolveTemplate(theme, template, "road-side structure");
            if (resolved != null) {
                resolvedTemplates.add(resolved);
            }
        }
        if (resolvedTemplates.isEmpty()) {
            return;
        }
        double pathLength = Math.max(0.0, roadPath.length());
        if (pathLength <= 0.01) {
            return;
        }
        double spacing = Math.max(10.0, Math.max(6.0, lastRoadHalfWidth * 4.0));
        int slots = Math.max(1, (int) Math.round(pathLength / spacing));
        Map<MapTheme.StructureTemplate, Integer> usageCounts = new HashMap<>();
        List<Location> placedStructures = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            double offset = random.nextDouble() * (spacing * 0.5);
            double sampleDistance = Math.min(pathLength, i * spacing + offset);
            placeLampPairAlongRoad(theme, roadPath, resolvedTemplates, usageCounts, placedStructures, radius, sampleDistance);
        }
    }

    private void placeLampPairAlongRoad(MapTheme theme,
                                        RoadPath roadPath,
                                        List<TemplateResolved> templates,
                                        Map<MapTheme.StructureTemplate, Integer> usageCounts,
                                        List<Location> placedStructures,
                                        int radius,
                                        double sampleDistance) {
        if (templates.isEmpty()) {
            return;
        }
        RoadPoint anchor = roadPath.sampleAtDistance(sampleDistance);
        if (anchor == null) {
            return;
        }
        double aheadDistance = Math.min(roadPath.length(), sampleDistance + 1.4);
        double behindDistance = Math.max(0.0, sampleDistance - 1.4);
        RoadPoint ahead = roadPath.sampleAtDistance(aheadDistance);
        RoadPoint behind = roadPath.sampleAtDistance(behindDistance);
        if (ahead == null || behind == null) {
            return;
        }
        double dirX = ahead.x() - behind.x();
        double dirZ = ahead.z() - behind.z();
        double length = Math.hypot(dirX, dirZ);
        if (length < 0.0001) {
            return;
        }
        dirX /= length;
        dirZ /= length;
        double perpX = -dirZ;
        double perpZ = dirX;
        double offset = Math.max(3.0, lastRoadHalfWidth + 2.5);
        double[] sides = {1.0, -1.0};
        for (double side : sides) {
            int blockX = (int) Math.round(anchor.x() + perpX * offset * side);
            int blockZ = (int) Math.round(anchor.z() + perpZ * offset * side);
            TemplateResolved resolved = templates.get(random.nextInt(templates.size()));
            MapTheme.StructureTemplate template = resolved.template();
            int maxPlacements = Math.max(0, template.maxPlacements());
            if (maxPlacements > 0 && usageCounts.getOrDefault(template, 0) >= maxPlacements) {
                continue;
            }
            Location placed = tryPlaceRoadStructure(theme, resolved, blockX, blockZ, radius, placedStructures);
            if (placed != null) {
                usageCounts.merge(template, 1, Integer::sum);
                placedStructures.add(placed);
            }
        }
    }

    private Location tryPlaceRoadStructure(MapTheme theme,
                                           TemplateResolved resolved,
                                           int blockX,
                                           int blockZ,
                                           int radius,
                                           List<Location> placedStructures) {
        if (resolved == null || activeCenter == null) {
            return null;
        }
        int effectiveRadius = Math.max(2, radius - 2);
        if (!withinArenaRadius(blockX, blockZ, effectiveRadius)) {
            return null;
        }
        if (isRoadTile(blockX, blockZ) || isStructureBlocked(blockX, blockZ)) {
            return null;
        }
        Integer surfaceY = surfaceHeights.get(columnKey(blockX, blockZ));
        if (surfaceY == null) {
            surfaceY = findNearestSurfaceY(blockX, blockZ, activeCenter.getBlockY());
        }
        if (surfaceY == null) {
            return null;
        }
        double minSpacing = Math.max(6.0, resolved.footprintRadius() * 2.0 + lastRoadHalfWidth);
        if (!isRoadStructureFarEnough(placedStructures, blockX + 0.5, blockZ + 0.5, minSpacing)) {
            return null;
        }
        if (!canPlaceStructure(blockX, blockZ, surfaceY, resolved.footprintRadius())) {
            return null;
        }
        if (!placeResolvedStructure(resolved, blockX, blockZ, surfaceY, theme)) {
            return null;
        }
        return new Location(activeWorld, blockX + 0.5, surfaceY + 1, blockZ + 0.5);
    }

    private boolean withinArenaRadius(int x, int z, int radius) {
        if (activeCenter == null || radius <= 0) {
            return false;
        }
        int dx = x - activeCenter.getBlockX();
        int dz = z - activeCenter.getBlockZ();
        return (dx * dx) + (dz * dz) <= radius * radius;
    }

    private boolean isRoadStructureFarEnough(List<Location> placed,
                                             double candidateX,
                                             double candidateZ,
                                             double minDistance) {
        double minSquared = minDistance * minDistance;
        for (Location location : placed) {
            if (location.getWorld() != activeWorld) {
                continue;
            }
            double dx = location.getX() - candidateX;
            double dz = location.getZ() - candidateZ;
            if ((dx * dx) + (dz * dz) < minSquared) {
                return false;
            }
        }
        return true;
    }

    private void placeSpawnMarkers(int playerCount, RoadPath roadPath, int radius) {
        if (activeWorld == null || activeCenter == null) {
            return;
        }
        spawnMarkerLocations.clear();
        int desiredMarkers = Math.max(9, 9 + Math.max(0, playerCount) * 2);
        int remaining = desiredMarkers;
        if (roadPath != null && !roadPath.isEmpty()) {
            remaining = Math.max(0, remaining - placeMarkersAlongRoad(roadPath, radius, desiredMarkers));
        }
        if (remaining > 0) {
            placeFallbackSpawnMarkers(radius, remaining);
        }
    }

    private int placeMarkersAlongRoad(RoadPath roadPath, int radius, int desiredMarkers) {
        if (roadPath == null || roadPath.isEmpty()) {
            return 0;
        }
        double pathLength = roadPath.length();
        if (pathLength <= 4.0) {
            return 0;
        }
        int placed = 0;
        double spacing = pathLength / (desiredMarkers + 1);
        for (int i = 1; i <= desiredMarkers; i++) {
            double targetDistance = spacing * i;
            if (attemptPlaceMarkerNearRoad(roadPath, targetDistance, i, radius)) {
                placed++;
            }
        }
        return placed;
    }

    private boolean attemptPlaceMarkerNearRoad(RoadPath roadPath, double targetDistance, int index, int radius) {
        if (roadPath == null || roadPath.isEmpty()) {
            return false;
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            double jitter = (random.nextDouble() - 0.5) * 3.0;
            double sampleDistance = Math.max(0.0, Math.min(roadPath.length(), targetDistance + jitter));
            RoadPoint anchor = roadPath.sampleAtDistance(sampleDistance);
            if (anchor == null) {
                continue;
            }
            double aheadDistance = Math.min(roadPath.length(), sampleDistance + 2.0);
            double behindDistance = Math.max(0.0, sampleDistance - 2.0);
            RoadPoint ahead = roadPath.sampleAtDistance(aheadDistance);
            RoadPoint behind = roadPath.sampleAtDistance(behindDistance);
            if (ahead == null || behind == null) {
                continue;
            }
            double dx = ahead.x() - behind.x();
            double dz = ahead.z() - behind.z();
            double length = Math.hypot(dx, dz);
            if (length < 0.001) {
                dx = 1.0;
                dz = 0.0;
                length = 1.0;
            }
            double perpX = -dz / length;
            double perpZ = dx / length;
            double side = (index + attempt) % 2 == 0 ? 1.0 : -1.0;
            double baseOffset = Math.max(3.0, lastRoadHalfWidth + 2.0);
            double offset = baseOffset + random.nextDouble() * (baseOffset * 0.75);
            double candidateX = anchor.x() + perpX * offset * side;
            double candidateZ = anchor.z() + perpZ * offset * side;
            RoadPoint clamped = clampToArena(candidateX, candidateZ, radius);
            int blockX = (int) Math.round(clamped.x());
            int blockZ = (int) Math.round(clamped.z());
            if (tryPlaceSpawnMarker(blockX, blockZ, MIN_SPAWN_MARKER_SPACING)) {
                return true;
            }
        }
        return false;
    }

    private void placeFallbackSpawnMarkers(int radius, int remaining) {
        if (activeCenter == null || activeWorld == null || remaining <= 0) {
            return;
        }
        double innerRadius = radius * 0.35;
        double outerRadius = Math.max(innerRadius + 2.0, radius * 0.8);
        double spacing = MIN_SPAWN_MARKER_SPACING;
        while (remaining > 0 && spacing >= 3.0) {
            int attempts = remaining * 14;
            while (remaining > 0 && attempts-- > 0) {
                double distance = innerRadius + random.nextDouble() * (outerRadius - innerRadius);
                double angle = random.nextDouble() * Math.PI * 2;
                int x = activeCenter.getBlockX() + (int) Math.round(distance * Math.cos(angle));
                int z = activeCenter.getBlockZ() + (int) Math.round(distance * Math.sin(angle));
                if (tryPlaceSpawnMarker(x, z, spacing)) {
                    remaining--;
                }
            }
            spacing -= 0.5;
        }
    }

    private boolean tryPlaceSpawnMarker(int blockX, int blockZ, double spacing) {
        if (isStructureBlocked(blockX, blockZ)) {
            return false;
        }
        if (isRoadTile(blockX, blockZ)) {
            return false;
        }
        if (!isMarkerFarEnough(blockX, blockZ, spacing)) {
            return false;
        }
        Integer surfaceY = surfaceHeights.get(columnKey(blockX, blockZ));
        if (surfaceY == null && activeCenter != null) {
            surfaceY = findNearestSurfaceY(blockX, blockZ, activeCenter.getBlockY());
        }
        if (surfaceY == null) {
            return false;
        }
        int markerY = surfaceY + 1;
        setBlock(activeWorld, blockX, markerY, blockZ, Material.YELLOW_WOOL);
        spawnMarkerLocations.add(new Location(activeWorld, blockX + 0.5, markerY, blockZ + 0.5));
        return true;
    }

    private RoadPoint clampToArena(double x, double z, int radius) {
        if (activeCenter == null) {
            return new RoadPoint(x, z);
        }
        double centerX = activeCenter.getBlockX();
        double centerZ = activeCenter.getBlockZ();
        double dx = x - centerX;
        double dz = z - centerZ;
        double limit = Math.max(2.0, radius - 1.5);
        double distance = Math.hypot(dx, dz);
        if (distance <= limit) {
            return new RoadPoint(x, z);
        }
        double scale = limit / Math.max(distance, 0.0001);
        return new RoadPoint(centerX + dx * scale, centerZ + dz * scale);
    }

    private boolean isRoadTile(int x, int z) {
        return roadColumns.contains(columnKey(x, z));
    }

    private void placePathBeacons(RoadPath roadPath) {
        if (roadPath == null || roadPath.isEmpty()) {
            return;
        }
        placeBeaconAt(roadPath.start(), false);
        placeBeaconAt(roadPath.end(), true);
    }

    private void placeBeaconAt(RoadPoint point, boolean bossBeacon) {
        if (activeWorld == null || point == null) {
            return;
        }
        int x = (int) Math.round(point.x());
        int z = (int) Math.round(point.z());
        ColumnCoordinate adjusted = nudgeColumnTowardCenter(x, z, BEACON_NUDGE_STEPS);
        Integer surfaceY = surfaceHeights.get(columnKey(x, z));
        if (surfaceY == null && activeCenter != null) {
            surfaceY = findNearestSurfaceY(x, z, activeCenter.getBlockY());
        }
        if (surfaceY == null) {
            return;
        }
        Material baseMaterial = bossBeacon ? Material.NETHERITE_BLOCK : Material.IRON_BLOCK;
        Material glassMaterial = bossBeacon ? Material.RED_STAINED_GLASS : Material.WHITE_STAINED_GLASS;
        int layers = bossBeacon ? 2 : 1;
        buildBeaconBase(x, z, surfaceY, baseMaterial, layers);
        int beaconY = surfaceY + 1;
        setBlock(activeWorld, x, beaconY, z, Material.BEACON);
        int glassHeight = bossBeacon ? 4 : 2;
        for (int i = 1; i <= glassHeight; i++) {
            setBlock(activeWorld, x, beaconY + i, z, glassMaterial);
        }
        clearSkyColumn(x, z, beaconY + glassHeight + 1);
        reservedStructureColumns.add(columnKey(x, z));
        surfaceHeights.put(columnKey(x, z), surfaceY);
        regionMinY = Math.min(regionMinY, surfaceY - (layers - 1));
        regionMaxY = Math.max(regionMaxY, beaconY + glassHeight + 1);
    }

    private ColumnCoordinate nudgeColumnTowardCenter(int x, int z, int steps) {
        if (activeCenter == null) {
            return new ColumnCoordinate(x, z);
        }
        int currentX = x;
        int currentZ = z;
        for (int i = 0; i < steps; i++) {
            long key = columnKey(currentX, currentZ);
            if (!reservedStructureColumns.contains(key)) {
                break;
            }
            double dx = activeCenter.getBlockX() - currentX;
            double dz = activeCenter.getBlockZ() - currentZ;
            double length = Math.hypot(dx, dz);
            if (length < 1.0) {
                break;
            }
            currentX += (int) Math.round(dx / length);
            currentZ += (int) Math.round(dz / length);
        }
        return new ColumnCoordinate(currentX, currentZ);
    }

    private void buildBeaconBase(int centerX,
                                 int centerZ,
                                 int topY,
                                 Material material,
                                 int layers) {
        if (activeWorld == null) {
            return;
        }
        int minY = activeWorld.getMinHeight();
        for (int layer = 0; layer < layers; layer++) {
            int radius = 1 + layer;
            int y = Math.max(minY, topY - layer);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int blockX = centerX + dx;
                    int blockZ = centerZ + dz;
                    setBlock(activeWorld, blockX, y, blockZ, material);
                    if (layer == 0) {
                        clearAboveSurface(blockX, y + 1, blockZ);
                    }
                    surfaceHeights.put(columnKey(blockX, blockZ), y);
                    reservedStructureColumns.add(columnKey(blockX, blockZ));
                    regionMinY = Math.min(regionMinY, y);
                    regionMaxY = Math.max(regionMaxY, y + 1);
                }
            }
        }
    }

    private void clearSkyColumn(int x, int z, int startY) {
        if (activeWorld == null) {
            return;
        }
        int maxY = activeWorld.getMaxHeight();
        int minY = Math.max(startY, activeWorld.getMinHeight());
        int highestCleared = Integer.MIN_VALUE;
        for (int y = minY; y <= maxY; y++) {
            Block block = activeWorld.getBlockAt(x, y, z);
            if (!block.isEmpty()) {
                setBlock(activeWorld, x, y, z, Material.AIR);
                highestCleared = Math.max(highestCleared, y);
            }
        }
        if (highestCleared != Integer.MIN_VALUE) {
            regionMaxY = Math.max(regionMaxY, highestCleared);
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
        MapTheme.PoolSettings poolSettings = theme.getPoolSettings();
        double spawnWeight = poolSettings == null ? 1.0 : poolSettings.spawnWeight();
        int attempts = Math.max(1, (int) Math.round((radius / 20.0) * Math.max(0.25, spawnWeight)));
        if (allowWater) {
            spawnFluidPools(Material.WATER, attempts, radius, theme, poolSettings);
        }
        if (allowLava) {
            int lavaAttempts = Math.max(1, (int) Math.round(attempts * 0.5));
            spawnFluidPools(Material.LAVA, lavaAttempts, radius, theme, poolSettings);
        }
    }

    private void placeStructures(MapTheme theme, int radius) {
        if (activeWorld == null || activeCenter == null) {
            return;
        }
        MapTheme.StructureSettings structureSettings = theme.getStructureSettings();
        if (structureSettings == null || !structureSettings.enabled()) {
            return;
        }
        if (radius < 18) {
            return;
        }

        int safeRadiusSquared = (radius - 6) * (radius - 6);

        for (MapTheme.StructureTemplate template : structureSettings.templates()) {
            TemplateResolved resolved = resolveTemplate(theme, template);
            if (resolved == null) {
                continue;
            }
            int maxPlacements = Math.max(1, template.maxPlacements());
            int desiredPlacements = Math.max(1, maxPlacements == 1 ? 1 : 1 + random.nextInt(maxPlacements));
            double minSpacing = Math.max(8.0, resolved.footprintRadius() * 1.5);

            List<Location> placed = new ArrayList<>(desiredPlacements);
            int attempts = Math.max(desiredPlacements * 18, 24);

            while (placed.size() < desiredPlacements && attempts-- > 0) {
                double distance = radius * (0.35 + random.nextDouble() * 0.4);
                double angle = random.nextDouble() * Math.PI * 2;
                int x = activeCenter.getBlockX() + (int) Math.round(distance * Math.cos(angle));
                int z = activeCenter.getBlockZ() + (int) Math.round(distance * Math.sin(angle));
                int dx = x - activeCenter.getBlockX();
                int dz = z - activeCenter.getBlockZ();
                if ((dx * dx) + (dz * dz) > safeRadiusSquared) {
                    continue;
                }
                if (!isStructureFarEnough(placed, x + 0.5, z + 0.5, minSpacing)) {
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
                if (!canPlaceStructure(x, z, surfaceY, resolved.footprintRadius())) {
                    continue;
                }
                if (!placeResolvedStructure(resolved, x, z, surfaceY, theme)) {
                    continue;
                }

                placed.add(new Location(activeWorld, x + 0.5, surfaceY, z + 0.5));
            }
        }
    }

    private boolean canPlaceStructure(int centerX, int centerZ, int baseY, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx * dx) + (dz * dz) > radius * radius) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                long key = columnKey(x, z);
                if (isStructureBlocked(x, z)) {
                    return false;
                }
                if (roadColumns.contains(key)) {
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

    private void markPoolColumn(int x, int z) {
        poolColumns.add(columnKey(x, z));
    }

    private void applyStructureBlend(MapTheme theme,
                                     int centerX,
                                     int centerZ,
                                     int surfaceY,
                                     int footprintRadius) {
        if (theme == null || activeWorld == null) {
            return;
        }
        int blendRadius = Math.max(footprintRadius + 2, footprintRadius + 5);
        int innerRadius = Math.max(1, footprintRadius);
        Material filler = theme.getFillerMaterial();
        if (filler == null || filler == Material.AIR) {
            filler = Material.STONE;
        }
        Material top = theme.getTopMaterial();
        if (top == null || top == Material.AIR) {
            top = filler;
        }
        double scale = Math.max(0.005, theme.getNoiseScale() * 2.0);
        for (int dx = -blendRadius; dx <= blendRadius; dx++) {
            for (int dz = -blendRadius; dz <= blendRadius; dz++) {
                double distance = Math.sqrt((dx * dx) + (dz * dz));
                if (distance <= innerRadius || distance > blendRadius) {
                    continue;
                }
                long key = columnKey(centerX + dx, centerZ + dz);
                if (reservedStructureColumns.contains(key)
                        || mountainColumns.contains(key)
                        || roadColumns.contains(key)
                        || poolColumns.contains(key)) {
                    continue;
                }
                double ringNormalized = (distance - innerRadius) / Math.max(1.0, blendRadius - innerRadius);
                ringNormalized = Math.min(1.0, Math.max(0.0, ringNormalized));
                double strength = Math.cos(ringNormalized * (Math.PI / 2.0));
                if (strength <= 0.01) {
                    continue;
                }
                double noiseSample = noise.sample(
                        (centerX + dx + noiseOffsetX) * scale,
                        (centerZ + dz + noiseOffsetZ) * scale
                );
                double offset = noiseSample * strength * 2.2;
                int adjustment = (int) Math.round(offset);
                if (adjustment == 0) {
                    continue;
                }
                Integer currentSurface = surfaceHeights.get(key);
                if (currentSurface == null) {
                    currentSurface = findNearestSurfaceY(centerX + dx, centerZ + dz, surfaceY);
                }
                if (currentSurface == null) {
                    continue;
                }
                int minAllowed = Math.max(activeWorld.getMinHeight() + 4, surfaceY - 8);
                int maxAllowed = Math.min(activeWorld.getMaxHeight() - 4, surfaceY + 12);
                int targetSurface = Math.max(minAllowed, Math.min(maxAllowed, currentSurface + adjustment));
                if (targetSurface == currentSurface) {
                    continue;
                }
                if (targetSurface > currentSurface) {
                    raiseColumn(centerX + dx, centerZ + dz, currentSurface, targetSurface, filler, top);
                } else {
                    lowerColumn(centerX + dx, centerZ + dz, currentSurface, targetSurface, filler, top);
                }
                surfaceHeights.put(key, targetSurface);
                regionMinY = Math.min(regionMinY, targetSurface - 3);
                regionMaxY = Math.max(regionMaxY, targetSurface + 1);
            }
        }
    }

    private boolean isStructureFarEnough(List<Location> placements, double candidateX, double candidateZ, double minDistance) {
        double minSquared = minDistance * minDistance;
        for (Location existing : placements) {
            if (existing.getWorld() != activeWorld) {
                continue;
            }
            double dx = existing.getX() - candidateX;
            double dz = existing.getZ() - candidateZ;
            if ((dx * dx) + (dz * dz) < minSquared) {
                return false;
            }
        }
        return true;
    }

    private boolean isStructureBlocked(int x, int z) {
        long key = columnKey(x, z);
        return reservedStructureColumns.contains(key)
                || mountainColumns.contains(key)
                || poolColumns.contains(key);
    }

    private void spawnFluidPools(Material fluid,
                                 int attempts,
                                 int radius,
                                 MapTheme theme,
                                 MapTheme.PoolSettings settings) {
        for (int i = 0; i < attempts; i++) {
            tryPlacePool(fluid, radius, theme, settings);
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

    private void generateMountains(MapTheme theme, int radius) {
        if (activeWorld == null || activeCenter == null) {
            return;
        }
        MapTheme.MountainSettings settings = theme.getMountainSettings();
        if (settings == null || !settings.enabled()) {
            return;
        }
        int peaks = Math.max(1, settings.peakCount());
        int attempts = Math.max(peaks * 16, 24);
        double innerRadius = Math.max(6.0, radius * 0.2);
        double outerRadius = Math.max(innerRadius + 4.0, radius * 0.82);
        int placed = 0;
        while (placed < peaks && attempts-- > 0) {
            double distance = innerRadius + random.nextDouble() * (outerRadius - innerRadius);
            double angle = random.nextDouble() * Math.PI * 2;
            int centerX = activeCenter.getBlockX() + (int) Math.round(distance * Math.cos(angle));
            int centerZ = activeCenter.getBlockZ() + (int) Math.round(distance * Math.sin(angle));
            if (!withinArenaRadius(centerX, centerZ, radius - 2)) {
                continue;
            }
            long centerKey = columnKey(centerX, centerZ);
            if (mountainColumns.contains(centerKey)) {
                continue;
            }
            Integer baseSurface = surfaceHeights.get(centerKey);
            if (baseSurface == null) {
                baseSurface = findNearestSurfaceY(centerX, centerZ, activeCenter.getBlockY());
            }
            if (baseSurface == null) {
                continue;
            }
            int footprint = Math.max(6, Math.min(radius / 2, (int) Math.round(radius * 0.1 + random.nextDouble() * radius * 0.08)));
            int peakHeight = computeMountainPeakHeight(settings, distance / radius);
            if (sculptMountain(centerX, centerZ, baseSurface, peakHeight, footprint, theme)) {
                mountainColumns.add(centerKey);
                placed++;
            }
        }
    }

    private int computeMountainPeakHeight(MapTheme.MountainSettings settings, double normalizedDistance) {
        double clamped = Math.min(1.0, Math.max(0.0, normalizedDistance));
        double distanceFactor = 1.0 - clamped;
        double base = Math.max(20.0, settings.maxHeight() * 0.55);
        double variability = (settings.maxHeight() - base) * Math.pow(distanceFactor, 0.65);
        double jitter = (random.nextDouble() - 0.5) * 6.0;
        double height = base + variability + jitter;
        return (int) Math.round(Math.max(20.0, Math.min(settings.maxHeight(), height)));
    }

    private boolean sculptMountain(int centerX,
                                   int centerZ,
                                   int baseSurface,
                                   int peakHeight,
                                   int radius,
                                   MapTheme theme) {
        if (activeWorld == null) {
            return false;
        }
        Material filler = theme.getFillerMaterial();
        if (filler == null || filler == Material.AIR) {
            filler = Material.STONE;
        }
        Material top = theme.getTopMaterial();
        if (top == null || top == Material.AIR) {
            top = filler;
        }
        int worldMax = activeWorld.getMaxHeight() - 2;
        double localNoiseScale = Math.max(0.004, theme.getNoiseScale() * 1.5);
        boolean modified = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt((dx * dx) + (dz * dz));
                if (distance > radius) {
                    continue;
                }
                double normalized = distance / Math.max(1.0, radius);
                double curve = Math.cos(normalized * (Math.PI / 2.0));
                if (curve <= 0) {
                    continue;
                }
                double noiseSample = noise.sample(
                        (centerX + dx + noiseOffsetX) * localNoiseScale,
                        (centerZ + dz + noiseOffsetZ) * localNoiseScale
                );
                double variance = 0.8 + noiseSample * 0.35;
                int delta = (int) Math.round(peakHeight * curve * variance);
                if (delta <= 0) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                long key = columnKey(x, z);
                Integer currentSurface = surfaceHeights.get(key);
                if (currentSurface == null) {
                    currentSurface = baseSurface;
                }
                int targetSurface = Math.min(worldMax, currentSurface + delta);
                if (targetSurface <= currentSurface) {
                    continue;
                }
                raiseColumn(x, z, currentSurface, targetSurface, filler, top);
                surfaceHeights.put(key, targetSurface);
                mountainColumns.add(key);
                regionMaxY = Math.max(regionMaxY, targetSurface + 1);
                regionMinY = Math.min(regionMinY, baseSurface - 4);
                modified = true;
            }
        }
        return modified;
    }

    private void raiseColumn(int x,
                             int z,
                             int currentSurface,
                             int targetSurface,
                             Material filler,
                             Material top) {
        if (activeWorld == null) {
            return;
        }
        for (int y = currentSurface + 1; y <= targetSurface; y++) {
            Material material = y == targetSurface ? top : filler;
            setBlock(activeWorld, x, y, z, material);
        }
    }

    private void lowerColumn(int x,
                             int z,
                             int currentSurface,
                             int targetSurface,
                             Material filler,
                             Material top) {
        if (activeWorld == null) {
            return;
        }
        for (int y = currentSurface; y > targetSurface; y--) {
            setBlock(activeWorld, x, y, z, filler);
        }
        setBlock(activeWorld, x, targetSurface, z, top);
        clearAboveSurface(x, targetSurface + 1, z);
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
                int x = centerX + dx;
                int z = centerZ + dz;
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared <= innerRadiusSquared || distanceSquared > floorRadiusSquared) {
                    continue;
                }

                // determine surfaceY (may be null) and try inner-surface fallback before placing trees
                Integer surfaceY = surfaceHeights.get(columnKey(x, z));
                if (surfaceY == null) {
                    surfaceY = findNearestSurfaceY(x, z, baseY);
                }

                int innerX = centerX + (int) Math.round(dx * (radius / (double) floorRadius));
                int innerZ = centerZ + (int) Math.round(dz * (radius / (double) floorRadius));
                Integer innerSurface = surfaceHeights.get(columnKey(innerX, innerZ));
                if (innerSurface == null) {
                    innerSurface = findNearestSurfaceY(innerX, innerZ, baseY);
                }
                if (innerSurface != null) {
                    if (surfaceY == null) {
                        surfaceY = innerSurface;
                    } else {
                        surfaceY = Math.max(surfaceY, innerSurface);
                    }
                }

                // tree placement handled by dedicated angular sampling after wall construction

                if (surfaceY == null) {
                    surfaceY = baseY;
                }
                buildBorderColumn(x, z, surfaceY, fillerMaterial, floorMaterial, wallMaterial, wallHeight);
            }
        }

        // place border trees on an evenly spaced circle (angular sampling)
        String treeResource = "structures/treeModel/bordertree.nbt";
        if (structureHelper.hasStructure(treeResource)) {
            int treeOffset = 1; // inward offset from floorRadius — smaller -> closer to wall
            int treeRadius = Math.max(1, floorRadius - treeOffset);
            int samples = Math.max(48, floorRadius * 8); // more samples -> denser ring
            Set<Long> placedTreeKeys = new HashSet<>();
            for (int i = 0; i < samples; i++) {
                double angle = (Math.PI * 2 * i) / samples;
                int tx = centerX + (int) Math.round(treeRadius * Math.cos(angle));
                int tz = centerZ + (int) Math.round(treeRadius * Math.sin(angle));
                long key = columnKey(tx, tz);
                if (placedTreeKeys.contains(key)) {
                    continue; // deduplicate multiple samples mapping to same block
                }
                if (reservedStructureColumns.contains(key) || roadColumns.contains(key) || mountainColumns.contains(key) || poolColumns.contains(key)) {
                    continue;
                }
                Integer treeSurface = surfaceHeights.get(key);
                if (treeSurface == null) {
                    treeSurface = findNearestSurfaceY(tx, tz, baseY);
                }
                if (treeSurface == null) {
                    continue;
                }
                try {
                    boolean placed = structureHelper.placeStructure(treeResource, activeWorld, tx, treeSurface + 1, tz, random, false, null);
                    plugin.getLogger().log(Level.INFO, "Border tree placement at {0},{1} result={2}", new Object[]{tx, tz, placed});
                    if (placed) {
                        BlockVector size = structureHelper.getStructureSize(treeResource);
                        int footprint = structureHelper.estimateFootprintRadius(size, 3);
                        markStructureFootprint(tx, tz, footprint);
                        reservedStructureColumns.add(key);
                        placedTreeKeys.add(key);
                    }
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Failed to place border tree at {0},{1}: {2}", new Object[]{tx, tz, ex.getMessage()});
                }
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

    private RoadPath buildRoadNetwork(MapTheme theme, int radius) {
        if (activeWorld == null || activeCenter == null) {
            return RoadPath.empty();
        }
        if (!theme.roadPathsEnabled()) {
            return RoadPath.empty();
        }
        List<Material> configuredMaterials = theme.getRoadMaterials();
        Material primaryMaterial = configuredMaterials != null && !configuredMaterials.isEmpty()
                ? configuredMaterials.get(0)
                : resolveBorderMaterial(theme);
        if (primaryMaterial == null || primaryMaterial == Material.AIR) {
            primaryMaterial = Material.COBBLESTONE;
        }

        int halfWidth = Math.max(1, (int) Math.round(radius * 0.04));
        lastRoadHalfWidth = halfWidth;
        activeRoadPalette = buildRoadPalette(configuredMaterials, primaryMaterial);
        RoadPainter painter = this::paintRoadTile;
        RoadBuildContext context = new RoadBuildContext(
                activeCenter.clone(),
                radius,
                halfWidth,
                primaryMaterial,
                random,
                painter
        );
        roadColumns.clear();
        return roadBuilder.buildRoadNetwork(context);
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
        Material tileMaterial = pickRoadMaterial(material);
        setBlock(activeWorld, x, surfaceY, z, tileMaterial);
        clearAboveSurface(x, surfaceY + 1, z);
        roadColumns.add(columnKey(x, z));
        regionMinY = Math.min(regionMinY, surfaceY);
        regionMaxY = Math.max(regionMaxY, surfaceY + 1);
    }

    private List<Material> buildRoadPalette(List<Material> configuredMaterials, Material fallback) {
        List<Material> palette = new ArrayList<>();
        if (configuredMaterials != null) {
            for (Material candidate : configuredMaterials) {
                addPaletteEntry(palette, candidate);
            }
        }
        palette.removeIf(mat -> mat == null || mat == Material.AIR);
        if (palette.isEmpty()) {
            addPaletteEntry(palette, fallback);
        }
        if (palette.isEmpty()) {
            palette.add(Material.COBBLESTONE);
        }
        return palette;
    }

    private void addPaletteEntry(List<Material> palette, Material candidate) {
        if (candidate == null || candidate == Material.AIR) {
            return;
        }
        if (!palette.contains(candidate)) {
            palette.add(candidate);
        }
    }

    private Material pickRoadMaterial(Material fallback) {
        if (activeRoadPalette == null || activeRoadPalette.isEmpty()) {
            return fallback;
        }
        return activeRoadPalette.get(random.nextInt(activeRoadPalette.size()));
    }

    private void tryPlacePool(Material fluid,
                              int radius,
                              MapTheme theme,
                              MapTheme.PoolSettings settings) {
        if (activeCenter == null) {
            return;
        }
        int tries = 12;
        double variance = settings == null ? 1.0 : settings.sizeVariance();
        double sizeMultiplier = Math.max(0.5, variance);
        while (tries-- > 0) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = radius * (0.2 + random.nextDouble() * 0.45);
            int x = activeCenter.getBlockX() + (int) Math.round(distance * Math.cos(angle));
            int z = activeCenter.getBlockZ() + (int) Math.round(distance * Math.sin(angle));
            long key = columnKey(x, z);
            if (isStructureBlocked(x, z) || roadColumns.contains(key) || mountainColumns.contains(key)) {
                continue;
            }
            Integer surfaceY = surfaceHeights.get(columnKey(x, z));
            if (surfaceY == null) {
                surfaceY = findNearestSurfaceY(x, z, activeCenter.getBlockY());
            }
            if (surfaceY == null) {
                continue;
            }
            int baseMaxRadius = Math.max(3, radius / 18);
            int maxRadius = Math.max(3, (int) Math.round(baseMaxRadius * (0.7 + sizeMultiplier)));
            int minRadius = Math.max(2, (int) Math.round(baseMaxRadius * 0.5));
            double roll = random.nextDouble();
            double skew = sizeMultiplier >= 1.0 ? Math.pow(roll, 0.6) : Math.pow(roll, 1.4);
            int majorRadius = Math.max(3, (int) Math.round(minRadius + skew * (maxRadius - minRadius)));
            int minorRadius = Math.max(2, (int) Math.round(majorRadius * (0.6 + random.nextDouble() * 0.3)));
            double rotation = random.nextDouble() * Math.PI * 2;
            carvePoolAt(x, surfaceY, z, fluid, theme, majorRadius, minorRadius, rotation);
            break;
        }
    }

    private void carvePoolAt(int centerX,
                             int surfaceY,
                             int centerZ,
                             Material fluid,
                             MapTheme theme,
                             int majorRadius,
                             int minorRadius,
                             double rotation) {
        if (activeWorld == null) {
            return;
        }
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        int footprintRadius = Math.max(majorRadius, minorRadius) + 2;
        double depthBase = 2.5 + Math.max(majorRadius, minorRadius) * 0.45;
        int worldMin = activeWorld.getMinHeight() + 2;
        int deepestAllowed = Math.max(worldMin, (activeCenter != null ? activeCenter.getBlockY() - 10 : worldMin));
        List<PoolColumn> carvedColumns = new ArrayList<>();
        for (int dx = -footprintRadius; dx <= footprintRadius; dx++) {
            for (int dz = -footprintRadius; dz <= footprintRadius; dz++) {
                double rotatedX = dx * cos - dz * sin;
                double rotatedZ = dx * sin + dz * cos;
                double normalized = (rotatedX * rotatedX) / (majorRadius * majorRadius)
                        + (rotatedZ * rotatedZ) / (minorRadius * minorRadius);
                double wobble = (random.nextDouble() - 0.5) * 0.2;
                double profile = normalized + wobble;
                if (profile > 1.15) {
                    continue;
                }
                double basinStrength = Math.max(0.0, 1.05 - profile);
                if (basinStrength <= 0.0) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                long key = columnKey(x, z);
                if (reservedStructureColumns.contains(key)
                        || roadColumns.contains(key)
                        || mountainColumns.contains(key)) {
                    continue;
                }
                Integer columnSurface = surfaceHeights.get(key);
                if (columnSurface == null) {
                    columnSurface = findNearestSurfaceY(x, z, surfaceY);
                }
                if (columnSurface == null) {
                    continue;
                }
                double depthFactor = basinStrength * (0.65 + random.nextDouble() * 0.25);
                int totalDepth = Math.max(1, (int) Math.round(depthBase * depthFactor));
                totalDepth = Math.max(1, totalDepth - POOL_FLOOR_LIFT_BLOCKS);
                int maxDepthFromSurface = columnSurface - deepestAllowed;
                if (maxDepthFromSurface <= 0) {
                    continue;
                }
                totalDepth = Math.min(totalDepth, maxDepthFromSurface);
                int maxDigDepth = Math.max(1, columnSurface - worldMin);
                totalDepth = Math.min(totalDepth, maxDigDepth);
                if (totalDepth <= 0) {
                    continue;
                }
                if (basinStrength < 0.2) {
                    int shorelineSurface = Math.max(worldMin, columnSurface - 1);
                    shapeShorelineColumn(x, z, columnSurface, shorelineSurface, theme);
                    continue;
                }
                int surfaceDrop = Math.max(1, (int) Math.round(totalDepth * 0.4));
                int loweredSurface = Math.max(worldMin, columnSurface - surfaceDrop);
                int fluidDepth = Math.max(1, totalDepth - surfaceDrop + 1);
                hollowColumn(x, z, columnSurface, loweredSurface);
                placeFluidColumn(x, loweredSurface, z, fluidDepth, fluid, theme);
                int fluidBottom = loweredSurface - (fluidDepth - 1);
                carvedColumns.add(new PoolColumn(x, z, loweredSurface, Math.max(worldMin, fluidBottom - 1)));
            }
        }
        fortifyPoolBoundary(carvedColumns, theme);
        regionMinY = Math.min(regionMinY, surfaceY - (int) Math.round(depthBase) - 2);
    }

    private void placeFluidColumn(int x,
                                  int surfaceY,
                                  int z,
                                  int depth,
                                  Material fluid,
                                  MapTheme theme) {
        if (activeWorld == null) {
            return;
        }
        int minY = activeWorld.getMinHeight();
        int layers = Math.max(1, depth);
        for (int i = 0; i < layers; i++) {
            int targetY = surfaceY - i;
            if (targetY < minY) {
                break;
            }
            setBlock(activeWorld, x, targetY, z, fluid);
            markPoolColumn(x, z);
            regionMinY = Math.min(regionMinY, targetY);
        }
        int baseY = surfaceY - layers;
        Material filler = theme.getFillerMaterial();
        if (filler == null || filler == Material.AIR) {
            filler = Material.STONE;
        }
        if (baseY >= minY) {
            setBlock(activeWorld, x, baseY, z, filler);
            regionMinY = Math.min(regionMinY, baseY);
        }
        surfaceHeights.put(columnKey(x, z), surfaceY);
        regionMaxY = Math.max(regionMaxY, surfaceY + 1);
    }

    private void shapeShorelineColumn(int x,
                                      int z,
                                      int originalSurfaceY,
                                      int newSurfaceY,
                                      MapTheme theme) {
        if (activeWorld == null) {
            return;
        }
        if (newSurfaceY > originalSurfaceY) {
            return;
        }
        int minY = activeWorld.getMinHeight();
        newSurfaceY = Math.max(minY, newSurfaceY);
        Material filler = theme.getFillerMaterial();
        if (filler == null || filler == Material.AIR) {
            filler = Material.DIRT;
        }
        Material top = theme.getTopMaterial();
        if (top == null || top == Material.AIR) {
            top = filler;
        }
        for (int y = originalSurfaceY; y >= newSurfaceY; y--) {
            Material material = y == newSurfaceY ? top : filler;
            setBlock(activeWorld, x, y, z, material);
        }
        clearPoolColumnAbove(x, newSurfaceY + 1, z);
        markPoolColumn(x, z);
        surfaceHeights.put(columnKey(x, z), newSurfaceY);
        regionMinY = Math.min(regionMinY, newSurfaceY - 1);
        regionMaxY = Math.max(regionMaxY, originalSurfaceY + 1);
    }

    private void hollowColumn(int x, int z, int originalSurfaceY, int targetSurfaceY) {
        if (activeWorld == null) {
            return;
        }
        if (targetSurfaceY > originalSurfaceY) {
            return;
        }
        int minY = Math.max(activeWorld.getMinHeight(), targetSurfaceY);
        for (int y = originalSurfaceY; y >= minY; y--) {
            setBlock(activeWorld, x, y, z, Material.AIR);
        }
        clearPoolColumnAbove(x, originalSurfaceY + 1, z);
        regionMaxY = Math.max(regionMaxY, originalSurfaceY + 1);
    }

    private void clearPoolColumnAbove(int x, int startY, int z) {
        if (activeWorld == null) {
            return;
        }
        int maxY = activeWorld.getMaxHeight();
        int capped = Math.min(maxY, startY + 5);
        for (int y = startY; y <= capped; y++) {
            Block block = activeWorld.getBlockAt(x, y, z);
            if (block.isEmpty()) {
                continue;
            }
            setBlock(activeWorld, x, y, z, Material.AIR);
        }
    }

    private void fortifyPoolBoundary(List<PoolColumn> carvedColumns, MapTheme theme) {
        if (activeWorld == null || carvedColumns == null || carvedColumns.isEmpty()) {
            return;
        }
        Material filler = theme.getFillerMaterial();
        if (filler == null || filler == Material.AIR) {
            filler = Material.STONE;
        }
        Material cap = theme.getTopMaterial();
        if (cap == null || cap == Material.AIR) {
            cap = filler;
        }
        Set<Long> carvedKeys = new HashSet<>();
        for (PoolColumn column : carvedColumns) {
            carvedKeys.add(columnKey(column.x(), column.z()));
        }
        int[][] offsets = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
        for (PoolColumn column : carvedColumns) {
            for (int[] offset : offsets) {
                int nx = column.x() + offset[0];
                int nz = column.z() + offset[1];
                long key = columnKey(nx, nz);
                if (carvedKeys.contains(key)) {
                    continue;
                }
                int sealTop = column.topY();
                int sealBottom = Math.max(activeWorld.getMinHeight(), column.bottomY());
                for (int y = sealTop; y >= sealBottom; y--) {
                    Block block = activeWorld.getBlockAt(nx, y, nz);
                    Material type = block.getType();
                    if (type.isSolid() && type != Material.WATER && type != Material.LAVA) {
                        continue;
                    }
                    Material material = y >= sealTop ? cap : filler;
                    setBlock(activeWorld, nx, y, nz, material);
                    markPoolColumn(nx, nz);
                    if (y == sealTop) {
                        surfaceHeights.put(key, sealTop);
                    }
                    regionMinY = Math.min(regionMinY, y - 1);
                    regionMaxY = Math.max(regionMaxY, sealTop + 1);
                }
            }
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
        if (activeCenter == null || radius <= 0) {
            return slices;
        }
        int centerX = activeCenter.getBlockX();
        int centerZ = activeCenter.getBlockZ();
        int radiusSquared = radius * radius;
        List<ColumnWork> allColumns = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > radiusSquared) {
                    continue;
                }
                double normalizedDistance = Math.sqrt(distanceSquared) / radius;
                boolean edge = normalizedDistance > 0.9;
                int x = centerX + dx;
                int z = centerZ + dz;
                allColumns.add(new ColumnWork(x, z, normalizedDistance, edge));
            }
        }
        allColumns.sort((a, b) -> Double.compare(a.normalizedDistance(), b.normalizedDistance()));
        int index = 0;
        while (index < allColumns.size()) {
            int end = Math.min(allColumns.size(), index + GENERATION_COLUMNS_PER_SLICE);
            slices.add(new ArrayList<>(allColumns.subList(index, end)));
            index = end;
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
        columns.sort((a, b) -> {
            long distA = columnDistanceSquared(a, centerX, centerZ);
            long distB = columnDistanceSquared(b, centerX, centerZ);
            return Long.compare(distA, distB);
        });
        return columns;
    }

    private long columnDistanceSquared(ColumnCoordinate column, int centerX, int centerZ) {
        long dx = column.x() - centerX;
        long dz = column.z() - centerZ;
        return (dx * dx) + (dz * dz);
    }

    private void resetRegionState() {
        touchedBlocks.clear();
        surfaceHeights.clear();
        spawnMarkerLocations.clear();
        reservedStructureColumns.clear();
        roadColumns.clear();
        mountainColumns.clear();
        poolColumns.clear();
        activeRoadPalette = new ArrayList<>();
        lastRoadHalfWidth = 0;
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
            generateMountains(theme, radius);
            RoadPath roadPath = buildRoadNetwork(theme, radius);
            placeRoadsideStructures(theme, roadPath, radius);
            buildTopCover(theme, radius, baseY);
            placePools(theme, radius);
            placeStructures(theme, radius);
            placePathBeacons(roadPath);
            placeSpawnMarkers(playerCount, roadPath, radius);
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
            reservedStructureColumns.clear();
            roadColumns.clear();
            mountainColumns.clear();
            poolColumns.clear();
            activeRoadPalette = new ArrayList<>();
            lastRoadHalfWidth = 0;
            regionMinY = Integer.MAX_VALUE;
            regionMaxY = Integer.MIN_VALUE;
            activeCenter = null;
            activeWorld = null;
            lastTheme = null;
            lastRadius = 0;
            error.accept("Failed to generate map. Check server logs.");
        }
    }

    private record TemplateResolved(MapTheme.StructureTemplate template,
                                    String resourcePath,
                                    int footprintRadius,
                                    int structureHeight) {
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

    private record PoolColumn(int x, int z, int topY, int bottomY) {
    }
    private static String blockKey(UUID worldId, int x, int y, int z) {
        return worldId + ":" + x + ":" + y + ":" + z;
    }

    private static long columnKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

}
