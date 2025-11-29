package com.yourfault.map;

import org.bukkit.Material;

import java.util.*;

/**
 * Defines the available biome-inspired themes for procedurally generated PvE arenas.
 */
public enum MapTheme {
    FOREST(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.MOSSY_COBBLESTONE,
            Material.MOSS_CARPET,
            0.07,
            3.5,
            3,
            0.01,
            List.of(
                    decoration(Material.DIRT_PATH, 0),
                    decoration(Material.FERN, 0.01),
                    decoration(Material.FLOWERING_AZALEA, 0.01)
            ),
            TerrainProfile.DEFAULT,
            PoolSettings.of(1.0, 1.2),
            true,
            false,
            true,
            false,
            Material.GLASS,
            true,
            roadMaterials(Material.MOSSY_COBBLESTONE, Material.COARSE_DIRT, Material.DIRT_PATH),
            StructureSettings.ofTemplates(
                    StructureTemplate.template("structures/decoration/watchtower.nbt", 10),
                    StructureTemplate.template("structures/treeModel/oaktree_6_7_5.nbt", 10),
                    StructureTemplate.template("structures/treeModel/oaktree_12_13_11.nbt", 10),
                    StructureTemplate.template("structures/treeModel/willotree_9_10_10.nbt", 10),
                    StructureTemplate.template("structures/treeModel/cherrytree_12_10_12.nbt", 10),
                    StructureTemplate.template("structures/decoration/smallshop.nbt", 10)
            ),
            MountainSettings.of(3, 35),
            true,
            List.of(
                    StructureTemplate.template("structures/lampPost/light1.nbt", 64)
                            .withFootprintRadius(2)
                            .withEstimatedHeight(7)
                            .withRotations(
                                    StructureTemplate.Rotation.NONE,
                                    StructureTemplate.Rotation.CLOCKWISE_90,
                                    StructureTemplate.Rotation.CLOCKWISE_180,
                                    StructureTemplate.Rotation.COUNTERCLOCKWISE_90
                            ),
                    StructureTemplate.template("structures/lampPost/light2.nbt", 64)
                            .withFootprintRadius(2)
                            .withEstimatedHeight(7)
                            .withRotations(
                                    StructureTemplate.Rotation.NONE,
                                    StructureTemplate.Rotation.CLOCKWISE_90,
                                    StructureTemplate.Rotation.CLOCKWISE_180,
                                    StructureTemplate.Rotation.COUNTERCLOCKWISE_90
                            )
            )
    ),
    NETHER(
            Material.CRIMSON_NYLIUM,
            Material.NETHERRACK,
            Material.POLISHED_BASALT,
            Material.CRIMSON_ROOTS,
            0.065,
            4.5,
            4,
            0.14,
            List.of(
                    decoration(Material.NETHER_WART_BLOCK, 1.0),
                    decoration(Material.SHROOMLIGHT, 0.8),
                    decoration(Material.CRIMSON_FUNGUS, 0.6)
            ),
            TerrainProfile.DEFAULT,
            PoolSettings.of(0.7, 1.1),
            false,
            true,
            true,
            false,
            Material.BLACK_STAINED_GLASS,
            true,
            roadMaterials(Material.POLISHED_BASALT, Material.BLACKSTONE, Material.CRIMSON_NYLIUM),
            StructureSettings.ofTemplates(
                    StructureTemplate.template("structures/decoration/watchtower.nbt", 2),
                    StructureTemplate.template("structures/treeModel/wintertree.nbt", 2)
            ),
            MountainSettings.disabled(),
            true,
            List.of(
                    StructureTemplate.template("structures/lampPost/light3nether.nbt", 64)
                            .withFootprintRadius(2)
                            .withEstimatedHeight(7)
                            .withRotations(
                                    StructureTemplate.Rotation.NONE,
                                    StructureTemplate.Rotation.CLOCKWISE_90,
                                    StructureTemplate.Rotation.CLOCKWISE_180,
                                    StructureTemplate.Rotation.COUNTERCLOCKWISE_90
                            )
            )
    ),
    END(
            Material.END_STONE,
            Material.END_STONE,
            Material.OBSIDIAN,
            Material.CHORUS_FLOWER,
            0.045,
            2.8,
            3,
            0.11,
            List.of(
                    decoration(Material.CHORUS_PLANT, 1.0),
                    decoration(Material.PURPUR_PILLAR, 0.9)
            ),
            TerrainProfile.DEFAULT,
            PoolSettings.of(0.4, 0.9),
            false,
            false,
            true,
            false,
            Material.GLASS,
            true,
            roadMaterials(Material.OBSIDIAN, Material.PURPUR_BLOCK, Material.END_STONE_BRICKS),
            StructureSettings.ofTemplates(
                    StructureTemplate.template("structures/decoration/watchtower.nbt", 3),
                    StructureTemplate.template("structures/treeModel/wintertree.nbt", 5)
            ),
            MountainSettings.disabled(),
            false,
            List.of()
    ),
    DESERT(
            Material.SAND,
            Material.SANDSTONE,
            Material.SMOOTH_SANDSTONE,
            Material.DEAD_BUSH,
            0.06,
            2.4,
            4,
            0.09,
            List.of(
                    decoration(Material.CACTUS, 0.7),
                    decoration(Material.CUT_SANDSTONE, 0.5),
                    decoration(Material.DEAD_BUSH, 1.2)
            ),
            TerrainProfile.DEFAULT,
            PoolSettings.of(0.6, 1.5),
            true,
            false,
            true,
            true,
            Material.GLASS,
            true,
            roadMaterials(Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE, Material.SANDSTONE),
            StructureSettings.template("structures/decoration/watchtower.nbt"),
            MountainSettings.disabled(),
            false,
            List.of()
    ),
    MOUNTAINS(
            Material.STONE,
            Material.STONE,
            Material.POLISHED_ANDESITE,
            Material.SNOW,
            0.085,
            5.5,
            5,
            0.06,
            List.of(
                    decoration(Material.COBBLED_DEEPSLATE, 1.0),
                    decoration(Material.PACKED_ICE, 0.8),
                    decoration(Material.COBBLESTONE_STAIRS, 0.6)
            ),
            TerrainProfile.DEFAULT,
            PoolSettings.of(0.8, 0.9),
            true,
            false,
            true,
            false,
            Material.GLASS,
            true,
            roadMaterials(Material.COBBLESTONE, Material.COBBLED_DEEPSLATE, Material.POLISHED_ANDESITE),
            StructureSettings.template("structures/decoration/watchtower.nbt"),
            MountainSettings.of(3, 36),
            false,
            List.of()
    ),
    GLACIER(
            Material.SNOW_BLOCK,
            Material.PACKED_ICE,
            Material.BLUE_ICE,
            Material.ICE,
            0.05,
            4.2,
            4,
            0.1,
            List.of(
                    decoration(Material.SNOW, 1.2),
                    decoration(Material.ICE, 0.8),
                    decoration(Material.BLUE_ICE, 0.5)
            ),
            TerrainProfile.REAL_MOUNTAIN,
            PoolSettings.of(0.9, 1.3),
            true,
            false,
            true,
            false,
            Material.BLUE_STAINED_GLASS,
            true,
            roadMaterials(Material.PACKED_ICE, Material.BLUE_ICE, Material.SNOW_BLOCK),
            StructureSettings.template("structures/decoration/watchtower.nbt"),
            MountainSettings.of(2, 34),
            false,
            List.of()
    ),
    TROPICAL_RAINFOREST(
            Material.GRASS_BLOCK,
            Material.PODZOL,
            Material.MOSSY_STONE_BRICKS,
            Material.JUNGLE_LEAVES,
            0.07,
            3.2,
            3,
            0.22,
            List.of(
                    decoration(Material.JUNGLE_SAPLING, 1.0),
                    decoration(Material.BAMBOO, 0.8),
                    decoration(Material.VINE, 0.6)
            ),
            TerrainProfile.DEFAULT,
            PoolSettings.of(1.5, 1.8),
            true,
            false,
            true,
            true,
            Material.JUNGLE_LEAVES,
            true,
            roadMaterials(Material.MOSSY_STONE_BRICKS, Material.MOSS_BLOCK, Material.JUNGLE_PLANKS),
            StructureSettings.template("structures/decoration/watchtower.nbt"),
            MountainSettings.disabled(),
            false,
            List.of()
    ),
    SAVANNA(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.CUT_SANDSTONE,
            Material.ACACIA_LEAVES,
            0.06,
            2.6,
            3,
            0.15,
            List.of(
                    decoration(Material.ACACIA_SAPLING, 1.0),
                    decoration(Material.DEAD_BUSH, 0.9),
                    decoration(Material.HAY_BLOCK, 0.6)
            ),
            TerrainProfile.DEFAULT,
            PoolSettings.of(0.5, 1.0),
            true,
            false,
            true,
            false,
            Material.GLASS,
            true,
            roadMaterials(Material.CUT_SANDSTONE, Material.SMOOTH_SANDSTONE, Material.TERRACOTTA),
            StructureSettings.template("structures/decoration/watchtower.nbt"),
            MountainSettings.disabled(),
            false,
            List.of()
    ),
    WETLANDS(
            Material.MUD,
            Material.PACKED_MUD,
            Material.MANGROVE_ROOTS,
            Material.MANGROVE_LEAVES,
            0.05,
            2.2,
            4,
            0.2,
            List.of(
                    decoration(Material.MANGROVE_PROPAGULE, 1.0),
                    decoration(Material.LILY_PAD, 0.8),
                    decoration(Material.TALL_GRASS, 0.7)
            ),
            TerrainProfile.DEFAULT,
            PoolSettings.of(1.8, 2.0),
            true,
            false,
            true,
            false,
            Material.GLASS,
            true,
            roadMaterials(Material.MUD_BRICKS, Material.MUD, Material.ROOTED_DIRT),
            StructureSettings.template("structures/decoration/watchtower.nbt"),
            MountainSettings.disabled(),
            false,
            List.of()
    ),
    REAL_MOUNTAIN(
            Material.STONE,
            Material.STONE,
            Material.POLISHED_DEEPSLATE,
            Material.SNOW,
            0.12,
            10.0,
            6,
            0.04,
            List.of(
                    decoration(Material.SPRUCE_SAPLING, 0.8),
                    decoration(Material.COBBLESTONE_WALL, 0.6),
                    decoration(Material.PACKED_ICE, 0.5)
            ),
            TerrainProfile.REAL_MOUNTAIN,
            PoolSettings.of(0.7, 1.0),
            true,
            false,
            true,
            false,
            Material.PACKED_ICE,
            true,
            roadMaterials(Material.POLISHED_DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.STONE),
            StructureSettings.template("structures/decoration/watchtower.nbt"),
            MountainSettings.of(4, 44),
            false,
            List.of()
    );


