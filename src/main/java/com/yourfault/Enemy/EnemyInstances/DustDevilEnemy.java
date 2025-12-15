package com.yourfault.Enemy.EnemyInstances;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.wave.WaveContext;

public class DustDevilEnemy extends Enemy {

    public DustDevilEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 5L, type);
    }

    @Override
    public void update() {
        if (entity.isDead()) return;
        if (entity.getTarget() == null) {
            if (getNearestPlayer() != null) {
                entity.setTarget(getNearestPlayer().MINECRAFT_PLAYER);
            }
        }

        Location center = entity.getLocation().add(0, 0.5, 0);
        double radius = 6.0;
        entity.getWorld().spawnParticle(Particle.CLOUD, center, 5, 1, 1, 1, 0.1);

        List<Entity> nearbyEntities = entity.getNearbyEntities(radius, radius, radius);
        for (Entity e : nearbyEntities) {
            if (e.getUniqueId().equals(entity.getUniqueId())) continue;
            if (e instanceof Player || e instanceof Item) {
                Vector direction = center.toVector().subtract(e.getLocation().toVector()).normalize();
                double strength = 0.15;
                e.setVelocity(e.getVelocity().add(direction.multiply(strength)));
                if (e instanceof LivingEntity && e.getLocation().distance(center) < 2.5) {
                    ((LivingEntity) e).damage(1.0, entity);
                }
            }
        }
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {
    }

    @Override
    public void OnDealDamage() {
    }
}
