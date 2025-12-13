package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.FrostShardArcherEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Stray;

public class FrostShardArcher_Type extends AbstractEnemyType {
    public FrostShardArcher_Type() {
        super("Frost Shard Archer", 8, 35, 5, 2, 8, 1.0, 7, 14, 24, 58, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Stray stray = (Stray) location.getWorld().spawnEntity(location, EntityType.STRAY);
        return stray;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new FrostShardArcherEnemy(e, context, this);
    }
}