    private final Material topMaterial;
    private final Material fillerMaterial;
    private final Material borderMaterial;
    private final Material accentMaterial;
    private final double noiseScale;
    private final double heightVariance;
    private final int fillerDepth;
    private final double decorationChance;
    private final List<DecorationOption> decorations;
    private final TerrainProfile terrainProfile;
    private final PoolSettings poolSettings;
    private final boolean waterPools;
    private final boolean lavaPools;
    private final boolean borderEnabled;
    private final boolean topCoverEnabled;
    private final Material topCoverMaterial;
    private final boolean roadPathsEnabled;
    private final List<Material> roadMaterials;
    private final StructureSettings structureSettings;
    private final boolean lampPostsEnabled;
    private final List<StructureTemplate> lampPostTemplates;
    private final MountainSettings mountainSettings;

    MapTheme(Material topMaterial,
             Material fillerMaterial,
             Material borderMaterial,
             Material accentMaterial,
             double noiseScale,
             double heightVariance,
             int fillerDepth,
             double decorationChance,
             List<DecorationOption> decorations,
             TerrainProfile terrainProfile,
             PoolSettings poolSettings,
             boolean waterPools,
             boolean lavaPools,
             boolean borderEnabled,
             boolean topCoverEnabled,
             Material topCoverMaterial,
             boolean roadPathsEnabled,
             List<Material> roadMaterials,
             StructureSettings structureSettings,
             MountainSettings mountainSettings,
             boolean lampPostsEnabled,
             List<StructureTemplate> lampPostTemplates) {
        this.topMaterial = topMaterial;
        this.fillerMaterial = fillerMaterial;
        this.borderMaterial = borderMaterial;
        this.accentMaterial = accentMaterial;
        this.noiseScale = noiseScale;
        this.heightVariance = heightVariance;
        this.fillerDepth = fillerDepth;
        this.decorationChance = decorationChance;
        this.decorations = decorations;
        this.terrainProfile = terrainProfile;
        this.poolSettings = poolSettings == null ? PoolSettings.defaultSettings() : poolSettings;
        this.waterPools = waterPools;
        this.lavaPools = lavaPools;
        this.borderEnabled = borderEnabled;
        this.topCoverEnabled = topCoverEnabled;
        this.topCoverMaterial = topCoverMaterial;
        this.roadPathsEnabled = roadPathsEnabled;
        this.roadMaterials = roadMaterials == null ? List.of() : List.copyOf(roadMaterials);
        this.structureSettings = structureSettings == null ? StructureSettings.disabled() : structureSettings;
        this.lampPostsEnabled = lampPostsEnabled;
        this.lampPostTemplates = lampPostTemplates == null ? List.of() : List.copyOf(lampPostTemplates);
        this.mountainSettings = mountainSettings == null ? MountainSettings.disabled() : mountainSettings;
    }

