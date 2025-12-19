package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.ArcaneNullifierEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Mob;

public class ArcaneNullifier_Type extends AbstractEnemyType {
    public ArcaneNullifier_Type() {
        super("Arcane Nullifier", 4.2f, 160.0f, 12.0f, 3, 12, 0.45, 8, 15, 24, 42, EnemyClassification.ELITE);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Evoker ev = location.getWorld().spawn(location, Evoker.class);
        ev.setPersistent(true);
        return ev;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new ArcaneNullifierEnemy(e, context, this);
    }
}
