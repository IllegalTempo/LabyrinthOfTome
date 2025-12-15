package com.yourfault.Enemy.EnemyTypes;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyClassification;
import com.yourfault.Enemy.EnemyInstances.ArachnidFabricatorEnemy;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Spider;

public class ArachnidFabricator_Type extends AbstractEnemyType {

    public ArachnidFabricator_Type() {
        super("Arachnid Fabricator", 100, 300, 15, 5, 15, 1.0, 100, 500, 1000, 5000, EnemyClassification.BOSS);
    }

    @Override
    public Mob SpawnEntity(Location location) {
        Spider spider = (Spider) location.getWorld().spawnEntity(location, EntityType.SPIDER);
        return spider;
    }

    @Override
    public Enemy CreateEnemyInstance(Mob e, WaveContext context) {
        return new ArachnidFabricatorEnemy(e, context, this);
    }
}
