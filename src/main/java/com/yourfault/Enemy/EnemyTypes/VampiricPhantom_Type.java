package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.VampiricPhantomEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

public class VampiricPhantom_Type extends AbstractEnemyType {
    public VampiricPhantom_Type() {
        super("Vampiric Phantom", 5, 20.0f, 0.0f, 2, 5, 0.5, 3, 8, 15, 25, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return (Mob) location.getWorld().spawnEntity(location, EntityType.WITCH);
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new VampiricPhantomEnemy(e, context, this);
    }
}
