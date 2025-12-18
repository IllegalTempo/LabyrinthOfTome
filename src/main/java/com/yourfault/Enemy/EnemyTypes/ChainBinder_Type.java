package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Vindicator;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.ChainBinderEnemy;
import com.yourfault.wave.WaveContext;

public class ChainBinder_Type extends AbstractEnemyType {

    public ChainBinder_Type() {
        super("Chain Binder", 12f, 180f, 10f, 2, 8, 0.9, 8, 18, 25, 65, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Vindicator vindicator = (Vindicator) location.getWorld().spawnEntity(location, EntityType.VINDICATOR);
        vindicator.setCustomName(displayName);
        vindicator.setPersistent(true);
        return vindicator;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new ChainBinderEnemy(e, context, this);
    }
}
