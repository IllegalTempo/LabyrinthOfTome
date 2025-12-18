package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.PhaseGuardianEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Shulker;

public class PhaseGuardian_Type extends AbstractEnemyType {

    public PhaseGuardian_Type() {
        super("Phase Guardian", 24f, 850f, 14f, 3, 15, 0.55, 15, 25 ,50, 120, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Shulker shulker = (Shulker) location.getWorld().spawnEntity(location, EntityType.SHULKER);
        shulker.setColor(org.bukkit.DyeColor.PURPLE);
        shulker.setCustomName(displayName);
        shulker.setPersistent(true);
        return shulker;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new PhaseGuardianEnemy(e, context, this);
    }
}
