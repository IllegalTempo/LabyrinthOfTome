package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.ShieldSkeleton_Type;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class ShieldSkeletonEnemy extends Enemy {

    private int bashCooldown = 30;

    public ShieldSkeletonEnemy(Mob entity, WaveContext context, ShieldSkeleton_Type type) {
        super(entity, context, 5L, type);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) {
            return;
        }
        if (bashCooldown > 0) {
            bashCooldown -= 5;
        }
        GamePlayer target = getNearestPlayer();
        if (target != null && target.getMinecraftPlayer() != null) {
            Player player = target.getMinecraftPlayer();
            entity.setTarget(player);
            if (entity.getLocation().distanceSquared(player.getLocation()) < 9 && bashCooldown <= 0) {
                bash(player);
                bashCooldown = 60;
            }
        }
    }

    private void bash(Player player) {
        Vector knock = player.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.8);
        player.setVelocity(knock.setY(0.35));
        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.6f);
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}

    @Override
    public void Destroy() {
        super.Destroy();
    }
}