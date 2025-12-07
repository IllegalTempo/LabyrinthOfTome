package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyProjectiles.Musket;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.wave.WaveContext;
import org.bukkit.entity.Mob;

public class SpinnyEnemy extends Enemy {
    public SpinnyEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 40, type);
    }

    @Override
    public void update() {
        if(entity.getTarget() == null)
        {
            entity.setTarget(getNearestPlayer().MINECRAFT_PLAYER);

        }
        new Musket(entity.getEyeLocation(), damageMultiplier * enemyType.baseDamage,this);




    }

    @Override
    public void tick() {
        entity.lookAt(getNearestPlayer().MINECRAFT_PLAYER);

    }

    @Override
    public void OnAttack() {

    }

    @Override
    public void OnDealDamage() {

    }
}
