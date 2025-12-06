package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.system.AbstractEnemyType;
import com.yourfault.Enemy.system.EnemyClassification;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class Mage_Type extends AbstractEnemyType {
    public Mage_Type() {
        super("Mage", 4, 9.0f, 6.0f, 2, 6, 0.6, 2, 6, 12, 20, EnemyClassification.NORMAL);
    }

    @Override
    public LivingEntity SpawnEntity(Location location) {
        // Prefer Evoker for mage-like behavior; fallback to Witch if Evoker not available in runtime
        try {
            return location.getWorld().spawn(location, org.bukkit.entity.Evoker.class);
        } catch (Throwable ex) {
            return location.getWorld().spawn(location, org.bukkit.entity.Witch.class);
        }
    }

}
