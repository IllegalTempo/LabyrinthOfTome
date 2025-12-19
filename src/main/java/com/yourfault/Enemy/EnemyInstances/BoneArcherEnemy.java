package com.yourfault.Enemy.EnemyInstances;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.BoneArcher_Type;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;

public class BoneArcherEnemy extends Enemy {

    private static final String ARROW_TAG = "lot_bone_archer_arrow";
    private final Random random = new Random();
    private int shotCooldown = 20;

    public BoneArcherEnemy(Mob entity, WaveContext context, BoneArcher_Type type) {
        super(entity, context, 5L, type);
        if (entity.getEquipment() != null) {
            ItemStack bow = type.createBow();
            entity.getEquipment().setItemInMainHand(bow);
            entity.getEquipment().setItemInOffHand(null);
        }
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) {
            return;
        }
        if (shotCooldown > 0) {
            shotCooldown -= 5;
        }
        GamePlayer target = getNearestPlayer();
        if (target != null && target.getMinecraftPlayer() != null) {
            entity.setTarget(target.getMinecraftPlayer());
            if (shotCooldown <= 0 && entity.hasLineOfSight(target.getMinecraftPlayer())) {
                firePiercingArrow(target.getMinecraftPlayer());
                shotCooldown = 40 + random.nextInt(20);
            }
        }
    }

    private void firePiercingArrow(Player target) {
        Vector dir = target.getEyeLocation().toVector().subtract(entity.getEyeLocation().toVector()).normalize();
        Arrow arrow = entity.launchProjectile(Arrow.class, dir.multiply(1.2));
        arrow.setVelocity(dir.multiply(1.2));
        arrow.setDamage(enemyType.baseDamage * damageMultiplier * 0.9);
        arrow.setPierceLevel(3);
        arrow.setCritical(true);
        arrow.setMetadata(ARROW_TAG, new FixedMetadataValue(Main.plugin, true));
        arrow.getWorld().spawnParticle(Particle.SMOKE, arrow.getLocation(), 5, 0.05, 0.05, 0.05, 0.01);
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}

    public static boolean isMarkedArrow(Arrow arrow) {
        return arrow.hasMetadata(ARROW_TAG);
    }
}
