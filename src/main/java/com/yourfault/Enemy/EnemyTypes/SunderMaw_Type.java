package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.SunderMawEnemy;
import com.yourfault.wave.WaveContext;

public class SunderMaw_Type extends AbstractEnemyType {

    public SunderMaw_Type() {
        super("Sunder Maw", 12, 80, 10, 3, 9, 1.0, 12, 25, 35, 70, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return (Mob) location.getWorld().spawnEntity(location, EntityType.SILVERFISH);
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new SunderMawEnemy(e, context, this);
    }
}
