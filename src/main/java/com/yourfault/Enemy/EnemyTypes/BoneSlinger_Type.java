package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Skeleton;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.BoneSlingerEnemy;
import com.yourfault.wave.WaveContext;

public class BoneSlinger_Type extends AbstractEnemyType {
    public BoneSlinger_Type() {
        super("Bone Slinger", 2.0f, 10.0f, 2.0f, 1, 4, 1.0, 3, 5, 5, 10, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return location.getWorld().spawn(location, Skeleton.class);
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new BoneSlingerEnemy(e, context, this);
    }
}
