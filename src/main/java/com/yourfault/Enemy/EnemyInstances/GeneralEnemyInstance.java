package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.system.AbstractEnemyType;
import com.yourfault.wave.WaveContext;
import org.bukkit.entity.LivingEntity;

//This class is use for vanilla mobs
public class GeneralEnemyInstance extends Enemy {
    public GeneralEnemyInstance(LivingEntity entity, WaveContext context, AbstractEnemyType enemyType) {
        super(entity, context, enemyType);
    }

    @Override
    public void update() {

    }

    @Override
    public void tick() {

    }

    @Override
    public void OnAttack() {

    }

    @Override
    public void OnDealDamage() {

    }
}
