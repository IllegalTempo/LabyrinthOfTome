package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.EnemyClassification;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

public class Mage_Type extends AbstractEnemyType {
    public Mage_Type() {
        super("Mage", 4, 15.0f, 6.0f, 2, 6, 0.6, 2, 6, 12, 20, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        // Prefer Evoker for mage-like behavior; fallback to Witch if Evoker not available in runtime
        try {
            return location.getWorld().spawn(location, org.bukkit.entity.Evoker.class);
        } catch (Throwable ex) {
            return location.getWorld().spawn(location, org.bukkit.entity.Witch.class);
        }
    }

}
