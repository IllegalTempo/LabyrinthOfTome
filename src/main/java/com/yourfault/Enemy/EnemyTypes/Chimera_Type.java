package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.ChimeraEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Ravager;

public class Chimera_Type extends AbstractEnemyType {

    public Chimera_Type() {
        super("Chimera", 100, 400, 20, 5, 20, 1.0, 150, 600, 1500, 6000, EnemyClassification.BOSS);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return (Mob) location.getWorld().spawnEntity(location, EntityType.RAVAGER);
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new ChimeraEnemy(e, context, this);
    }
}
