package com.yourfault.map.build;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Random;

public final class RoadBuildContext {
    private final Location center;
    private final int radius;
    private final int halfWidth;
    private final Material material;
    private final Random random;
    private final RoadPainter painter;

    public RoadBuildContext(Location center,
                            int radius,
                            int halfWidth,
                            Material material,
                            Random random,
                            RoadPainter painter) {
        this.center = center;
        this.radius = radius;
        this.halfWidth = halfWidth;
        this.material = material;
        this.random = random;
        this.painter = painter;
    }

    public Location center() {
        return center;
    }

    public int radius() {
        return radius;
    }

    public int halfWidth() {
        return halfWidth;
    }

    public Material material() {
        return material;
    }

    public Random random() {
        return random;
    }

    public RoadPainter painter() {
        return painter;
    }

    public int centerX() {
        return center.getBlockX();
    }

    public int centerZ() {
        return center.getBlockZ();
    }

    public int radiusSquared() {
        return radius * radius;
    }
}
