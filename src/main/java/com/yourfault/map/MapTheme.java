package com.yourfault.map;

import org.bukkit.Material;

import java.util.List;
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
            List.of(Material.OAK_SAPLING, Material.FERN, Material.FLOWERING_AZALEA),
            TerrainProfile.DEFAULT
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
            List.of(Material.NETHER_WART_BLOCK, Material.SHROOMLIGHT, Material.CRIMSON_FUNGUS),
            TerrainProfile.DEFAULT
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
            List.of(Material.CHORUS_PLANT, Material.PURPUR_PILLAR),
            TerrainProfile.DEFAULT
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
            List.of(Material.CACTUS, Material.CUT_SANDSTONE),
            TerrainProfile.DEFAULT
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
            List.of(Material.COBBLED_DEEPSLATE, Material.PACKED_ICE, Material.COBBLESTONE_STAIRS),
            TerrainProfile.DEFAULT
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
            List.of(Material.SPRUCE_SAPLING, Material.COBBLESTONE_WALL, Material.PACKED_ICE),
            TerrainProfile.REAL_MOUNTAIN
    );

    private final Material topMaterial;
    private final Material fillerMaterial;
    private final Material borderMaterial;
    private final Material accentMaterial;
    private final double noiseScale;
    private final double heightVariance;
    private final int fillerDepth;
    private final double decorationChance;
    private final List<Material> decorations;
    private final TerrainProfile terrainProfile;

    MapTheme(Material topMaterial,
             Material fillerMaterial,
             Material borderMaterial,
             Material accentMaterial,
             double noiseScale,
             double heightVariance,
             int fillerDepth,
             double decorationChance,
             List<Material> decorations,
             TerrainProfile terrainProfile) {
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

    public Material pickRandomDecoration(Random random) {
        if (decorations.isEmpty()) {
            return null;
        }
        return decorations.get(random.nextInt(decorations.size()));
    }

    public static MapTheme pickRandom(Random random) {
        MapTheme[] values = values();
        return values[random.nextInt(values.length)];
    }

    public enum TerrainProfile {
        DEFAULT,
        REAL_MOUNTAIN
    }
}
