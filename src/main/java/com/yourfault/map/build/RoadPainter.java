package com.yourfault.map.build;

import org.bukkit.Material;

@FunctionalInterface
public interface RoadPainter {
    void paint(int x, int z, Material material);
}
