package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.projectiles.BoneProjectile;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BoneSlingerEnemy extends Enemy {
    private int attackCooldown = 0;

    public BoneSlingerEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 5L, type);
    }

    @Override
    public void update() {
        if (entity.getTarget() == null) {
            if (getNearestPlayer() != null) {
                entity.setTarget(getNearestPlayer().MINECRAFT_PLAYER);
            }
        }

        if (attackCooldown > 0) {
            attackCooldown = Math.max(0, attackCooldown - 1);
        }

        if (entity.getTarget() instanceof Player) {
            Player target = (Player) entity.getTarget();
            if (target != null && entity.hasLineOfSight(target)) {
                double distance = entity.getLocation().distance(target.getLocation());
                if (distance < 15 && attackCooldown <= 0) {
                    shootBone(target);
                    attackCooldown = 15; // Rapid fire
                }
            }
        }
    }

    private void shootBone(Player target) {
        Location spawnLoc = entity.getEyeLocation();
        Vector direction = target.getEyeLocation().toVector().subtract(spawnLoc.toVector()).normalize();
        spawnLoc.setDirection(direction);
        new BoneProjectile(spawnLoc, this);
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}
}
