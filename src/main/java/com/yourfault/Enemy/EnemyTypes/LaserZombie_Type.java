package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.EnemyInstances.LaserZombieEnemy;
import com.yourfault.Enemy.system.AbstractEnemyType;
import com.yourfault.Enemy.system.EnemyClassification;
import com.yourfault.Enemy.Enemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class LaserZombie_Type extends AbstractEnemyType {
    public LaserZombie_Type() {
        super("L.A.S.R. Zombie", 4.5f, 24.0f, 8.0f, 3, 6, 0.65, 3, 6, 14, 24,EnemyClassification.ELITE);
    }

    @Override
    public LivingEntity SpawnEntity(Location location) {
        return location.getWorld().spawn(location, org.bukkit.entity.Zombie.class);
    }

    @Override
    public Enemy CreateEnemyInstance(LivingEntity e, WaveContext context) {
        return new LaserZombieEnemy(e,context,this);
    }

}
