package com.yourfault.gameloop;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Central place for all gameplay coordinates so designers can tweak them without chasing literals.
 */
public class GameLoopConfig {
    private final Vector playMapCenter = new Vector(228, 50, -158);
    private final double startScatterRadius = 8.0;
    private final Vector bossArenaCenter = new Vector(228, 0, -158);
    private final double bossObservationHeight = 50.0;
    private final double bossSpawnYOffset = 4.0;
    private final Vector perkHubLocation = new Vector(309, -57, 60);
    private final Vector lobbyLocation = new Vector(39, -60, 19);
    private Vector readyCornerA = new Vector(-5.5, -59, 21.5);
    private Vector readyCornerB = new Vector(-10.5, -57, 16.5);

    public Location resolvePlayMapCenter(World world) {
        return toLocation(world, playMapCenter);
    }

    public double getStartScatterRadius() {
        return startScatterRadius;
    }

    public Location resolveBossArenaCenter(World world) {
        return toLocation(world, bossArenaCenter);
    }

    public Location resolveBossObservationPoint(World world) {
        Location center = resolveBossArenaCenter(world);
        return center.clone().add(0, bossObservationHeight, 0);
    }

    public Location resolveBossSpawn(World world) {
        Location center = resolveBossArenaCenter(world);
        return center.clone().add(0, bossSpawnYOffset, 0);
    }

    public Location resolvePerkHub(World world) {
        return toLocation(world, perkHubLocation);
    }

    public Location resolveLobby(World world) {
        return toLocation(world, lobbyLocation);
    }

    public boolean isLocationInReadyArea(Location loc) {
        if (loc == null) return false;
        if (loc.getWorld() == null) return false;
        // world-agnostic check should be performed by caller; assume same world
        double minX = Math.min(readyCornerA.getX(), readyCornerB.getX());
        double maxX = Math.max(readyCornerA.getX(), readyCornerB.getX());
        double minY = Math.min(readyCornerA.getY(), readyCornerB.getY());
        double maxY = Math.max(readyCornerA.getY(), readyCornerB.getY());
        double minZ = Math.min(readyCornerA.getZ(), readyCornerB.getZ());
        double maxZ = Math.max(readyCornerA.getZ(), readyCornerB.getZ());
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public void setReadyArea(Vector cornerA, Vector cornerB) {
        if (cornerA != null) readyCornerA = cornerA;
        if (cornerB != null) readyCornerB = cornerB;
    }

    private Location toLocation(World world, Vector vector) {
        World resolvedWorld = world;
        if (resolvedWorld == null && vector != null) {
            throw new IllegalArgumentException("World reference is required to resolve coordinates.");
        }
        return new Location(resolvedWorld, vector.getX(), vector.getY(), vector.getZ());
    }
}
