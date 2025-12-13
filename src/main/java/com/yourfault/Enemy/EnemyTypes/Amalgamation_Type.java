package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.AmalgamationEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Slime;

public class Amalgamation_Type extends AbstractEnemyType {

    public Amalgamation_Type() {
        super("The Amalgamation", 1, 400, 15, 4, 15, 0, 100, 200, 500, 1000, EnemyClassification.BOSS, true);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Slime slime = (Slime) location.getWorld().spawnEntity(location, EntityType.SLIME);
        slime.setSize(8);
        return slime;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new AmalgamationEnemy(e, context, this);
    }
}
