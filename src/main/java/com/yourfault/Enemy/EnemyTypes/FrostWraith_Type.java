package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Stray;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.FrostWraithEnemy;
import com.yourfault.wave.WaveContext;

public class FrostWraith_Type extends AbstractEnemyType {
    public FrostWraith_Type() {
        super("Frost Wraith", 4.0f, 30.0f, 4.0f, 2, 4, 0.7, 3, 6, 15, 25, EnemyClassification.NORMAL, false);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return location.getWorld().spawn(location, Stray.class);
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new FrostWraithEnemy(e, context, this);
    }
}
