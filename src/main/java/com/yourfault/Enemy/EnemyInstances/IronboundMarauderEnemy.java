package com.yourfault.Enemy.EnemyInstances;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;

public class IronboundMarauderEnemy extends Enemy {

    public IronboundMarauderEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
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

    @Override
    public void OnBeingDamage(float damage, GamePlayer damageDealer) {
        if (damageDealer != null) {
            Vector toAttacker = damageDealer.MINECRAFT_PLAYER.getLocation().toVector().subtract(entity.getLocation().toVector());
            Vector direction = entity.getLocation().getDirection();
            toAttacker.setY(0).normalize();
            direction.setY(0).normalize();
            double dot = direction.dot(toAttacker);
            if (dot > 0.5) {
                entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                return;
            }
        }
        super.OnBeingDamage(damage, damageDealer);
    }
}
