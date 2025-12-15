package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.DustDevilEnemy;
import com.yourfault.wave.WaveContext;

public class DustDevil_Type extends AbstractEnemyType {

    public DustDevil_Type() {
        super("Dust Devil", 8, 25, 3, 2, 5, 1.0, 4, 8, 12, 25, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return (Mob) location.getWorld().spawnEntity(location, EntityType.BREEZE);
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new DustDevilEnemy(e, context, this);
    }
}
