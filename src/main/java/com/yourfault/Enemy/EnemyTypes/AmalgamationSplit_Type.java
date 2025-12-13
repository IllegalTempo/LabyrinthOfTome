package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.AmalgamationSplitEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Slime;

public class AmalgamationSplit_Type extends AbstractEnemyType {

    public AmalgamationSplit_Type() {
        super("Amalgamation Split", 1, 150, 10, 3, 10, 0, 20, 50, 100, 200, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Slime slime = (Slime) location.getWorld().spawnEntity(location, EntityType.SLIME);
        slime.setSize(3);
        return slime;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new AmalgamationSplitEnemy(e, context, this);
    }
}
