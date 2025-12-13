package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Mob;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.FrostbiteBatEnemy;
import com.yourfault.wave.WaveContext;

public class FrostbiteBat_Type extends AbstractEnemyType {
    public FrostbiteBat_Type() {
        super("Frostbite Bat", 2.0f, 6.0f, 3.0f, 1, 5, 1.0, 2, 4, 4, 8, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return location.getWorld().spawn(location, Bat.class);
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new FrostbiteBatEnemy(e, context, this);
    }
}
