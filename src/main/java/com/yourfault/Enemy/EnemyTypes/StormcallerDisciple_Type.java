package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.StormcallerDiscipleEnemy;
import com.yourfault.wave.WaveContext;

public class StormcallerDisciple_Type extends AbstractEnemyType {

    public StormcallerDisciple_Type() {
        super("Stormcaller Disciple", 12, 40, 5, 3, 8, 1.0, 10, 20, 30, 60, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return (Mob) location.getWorld().spawnEntity(location, EntityType.STRAY);
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new StormcallerDiscipleEnemy(e, context, this);
    }
}
