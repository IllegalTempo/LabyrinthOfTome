package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Mob;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.CreeperlingSwarmerEnemy;
import com.yourfault.wave.WaveContext;

public class CreeperlingSwarmer_Type extends AbstractEnemyType {
    public CreeperlingSwarmer_Type() {
        super("Creeperling Swarmer", 8.0f, 15.0f, 4.0f, 1, 2, 0.8, 2, 4, 10, 20, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Creeper creeper = location.getWorld().spawn(location, Creeper.class);

        creeper.setMaxFuseTicks(20);
        creeper.setExplosionRadius(0);
        return creeper;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new CreeperlingSwarmerEnemy(e, context, this);
    }
}
