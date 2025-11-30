package com.yourfault.Enemy.mob;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveEnemyInstance;
import com.yourfault.wave.WaveEnemyType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Custom wave enemy that locks onto a player with a laser beam before dealing burst damage.
 */
public class LaserZombieEnemy extends WaveEnemyInstance {
    private static final double LOCK_RANGE = 22.0;
    private static final int TICK_INTERVAL = 5;
    private static final int LOCK_DURATION_TICKS = 60; // 3 seconds
    private static final double LASER_STEP = 0.4;

    private BukkitRunnable behaviorTask;
    private UUID lockedTarget;
    private int ticksUntilStrike = -1;

    public LaserZombieEnemy(LivingEntity entity,
                            float health,
                            float maxHealth,
                            float defense,
                            WaveEnemyType type,
                            double scaledDamage) {
        super(entity, health, maxHealth, defense, type, scaledDamage);
        startBehaviorLoop();
    }

    private void startBehaviorLoop() {
        behaviorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || entity.isDead() || !entity.isValid()) {
                    cancelBehavior();
                    return;
                }
                if (lockedTarget != null) {
                    Player target = Main.plugin.getServer().getPlayer(lockedTarget);
                    if (!validateLock(target)) {
                        clearLock();
                        return;
                    }
                    drawLaser(target);
                    ticksUntilStrike -= TICK_INTERVAL;
                    if (ticksUntilStrike <= 0) {
                        fireLaser(target);
                        clearLock();
                    }
                    return;
                }
                Player candidate = findTarget();
                if (candidate != null) {
                    lockedTarget = candidate.getUniqueId();
                    ticksUntilStrike = LOCK_DURATION_TICKS;
                    candidate.sendMessage(ChatColor.DARK_RED + "A laser zombie has locked onto you! Find cover.");
                }
            }
        };
        behaviorTask.runTaskTimer(Main.plugin, 0L, TICK_INTERVAL);
    }

    private boolean validateLock(Player target) {
        if (target == null || !target.isOnline() || target.isDead()) {
            return false;
        }
        if (!entity.getWorld().equals(target.getWorld())) {
            return false;
        }
        if (entity.getLocation().distanceSquared(target.getLocation()) > LOCK_RANGE * LOCK_RANGE) {
            return false;
        }
        return entity.hasLineOfSight(target);
    }

    private Player findTarget() {
        if (Main.game == null) {
            return null;
        }
        double bestDistance = Double.MAX_VALUE;
        Player best = null;
        for (GamePlayer gamePlayer : Main.game.PLAYER_LIST.values()) {
            Player bukkit = gamePlayer.getMinecraftPlayer();
            if (bukkit == null || !bukkit.isOnline() || bukkit.isDead()) {
                continue;
            }
            if (!entity.getWorld().equals(bukkit.getWorld())) {
                continue;
            }
            double distanceSquared = entity.getLocation().distanceSquared(bukkit.getLocation());
            if (distanceSquared > LOCK_RANGE * LOCK_RANGE) {
                continue;
            }
            if (!entity.hasLineOfSight(bukkit)) {
                continue;
            }
            if (distanceSquared < bestDistance) {
                bestDistance = distanceSquared;
                best = bukkit;
            }
        }
        return best;
    }

    private void drawLaser(Player target) {
        Location start = entity.getEyeLocation();
        Location end = target.getEyeLocation();
        double distance = start.distance(end);
        int points = Math.max(2, (int) Math.round(distance / LASER_STEP));
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            double x = start.getX() + (end.getX() - start.getX()) * t;
            double y = start.getY() + (end.getY() - start.getY()) * t;
            double z = start.getZ() + (end.getZ() - start.getZ()) * t;
            start.getWorld().spawnParticle(Particle.CRIT, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void fireLaser(Player target) {
        if (target == null) {
            return;
        }
        if (!entity.hasLineOfSight(target)) {
            return;
        }
        GamePlayer gamePlayer = Main.game != null ? Main.game.GetPlayer(target) : null;
        double rawDamage = getScaledDamage();
        float finalDamage = (float) Math.max(0.0, rawDamage);
        if (gamePlayer != null) {
            gamePlayer.damage(finalDamage);
        } else {
            // Fallback to vanilla if GamePlayer not available
            target.damage(finalDamage, entity);
        }
        target.sendMessage(ChatColor.RED + "The laser burns through you for " + String.format("%.1f", finalDamage) + " damage!");
        target.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 15, 0.2, 0.2, 0.2, 0.01);
        target.getWorld().spawnParticle(Particle.CRIT, target.getEyeLocation(), 15, 0.2, 0.2, 0.2, 0.01);
    }

    private void clearLock() {
        lockedTarget = null;
        ticksUntilStrike = -1;
    }

    private void cancelBehavior() {
        if (behaviorTask != null) {
            behaviorTask.cancel();
            behaviorTask = null;
        }
    }

    @Override
    public void Destroy() {
        cancelBehavior();
        super.Destroy();
    }
}
