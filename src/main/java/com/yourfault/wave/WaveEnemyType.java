package com.yourfault.wave;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public enum WaveEnemyType {
    GRUNT(EntityType.ZOMBIE, 1, 6.0, 4.0, 1, 1, 1.0),
    ARCHER(EntityType.SKELETON, 2, 5.0, 5.0, 1, 2, 0.9),
    BRUTE(EntityType.HUSK, 3, 12.0, 7.0, 2, 4, 0.75),
    MAGE(EntityType.STRAY, 4, 9.0, 6.0, 2, 6, 0.6),
    BOSS(EntityType.WITHER_SKELETON, 10, 80.0, 18.0, 3, 10, 0.35);

    private final EntityType entityType;
    private final double weight;
    private final double baseHealth;
    private final double baseDamage;
    private final int tier;
    private final int minWave;
    private final double spawnBias;

    WaveEnemyType(EntityType entityType,
                  double weight,
                  double baseHealth,
                  double baseDamage,
                  int tier,
                  int minWave,
                  double spawnBias) {
        this.entityType = entityType;
        this.weight = weight;
        this.baseHealth = baseHealth;
        this.baseDamage = baseDamage;
        this.tier = tier;
        this.minWave = minWave;
        this.spawnBias = spawnBias;
    }

    public double weight() {
        return weight;
    }

    public double baseHealth() {
        return baseHealth;
    }

    public double baseDamage() {
        return baseDamage;
    }

    public int tier() {
        return tier;
    }

    public int minWave() {
        return minWave;
    }

    public double spawnBias() {
        return spawnBias;
    }

    public boolean isBoss() {
        return this == BOSS;
    }

    public LivingEntity spawn(World world, Location location) {
        return (LivingEntity) world.spawnEntity(location, entityType);
    }
}
