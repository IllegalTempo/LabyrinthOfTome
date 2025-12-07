package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.EnemyClassification;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

public class Grunt_Type extends AbstractEnemyType {
    public Grunt_Type() {
        super("Grunt", 1, 10.0f, 4.0f, 1, 1, 1.0, 1, 3, 5, 10, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return location.getWorld().spawn(location, org.bukkit.entity.Zombie.class);
    }

}
