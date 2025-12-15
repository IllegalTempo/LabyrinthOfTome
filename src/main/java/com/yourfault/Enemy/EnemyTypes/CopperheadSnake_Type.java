package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.CopperheadSnakeEnemy;
import com.yourfault.wave.WaveContext;

public class CopperheadSnake_Type extends AbstractEnemyType {

    public CopperheadSnake_Type() {
        super("Copperhead Snake", 6, 12, 6, 2, 4, 1.0, 2, 4, 6, 12, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return (Mob) location.getWorld().spawnEntity(location, EntityType.SILVERFISH);
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new CopperheadSnakeEnemy(e, context, this);
    }
}
