package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.TimeDilatorEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

public class TimeDilator_Type extends AbstractEnemyType {

    public TimeDilator_Type() {
        super("Time Dilator", 18f, 260f, 12f, 3, 12, 0.75, 12, 20, 35, 90, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Enderman enderman = (Enderman) location.getWorld().spawnEntity(location, EntityType.ENDERMAN);
        enderman.setCarriedBlock(null);
        enderman.setPersistent(true);
        enderman.setCustomName(displayName);
        return enderman;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new TimeDilatorEnemy(e, context, this);
    }
}
