package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class CinderHoundEnemy extends Enemy {

    public CinderHoundEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 5L, type);
        // initial fire resistance so the hound doesn't burn itself
        if (entity != null) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0, true, false));
        }
    }

    @Override
    public void update() {
        if (entity.isDead()) return;

        //Refresh fire resistance idk
        entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0, true, false));

        if (entity.getTarget() == null) {
            if (getNearestPlayer() != null) {
                entity.setTarget(getNearestPlayer().MINECRAFT_PLAYER);
            }
        }

        Location loc = entity.getLocation();
        Block block = loc.getBlock();
        if (block.getType() == Material.AIR) {
            block.setType(Material.FIRE);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (block.getType() == Material.FIRE) {
                        block.setType(Material.AIR);
                    }
                }
            }.runTaskLater(Main.plugin, 60L);
        }
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {
    }

    @Override
    public void OnDealDamage() {
        if (entity.getTarget() != null) {
            entity.getTarget().setFireTicks(60);
        }
    }
}
