package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.listener.TimeBubbleManager;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.Random;

public class TimeDilatorEnemy extends Enemy {
    private static final double BUBBLE_RADIUS = 4.5;
    private static final long BUBBLE_DURATION = 20L * 12;
    private static final int MAX_ACTIVE_BUBBLES = 3;
    private static final double BLINK_RADIUS = 5.0;
    private int bubbleCooldown = 40;
    private int blinkCooldown = 140;
    private final Random random = new Random();

    public TimeDilatorEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 5L, type);
        if (entity instanceof Enderman enderman) {
            enderman.setCarriedBlock(null);
            enderman.setSilent(true);
            enderman.setPersistent(true);
        }
    }

    @Override
    public void update() {
        if (entity.isDead()) {
            return;
        }
        acquireTarget();
        tickBubbleSpawner();
        tickBlink();
        entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation().add(0, 1.2, 0), 4, 0.4, 0.4, 0.4, 0.02);
    }

    private void acquireTarget() {
        GamePlayer nearest = getNearestPlayer();
        if (nearest != null && nearest.getMinecraftPlayer() != null) {
            entity.setTarget(nearest.getMinecraftPlayer());
        }
    }

    private void tickBubbleSpawner() {
        if (bubbleCooldown > 0) {
            bubbleCooldown = Math.max(0, bubbleCooldown - 5);
            return;
        }
        if (TimeBubbleManager.getOwnedBubbleCount(entity.getUniqueId()) >= MAX_ACTIVE_BUBBLES) {
            bubbleCooldown = 20;
            return;
        }
        Location center = selectBubbleLocation();
        if (center != null) {
            TimeBubbleManager.registerBubble(entity.getUniqueId(), center, BUBBLE_RADIUS, BUBBLE_DURATION);
            center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.3f);
        }
        bubbleCooldown = 80;
    }

    private Location selectBubbleLocation() {
        GamePlayer target = getNearestPlayer();
        Location base;
        if (target != null && target.getMinecraftPlayer() != null) {
            base = target.getMinecraftPlayer().getLocation().clone();
        } else {
            base = entity.getLocation().clone();
        }
        base.add(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
        int highestY = base.getWorld().getHighestBlockYAt(base.getBlockX(), base.getBlockZ());
        base.setY(highestY + 1);
        return base;
    }

    private void tickBlink() {
        if (blinkCooldown > 0) {
            blinkCooldown = Math.max(0, blinkCooldown - 5);
            return;
        }
        Player target = entity.getTarget() instanceof Player player ? player : null;
        if (target == null) {
            return;
        }
        Location destination = target.getLocation().clone().add(randomOffset(BLINK_RADIUS), 0, randomOffset(BLINK_RADIUS));
        int highestY = destination.getWorld().getHighestBlockYAt(destination.getBlockX(), destination.getBlockZ());
        destination.setY(highestY + 1);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.4f);
        entity.teleport(destination);
        entity.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        blinkCooldown = 140;
    }

    private double randomOffset(double range) {
        return (random.nextDouble() * range * 2) - range;
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

    public void Destroy(GamePlayer killer) {
        TimeBubbleManager.removeBubblesOwnedBy(entity.getUniqueId());
        super.Destroy(killer);
    }
}
