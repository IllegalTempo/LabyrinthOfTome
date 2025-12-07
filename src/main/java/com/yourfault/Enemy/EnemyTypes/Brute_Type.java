package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.EnemyClassification;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

public class Brute_Type extends AbstractEnemyType {
    public Brute_Type() {
        super("Brute", 3, 12.0f, 7.0f, 2, 4, 0.75, 3, 5, 10, 18, EnemyClassification.NORMAL);
    }


    @Override
    public Mob SpawnEntity(Location location) {
        return location.getWorld().spawn(location, org.bukkit.entity.Husk.class);
    }

}
