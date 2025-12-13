package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Spider;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.TarantulaEnemy;
import com.yourfault.wave.WaveContext;

public class Tarantula_Type extends AbstractEnemyType {
    public Tarantula_Type() {
        super("Tarantula", 2.0f, 12.0f, 4.0f, 1, 3, 1.0, 3, 5, 5, 10, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return location.getWorld().spawn(location, Spider.class);
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new TarantulaEnemy(e, context, this);
    }
}
