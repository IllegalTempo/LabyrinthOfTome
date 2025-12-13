package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.CinderHoundEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Wolf;

public class CinderHound_Type extends AbstractEnemyType {

    public CinderHound_Type() {
        super("Cinder Hound", 10, 40, 6, 2, 5, 1.0, 5, 10, 20, 50, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Wolf wolf = (Wolf) location.getWorld().spawnEntity(location, EntityType.WOLF);
        wolf.setAngry(true);
        return wolf;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new CinderHoundEnemy(e, context, this);
    }
}
