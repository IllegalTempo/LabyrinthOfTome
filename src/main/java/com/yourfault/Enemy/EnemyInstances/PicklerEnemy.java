package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.wave.WaveContext;
import org.bukkit.entity.Mob;

public class PicklerEnemy extends Enemy {

    public PicklerEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 20L, type);
    }

    @Override
    public void update() {
        if (entity.isDead()) return;
        if (entity.getTarget() == null && getNearestPlayer() != null) {
            entity.setTarget(getNearestPlayer().MINECRAFT_PLAYER);
        }
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
