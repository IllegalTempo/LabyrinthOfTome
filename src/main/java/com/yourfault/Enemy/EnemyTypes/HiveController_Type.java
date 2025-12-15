package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Shulker;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.HiveControllerEnemy;
import com.yourfault.wave.WaveContext;

public class HiveController_Type extends AbstractEnemyType {

    public HiveController_Type() {
        super("Hive Controller", 15, 60, 0, 3, 10, 1.0, 15, 30, 40, 80, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Shulker shulker = (Shulker) location.getWorld().spawnEntity(location, EntityType.SHULKER);
        shulker.setAI(true);
        return shulker;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new HiveControllerEnemy(e, context, this);
    }
}