    public Material getTopMaterial() {
        return topMaterial;
    }

    public Material getFillerMaterial() {
        return fillerMaterial;
    }

    public Material getBorderMaterial() {
        return borderMaterial;
    }

    public Material getAccentMaterial() {
        return accentMaterial;
    }

    public double getNoiseScale() {
        return noiseScale;
    }

    public double getHeightVariance() {
        return heightVariance;
    }

    public int getFillerDepth() {
        return fillerDepth;
    }

    public double getDecorationChance() {
        return decorationChance;
    }

    public TerrainProfile getTerrainProfile() {
        return terrainProfile;
    }

    public PoolSettings getPoolSettings() {
        return poolSettings;
    }

    public boolean allowsWaterPools() {
        return waterPools;
    }

    public boolean allowsLavaPools() {
        return lavaPools;
    }

    public boolean hasBorder() {
        return borderEnabled;
    }

    public boolean hasTopCover() {
        return topCoverEnabled;
    }

    public Material getTopCoverMaterial() {
        return topCoverMaterial;
    }

    public boolean roadPathsEnabled() {
        return roadPathsEnabled;
    }

    public List<Material> getRoadMaterials() {
        return roadMaterials;
    }

    public StructureSettings getStructureSettings() {
        return structureSettings;
    }

    public boolean lampPostsEnabled() {
        return lampPostsEnabled && !lampPostTemplates.isEmpty();
    }

