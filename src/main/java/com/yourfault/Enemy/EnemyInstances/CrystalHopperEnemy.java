package com.yourfault.Enemy.EnemyInstances;

import java.util.Random;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.wave.WaveContext;

public class CrystalHopperEnemy extends Enemy {

    private final Random random = new Random();

    public CrystalHopperEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
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

        // Double jump logic
        if (entity.isOnGround() && entity.getTarget() != null) {
            double distance = entity.getLocation().distance(entity.getTarget().getLocation());
            if (distance < 8 && random.nextFloat() < 0.3) {
                Vector direction = entity.getTarget().getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
                direction.multiply(1.2).setY(0.8);
                entity.setVelocity(direction);
            }
        }
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {
        if (entity.getTarget() != null && entity.getTarget() instanceof Player) {
            Player target = (Player) entity.getTarget();
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1));
        }
    }
}
