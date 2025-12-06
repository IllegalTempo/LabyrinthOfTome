package com.yourfault.Enemy.system;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyInstances.GeneralEnemyInstance;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public abstract class AbstractEnemyType {
    public final String displayName;
    public final float weight;
    public final float baseHealth;
    public final float baseDamage;
    public final int tier;
    public final int minWave;
    public final double spawnBias;
    public final int hitCoins;
    public final int hitXp;
    public final int killCoins;
    public final int killXp;
    public final boolean isBoss;
    public final EnemyClassification classification;
    //When creating a EnemyType you need to
    //-Define CreateEnemyInstance, new <the enemy instance type>
    //-Define SpawnEntity, spawn the minecraft entity here

    public AbstractEnemyType(String displayName, float weight, float baseHealth, float baseDamage, int tier, int minWave, double spawnBias, int hitCoins, int hitXp, int killCoins, int killXp, EnemyClassification classification, boolean isBoss){
        this.displayName = displayName;
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
        this.isBoss = isBoss;
    }
    public AbstractEnemyType(String displayName, float weight, float baseHealth, float baseDamage, int tier, int minWave, double spawnBias, int hitCoins, int hitXp, int killCoins, int killXp, EnemyClassification classification){
        this(displayName, weight, baseHealth, baseDamage, tier, minWave, spawnBias, hitCoins, hitXp, killCoins, killXp,classification,false);
    }

    public abstract LivingEntity SpawnEntity(Location location);
    public Enemy CreateEnemyInstance(LivingEntity e, WaveContext context)
    {
        return new GeneralEnemyInstance(e,context,this);
    }

    //I change it so that different enemy type can control how its scaling with wave and difficulty
    public float getScaledHealth(WaveContext context)
    {
        float health1 = baseHealth * (1 + (context.waveNumber() * 0.15f));
        float healthMultiplier = 1 + (weight * 0.05f);
        return health1 * healthMultiplier * context.difficulty().difficultyScale();
    }
    public float getScaledDefense(WaveContext context)
    {
        float baseDefense = (context.waveNumber() * 0.3f) + (weight * 0.2f);
        return baseDefense * context.difficulty().difficultyScale();
    }
    public float getDamageMultipler(WaveContext context)
    {
        return 1 + (context.waveNumber() * 0.1f);
    }


}
