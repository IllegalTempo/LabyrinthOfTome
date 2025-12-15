package com.yourfault.Enemy.EnemyInstances;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.wave.WaveContext;

public class CopperheadSnakeEnemy extends Enemy {

    public CopperheadSnakeEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 20L, type);
    }

    @Override
    public void update() {
        if (entity.isDead()) return;

        if (entity.getTarget() == null) {
            if (getNearestPlayer() != null) {
                entity.setTarget(getNearestPlayer().MINECRAFT_PLAYER);
            }
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
        if (entity.getTarget() != null && entity.getTarget() instanceof Player) {
            Player target = (Player) entity.getTarget();
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 2));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0));
        }
    }
}
