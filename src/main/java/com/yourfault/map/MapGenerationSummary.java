package com.yourfault.map;

public class MapGenerationSummary {
    private final MapTheme theme;
    private final int radius;
    private final int modifiedBlocks;
    private final int spawnMarkerCount;

    public MapGenerationSummary(MapTheme theme, int radius, int modifiedBlocks, int spawnMarkerCount) {
        this.theme = theme;
        this.radius = radius;
        this.modifiedBlocks = modifiedBlocks;
        this.spawnMarkerCount = spawnMarkerCount;
    }

    public MapTheme getTheme() {
        return theme;
    }

    public int getRadius() {
        return radius;
    }

    public int getModifiedBlocks() {
        return modifiedBlocks;
    }

    public int getSpawnMarkerCount() {
        return spawnMarkerCount;
    }
}
