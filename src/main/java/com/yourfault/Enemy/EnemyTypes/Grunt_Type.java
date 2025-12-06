package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.system.AbstractEnemyType;
import com.yourfault.Enemy.system.EnemyClassification;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class Grunt_Type extends AbstractEnemyType {
    public Grunt_Type() {
        super("Grunt", 1, 6.0f, 4.0f, 1, 1, 1.0, 1, 3, 5, 10, EnemyClassification.NORMAL);
    }

    @Override
    public LivingEntity SpawnEntity(Location location) {
        return location.getWorld().spawn(location, org.bukkit.entity.Zombie.class);
    }

}