    public List<StructureTemplate> getLampPostTemplates() {
        return lampPostTemplates;
    }

    public MountainSettings getMountainSettings() {
        return mountainSettings;
    }

    public Material pickRandomDecoration(Random random) {
        if (decorations.isEmpty()) {
            return null;
        }
        double total = decorations.stream()
                .mapToDouble(option -> Math.max(0.01, option.weight()))
                .sum();
        if (total <= 0) {
            return decorations.get(random.nextInt(decorations.size())).material();
        }
        double roll = random.nextDouble() * total;
        double cumulative = 0.0;
        for (DecorationOption option : decorations) {
            cumulative += Math.max(0.01, option.weight());
            if (roll <= cumulative) {
                return option.material();
            }
        }
        return decorations.get(decorations.size() - 1).material();
    }

    public static final class StructureSettings {
        private final boolean enabled;
        private final List<StructureTemplate> templates;

        private StructureSettings(boolean enabled, List<StructureTemplate> templates) {
            this.enabled = enabled;
            this.templates = templates == null ? List.of() : List.copyOf(templates);
        }

        public static StructureSettings disabled() {
            return new StructureSettings(false, List.of());
        }

        public static StructureSettings template(String resourcePath) {
            return ofTemplates(StructureTemplate.template(resourcePath));
        }

