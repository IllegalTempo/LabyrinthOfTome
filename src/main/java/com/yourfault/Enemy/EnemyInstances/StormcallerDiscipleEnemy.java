package com.yourfault.Enemy.EnemyInstances;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.wave.WaveContext;

public class StormcallerDiscipleEnemy extends Enemy {

    private Player targetPlayer;
    private int channelTicks = 0;
    private static final int MAX_CHANNEL_TICKS = 100;
    private static final double MAX_RANGE = 15.0;

    public StormcallerDiscipleEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 2L, type); // Fast update for beam visuals

        // Spawn 2 Dust Devils
        if (Main.game.getWaveManager() != null) {
            AbstractEnemyType dustDevilType = Main.game.getWaveManager().EnemyTypes.get("dustdevil");
            if (dustDevilType != null) {
                for (int i = 0; i < 2; i++) {
                    Location spawnLoc = entity.getLocation().add((Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4);
                    Main.game.getWaveManager().spawnEnemyAt(spawnLoc, dustDevilType);
                }
            }
        }
    }

    @Override
    public void update() {
        if (entity.isDead()) return;

        // Find target
        if (targetPlayer == null || !targetPlayer.isValid() || targetPlayer.isDead() || targetPlayer.getLocation().distance(entity.getLocation()) > MAX_RANGE) {
            targetPlayer = null;
            channelTicks = 0;
            if (getNearestPlayer() != null) {
                Player p = getNearestPlayer().MINECRAFT_PLAYER;
                if (p.getLocation().distance(entity.getLocation()) <= MAX_RANGE) {
                    targetPlayer = p;
                }
            }
        }

        if (targetPlayer != null) {
            if (entity.hasLineOfSight(targetPlayer)) {
                channelTicks++;
                drawBeam();
                if (channelTicks % 10 == 0) {
                    double damage = 2.0 + (channelTicks / 20.0);
                    targetPlayer.damage(damage, entity);
                    targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);
                }
            } else {
                channelTicks = 0;
            }
        }
    }

    private void drawBeam() {
        Location start = entity.getEyeLocation();
        Location end = targetPlayer.getEyeLocation().subtract(0, 0.5, 0);
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = start.distance(end);
        direction.normalize();

        for (double d = 0; d < distance; d += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(d));
            entity.getWorld().spawnParticle(Particle.DUST, point, 1, new Particle.DustOptions(Color.AQUA, 1));
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
