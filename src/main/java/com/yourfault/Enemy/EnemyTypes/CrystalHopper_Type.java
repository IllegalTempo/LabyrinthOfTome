package com.yourfault.Enemy.EnemyTypes;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Rabbit;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.CrystalHopperEnemy;
import com.yourfault.wave.WaveContext;

public class CrystalHopper_Type extends AbstractEnemyType {

    public CrystalHopper_Type() {
        super("Crystal Hopper", 5, 15, 5, 1, 3, 1.0, 3, 5, 8, 15, EnemyClassification.NORMAL);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Rabbit rabbit = (Rabbit) location.getWorld().spawnEntity(location, EntityType.RABBIT);
        rabbit.setRabbitType(Rabbit.Type.SALT_AND_PEPPER);
        rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
        return rabbit;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new CrystalHopperEnemy(e, context, this);
    }
}