        public static StructureSettings ofTemplates(StructureTemplate... templates) {
            if (templates == null || templates.length == 0) {
                return disabled();
            }
            List<StructureTemplate> list = new ArrayList<>();
            for (StructureTemplate template : templates) {
                if (template != null) {
                    list.add(template);
                }
            }
            if (list.isEmpty()) {
                return disabled();
            }
            return new StructureSettings(true, list);
        }

        public StructureSettings addTemplate(StructureTemplate template) {
            if (template == null) {
                return this;
            }
            List<StructureTemplate> list = new ArrayList<>(templates);
            list.add(template);
            return new StructureSettings(true, list);
        }

        public boolean enabled() {
            return enabled && !templates.isEmpty();
        }

        public List<StructureTemplate> templates() {
            return templates;
        }

        public StructureTemplate pickTemplate(Random random) {
            if (!enabled()) {
                return null;
            }
            if (templates.size() == 1 || random == null) {
                return templates.get(0);
            }
            double total = 0.0;
            for (StructureTemplate template : templates) {
                total += Math.max(0.01, template.weight());
            }
            double roll = random.nextDouble() * total;
            double cumulative = 0.0;
            for (StructureTemplate template : templates) {
                cumulative += Math.max(0.01, template.weight());
                if (roll <= cumulative) {
                    return template;
                }
            }
            return templates.get(templates.size() - 1);
        }
    }

    public static final class StructureTemplate {
        private final String resourcePath;
        private final boolean includeEntities;
        private final int fallbackFootprintRadius;
        private final int estimatedHeight;
        private final double weight;
        private final int maxPlacements;
        private final EnumSet<Rotation> rotations;
        private final Rotation[] rotationPool;

        private StructureTemplate(String resourcePath,
                                  boolean includeEntities,
                                  int fallbackFootprintRadius,
                                  int estimatedHeight,
                                  double weight,
                                  int maxPlacements,
                                  EnumSet<Rotation> rotations) {
            this.resourcePath = resourcePath;
            this.includeEntities = includeEntities;
            this.fallbackFootprintRadius = Math.max(1, fallbackFootprintRadius);
            this.estimatedHeight = Math.max(1, estimatedHeight);
            this.weight = weight <= 0 ? 1.0 : weight;
            this.maxPlacements = Math.max(0, maxPlacements);
            EnumSet<Rotation> normalized = rotations == null || rotations.isEmpty()
                    ? EnumSet.of(Rotation.NONE)
                    : EnumSet.copyOf(rotations);
            this.rotations = normalized;
            this.rotationPool = normalized.toArray(new Rotation[0]);
        }

        public static StructureTemplate template(String resourcePath) {
            return new StructureTemplate(resourcePath, false, 4, 24, 1.0, 1, EnumSet.of(Rotation.NONE));
        }

        public static StructureTemplate template(String resourcePath, int maxPlacements) {
            return new StructureTemplate(resourcePath, false, 4, 24, 1.0, maxPlacements, EnumSet.of(Rotation.NONE));
        }

        public StructureTemplate withIncludeEntities(boolean includeEntities) {
            return new StructureTemplate(resourcePath, includeEntities, fallbackFootprintRadius, estimatedHeight, weight, maxPlacements, rotations);
        }

        public StructureTemplate withFootprintRadius(int radius) {
            return new StructureTemplate(resourcePath, includeEntities, Math.max(1, radius), estimatedHeight, weight, maxPlacements, rotations);
        }

        public StructureTemplate withEstimatedHeight(int height) {
            return new StructureTemplate(resourcePath, includeEntities, fallbackFootprintRadius, Math.max(1, height), weight, maxPlacements, rotations);
        }

        public StructureTemplate withWeight(double weight) {
            return new StructureTemplate(resourcePath, includeEntities, fallbackFootprintRadius, estimatedHeight, weight, maxPlacements, rotations);
        }

        public StructureTemplate withMaxPlacements(int maxPlacements) {
            return new StructureTemplate(resourcePath, includeEntities, fallbackFootprintRadius, estimatedHeight, weight, maxPlacements, rotations);
        }

        public StructureTemplate withRotations(Rotation... rotations) {
            return new StructureTemplate(resourcePath, includeEntities, fallbackFootprintRadius, estimatedHeight, weight, maxPlacements, normalizeRotations(rotations));
        }

