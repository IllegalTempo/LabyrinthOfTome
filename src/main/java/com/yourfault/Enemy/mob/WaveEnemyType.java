package com.yourfault.Enemy.mob;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public enum WaveEnemyType {
    GRUNT("Grunt", EntityType.ZOMBIE, 1, 6.0, 4.0, 1, 1, 1.0, 1, 3, 5, 10, EnemyClassification.MOB),
    ARCHER("Archer", EntityType.SKELETON, 2, 5.0, 5.0, 1, 2, 0.9, 2, 4, 6, 12, EnemyClassification.MOB),
    BRUTE("Brute", EntityType.HUSK, 3, 12.0, 7.0, 2, 4, 0.75, 3, 5, 10, 18, EnemyClassification.MOB),
    MAGE("Mage", EntityType.STRAY, 4, 9.0, 6.0, 2, 6, 0.6, 2, 6, 12, 20, EnemyClassification.MOB),
    LASER_ZOMBIE("L.A.S.R. Zombie", EntityType.ZOMBIE, 4.5, 24.0, 8.0, 3, 6, 0.65, 3, 6, 14, 24, EnemyClassification.MOB),
    BOSS("Eclipse Warden", EntityType.WITHER_SKELETON, 10, 80.0, 18.0, 3, 10, 0.35, 5, 10, 50, 100, EnemyClassification.BOSS);

    public enum EnemyClassification {
        MOB,
        BOSS
    }

    private final EntityType entityType;
    private final String displayName;
    private final double weight;
    private final double baseHealth;
    private final double baseDamage;
    private final int tier;
    private final int minWave;
    private final double spawnBias;
    private final int hitCoins;
    private final int hitXp;
    private final int killCoins;
    private final int killXp;
    private final EnemyClassification classification;

    WaveEnemyType(String displayName,
                  EntityType entityType,
                  double weight,
                  double baseHealth,
                  double baseDamage,
                  int tier,
                  int minWave,
                  double spawnBias,
                  int hitCoins,
                  int hitXp,
                  int killCoins,
                  int killXp,
                  EnemyClassification classification) {
        this.displayName = displayName;
        this.entityType = entityType;
        this.weight = weight;
        this.baseHealth = baseHealth;
        this.baseDamage = baseDamage;
        this.tier = tier;
        this.minWave = minWave;
        this.spawnBias = spawnBias;
        this.hitCoins = hitCoins;
        this.hitXp = hitXp;
        this.killCoins = killCoins;
        this.killXp = killXp;
        this.classification = classification;
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

    public String displayName() {
        return displayName;
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

    public EnemyClassification classification() {
        return classification;
    }

    public boolean isBoss() {
        return classification == EnemyClassification.BOSS;
    }

    public LivingEntity spawn(World world, Location location) {
        return (LivingEntity) world.spawnEntity(location, entityType);
    }

    public int hitCoins() {
        return hitCoins;
    }

    public int hitXp() {
        return hitXp;
    }

    public int killCoins() {
        return killCoins;
    }

    public int killXp() {
        return killXp;
    }
}
