package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Mob;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.StoneclawGolemEnemy;
import com.yourfault.wave.WaveContext;

public class StoneclawGolem_Type extends AbstractEnemyType {
    public StoneclawGolem_Type() {

        super("Stoneclaw Golem", 1.0f, 250.0f, 15.0f, 3, 10, 0.5, 10, 20, 500, 1000, EnemyClassification.BOSS, true);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        IronGolem golem = location.getWorld().spawn(location, IronGolem.class);
        golem.setPlayerCreated(false);
        return golem;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new StoneclawGolemEnemy(e, context, this);
    }
}
