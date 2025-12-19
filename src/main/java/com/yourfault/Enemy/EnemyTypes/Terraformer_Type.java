package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.TerraformerEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Ravager;

public class Terraformer_Type extends AbstractEnemyType {
    public Terraformer_Type() {
        super("Terraformer", 4.8f, 300.0f, 18.0f, 3, 14, 0.35, 12, 20, 40, 70, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Ravager r = location.getWorld().spawn(location, Ravager.class);
        r.setPersistent(true);
        return r;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new TerraformerEnemy(e, context, this);
    }
}
