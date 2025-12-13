package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyProjectiles.IceArrowProjectile;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class FrostShardArcherEnemy extends Enemy {
    private int attackCooldown = 0;

    public FrostShardArcherEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 10L, type);
    }

    @Override
    public void update() {
        if (entity.isDead()) return;
        if (entity.getTarget() == null) {
            if (getNearestPlayer() != null) {
                entity.setTarget(getNearestPlayer().MINECRAFT_PLAYER);
            }
        }
        if (attackCooldown > 0) {
            attackCooldown = Math.max(0, attackCooldown-1);
        }
        if (entity.getTarget() instanceof Player) {
            Player target = (Player) entity.getTarget();
            if (target != null && entity.hasLineOfSight(target)) {
                double distance = entity.getLocation().distance(target.getLocation());
                if (distance < 20 && attackCooldown <= 0) {
                    shootIceArrow(target);
                    attackCooldown = 6;
                }
            }
        }
    }

    private void shootIceArrow(Player target) {
        Location spawnLoc = entity.getEyeLocation();
        Vector direction = target.getEyeLocation().toVector().subtract(spawnLoc.toVector()).normalize();
        spawnLoc.setDirection(direction);
        new IceArrowProjectile(spawnLoc, this);
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}
}
