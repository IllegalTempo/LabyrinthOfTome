package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.BoneMonarchEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.WitherSkeleton;

public class BoneMonarch_Type extends AbstractEnemyType {
    public BoneMonarch_Type() {
        super("Bone Monarch", 1.0f, 520.0f, 16.0f, 5, 25, 0.0, 200, 400, 1200, 2200, EnemyClassification.BOSS, true);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        WitherSkeleton skeleton = location.getWorld().spawn(location, WitherSkeleton.class);
        skeleton.setPersistent(true);
        skeleton.setCustomNameVisible(true);
        skeleton.setCustomName("§7Bone Monarch");
        skeleton.setInvulnerable(false);
        return skeleton;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new BoneMonarchEnemy(e, context, this);
    }
}
