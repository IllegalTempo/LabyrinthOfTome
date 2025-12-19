package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.CrystalSentinelEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Shulker;

public class CrystalSentinel_Type extends AbstractEnemyType {

    public CrystalSentinel_Type() {
        super("Crystal Sentinel", 1.0f, 620.0f, 20.0f, 6, 32, 0.0, 260, 520, 1400, 2800, EnemyClassification.BOSS, true);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Shulker shulker = location.getWorld().spawn(location, Shulker.class);
        shulker.setAI(false);
        shulker.setCustomNameVisible(true);
        shulker.setCustomName("§bCrystal Sentinel");
        shulker.setInvisible(true);
        return shulker;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new CrystalSentinelEnemy(e, context, this);
    }
}
