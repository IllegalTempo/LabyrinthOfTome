package com.yourfault.map;

import org.bukkit.Material;

import java.util.List;
import java.util.Locale;
import java.util.Random;

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
            0.18,
            List.of(
                decoration(Material.OAK_SAPLING, 1.0),
                decoration(Material.FERN, 1.3),
                decoration(Material.FLOWERING_AZALEA, 0.7)
            ),
            TerrainProfile.DEFAULT,
            true,
            false,
            true,
            true,
            Material.GLASS,
            true,
            Material.MOSSY_COBBLESTONE
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
            false,
            true,
            true,
            false,
            Material.BLACK_STAINED_GLASS,
            false,
            Material.POLISHED_BASALT
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
            false,
            false,
            true,
            false,
            Material.GLASS,
            true,
            Material.PURPUR_BLOCK
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
            true,
            false,
            true,
            true,
            Material.GLASS,
            true,
            Material.SMOOTH_SANDSTONE
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
            true,
            false,
            true,
            false,
            Material.GLASS,
            true,
            Material.COBBLESTONE
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
            true,
            false,
            true,
            false,
            Material.BLUE_STAINED_GLASS,
            true,
            Material.PACKED_ICE
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
            true,
            false,
            true,
            true,
            Material.JUNGLE_LEAVES,
            true,
            Material.MOSS_CARPET
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
            true,
            false,
            true,
            false,
            Material.GLASS,
            true,
            Material.SMOOTH_SANDSTONE
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
            true,
            false,
            true,
            false,
            Material.GLASS,
            true,
            Material.MUD_BRICKS
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
            true,
            false,
            true,
            false,
            Material.PACKED_ICE,
            true,
            Material.POLISHED_DEEPSLATE
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
    private final boolean waterPools;
    private final boolean lavaPools;
    private final boolean borderEnabled;
    private final boolean topCoverEnabled;
    private final Material topCoverMaterial;
    private final boolean roadPathsEnabled;
    private final Material roadMaterial;

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
             boolean waterPools,
             boolean lavaPools,
             boolean borderEnabled,
             boolean topCoverEnabled,
             Material topCoverMaterial,
             boolean roadPathsEnabled,
             Material roadMaterial) {
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
        this.waterPools = waterPools;
        this.lavaPools = lavaPools;
        this.borderEnabled = borderEnabled;
        this.topCoverEnabled = topCoverEnabled;
        this.topCoverMaterial = topCoverMaterial;
        this.roadPathsEnabled = roadPathsEnabled;
        this.roadMaterial = roadMaterial;
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

    public Material getRoadMaterial() {
        return roadMaterial;
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
}