        public String resourcePath() {
            return resourcePath;
        }

        public boolean includeEntities() {
            return includeEntities;
        }

        public int fallbackFootprintRadius() {
            return fallbackFootprintRadius;
        }

        public int estimatedHeight() {
            return estimatedHeight;
        }

        public double weight() {
            return weight;
        }

        public int maxPlacements() {
            return maxPlacements;
        }

        public EnumSet<Rotation> rotations() {
            return EnumSet.copyOf(rotations);
        }

        public Rotation pickRotation(Random random) {
            if (rotationPool.length == 0) {
                return Rotation.NONE;
            }
            if (rotationPool.length == 1 || random == null) {
                return rotationPool[0];
            }
            return rotationPool[random.nextInt(rotationPool.length)];
        }

        private static EnumSet<Rotation> normalizeRotations(Rotation... candidates) {
            if (candidates == null || candidates.length == 0) {
                return EnumSet.of(Rotation.NONE);
            }
            EnumSet<Rotation> set = EnumSet.noneOf(Rotation.class);
            for (Rotation candidate : candidates) {
                if (candidate != null) {
                    set.add(candidate);
                }
            }
            return set.isEmpty() ? EnumSet.of(Rotation.NONE) : set;
        }

        public enum Rotation {
            NONE,
            CLOCKWISE_90,
            CLOCKWISE_180,
            COUNTERCLOCKWISE_90
        }
    }

    public static MapTheme pickRandom(Random random) {
        MapTheme[] values = values();
        return values[random.nextInt(values.length)];
    }

    public static MapTheme findByName(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        for (MapTheme theme : values()) {
            if (theme.name().equals(normalized)) {
                return theme;
            }
        }
        return null;
    }

    public enum TerrainProfile {
        DEFAULT,
        REAL_MOUNTAIN
    }

    public static DecorationOption decoration(Material material, double weight) {
        return new DecorationOption(material, weight);
    }

    public static List<Material> roadMaterials(Material... materials) {
        if (materials == null || materials.length == 0) {
            return List.of();
        }
        List<Material> list = new ArrayList<>();
        for (Material material : materials) {
            if (material == null || material == Material.AIR) {
                continue;
            }
            if (!list.contains(material)) {
                list.add(material);
            }
        }
        return list.isEmpty() ? List.of() : List.copyOf(list);
    }

    public static final class DecorationOption {
        private final Material material;
        private final double weight;

        private DecorationOption(Material material, double weight) {
            this.material = material;
            this.weight = weight;
        }

        public Material material() {
            return material;
        }

        public double weight() {
            return weight;
        }
    }

    public static final class PoolSettings {
        private final double spawnWeight;
        private final double sizeVariance;

        private PoolSettings(double spawnWeight, double sizeVariance) {
            this.spawnWeight = Math.max(0.1, spawnWeight);
            this.sizeVariance = Math.max(0.5, sizeVariance);
        }

        public static PoolSettings defaultSettings() {
            return new PoolSettings(1.0, 1.0);
        }

        public static PoolSettings of(double spawnWeight, double sizeVariance) {
            return new PoolSettings(spawnWeight, sizeVariance);
        }

        public double spawnWeight() {
            return spawnWeight;
        }

        public double sizeVariance() {
            return sizeVariance;
        }
    }

    public static final class MountainSettings {
        private final boolean enabled;
        private final int peakCount;
        private final int maxHeight;

        private MountainSettings(boolean enabled, int peakCount, int maxHeight) {
            this.enabled = enabled;
            this.peakCount = enabled ? Math.max(1, peakCount) : 0;
            this.maxHeight = enabled ? Math.max(20, maxHeight) : 0;
        }

        public static MountainSettings disabled() {
            return new MountainSettings(false, 0, 0);
        }

        public static MountainSettings of(int peakCount, int maxHeight) {
            return new MountainSettings(true, peakCount, maxHeight);
        }

        public boolean enabled() {
            return enabled;
        }

        public int peakCount() {
            return peakCount;
        }

        public int maxHeight() {
            return maxHeight;
        }
    }
}
