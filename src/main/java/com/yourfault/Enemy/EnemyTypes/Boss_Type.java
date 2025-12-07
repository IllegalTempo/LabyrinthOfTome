package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.EnemyClassification;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

public class Boss_Type extends AbstractEnemyType {
    public Boss_Type() {
        super("Eclipse Warden", 10, 80.0f, 18.0f, 3, 10, 0.35, 5, 10, 50, 100, EnemyClassification.BOSS);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return location.getWorld().spawn(location, org.bukkit.entity.IronGolem.class);
    }


}
