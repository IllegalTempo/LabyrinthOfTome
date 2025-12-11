package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.StoneclawGolemEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Mob;

public class StoneclawGolem_Type extends AbstractEnemyType {
    public StoneclawGolem_Type() {
        super("Stoneclaw Golem", 10.0f, 250.0f, 15.0f, 5, 10, 0.3, 10, 20, 100, 200, EnemyClassification.BOSS, true);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return location.getWorld().spawn(location, IronGolem.class);
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new StoneclawGolemEnemy(e, context, this);
    }
}
