package com.yourfault.gameloop;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Central place for all gameplay coordinates so designers can tweak them without chasing literals.
 */
public class GameLoopConfig {
    private final Vector playMapCenter = new Vector(228, 50, -158);
    private final Vector startBeaconCenter = new Vector(228, 25, -158);
    private final double startScatterRadius = 8.0;
    private final Vector endBeaconCenter = new Vector(228, 25, -158);
    private final double gatherRadius = 15.0;
    private final double gatherVerticalTolerance = 4.0;
    private final Vector bossArenaCenter = new Vector(228, 0, -158);
    private final double bossObservationHeight = 50.0;
    private final double bossSpawnYOffset = 4.0;
    private final Vector perkHubLocation = new Vector(309, -57, 60);
    private final Vector lobbyLocation = new Vector(39, -60, 19);

    public Location resolvePlayMapCenter(World world) {
        return toLocation(world, playMapCenter);
    }

    public Location resolveStartBeacon(World world) {
        return toLocation(world, startBeaconCenter);
    }

    public double getStartScatterRadius() {
        return startScatterRadius;
    }

    public Location resolveEndBeaconCenter(World world) {
        return toLocation(world, endBeaconCenter);
    }

    public double getGatherRadius() {
        return gatherRadius;
    }

    public double getGatherVerticalTolerance() {
        return gatherVerticalTolerance;
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

    private Location toLocation(World world, Vector vector) {
        World resolvedWorld = world;
        if (resolvedWorld == null && vector != null) {
            throw new IllegalArgumentException("World reference is required to resolve coordinates.");
        }
        return new Location(resolvedWorld, vector.getX(), vector.getY(), vector.getZ());
    }
}
