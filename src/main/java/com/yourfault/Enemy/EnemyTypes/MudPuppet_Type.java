package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Mob;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.MudPuppetEnemy;
import com.yourfault.wave.WaveContext;

public class MudPuppet_Type extends AbstractEnemyType {
    public MudPuppet_Type() {
        super("Mud Puppet", 2.0f, 15.0f, 3.0f, 1, 3, 1.0, 3, 5, 5, 10, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return location.getWorld().spawn(location, Husk.class);
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new MudPuppetEnemy(e, context, this);
    }
}
