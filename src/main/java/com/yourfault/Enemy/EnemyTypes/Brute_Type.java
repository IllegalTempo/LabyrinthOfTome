package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.system.AbstractEnemyType;
import com.yourfault.Enemy.system.EnemyClassification;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class Brute_Type extends AbstractEnemyType {
    public Brute_Type() {
        super("Brute", 3, 12.0f, 7.0f, 2, 4, 0.75, 3, 5, 10, 18, EnemyClassification.NORMAL);
    }


    @Override
    public LivingEntity SpawnEntity(Location location) {
        return location.getWorld().spawn(location, org.bukkit.entity.Husk.class);
    }

}
