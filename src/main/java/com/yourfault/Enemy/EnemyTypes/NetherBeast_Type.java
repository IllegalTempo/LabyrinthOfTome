package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.NetherBeastEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PigZombie;

public class NetherBeast_Type extends AbstractEnemyType {
    public NetherBeast_Type() {
        super("Nether Beast", 5.0f, 40.0f, 6.0f, 3, 5, 0.6, 4, 8, 20, 35, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        return location.getWorld().spawn(location, PigZombie.class);
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new NetherBeastEnemy(e, context, this);
    }
}
