package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.system.AbstractEnemyType;
import com.yourfault.Enemy.system.EnemyClassification;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;

public class Archer_Type extends AbstractEnemyType {
    public Archer_Type() {
        super("Archer", 2, 5.0f, 5.0f, 1, 2, 0.9, 2, 4, 6, 12, EnemyClassification.NORMAL);
    }


    @Override
    public LivingEntity SpawnEntity(Location location) {
        return location.getWorld().spawn(location, Skeleton.class);
    }



}
