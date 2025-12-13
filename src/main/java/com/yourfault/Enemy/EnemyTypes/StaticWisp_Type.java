package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.StaticWispEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.Bee;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

public class StaticWisp_Type extends AbstractEnemyType {

    public StaticWisp_Type() {
        super("Static Wisp", 8, 30, 4, 2, 7, 1.0, 6, 12, 22, 55, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Bee bee = (Bee) location.getWorld().spawnEntity(location, EntityType.BEE);
        bee.setAnger(999999);
        return bee;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new StaticWispEnemy(e, context, this);
    }
}
