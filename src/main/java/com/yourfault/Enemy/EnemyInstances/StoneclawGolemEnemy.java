package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyProjectiles.StoneclawProjectile;
import com.yourfault.Enemy.EnemyTypes.StoneclawGolem_Type;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class StoneclawGolemEnemy extends Enemy {

    private int abilityCooldown = 0;
    private int slamState = 0; // 0: Idle, 1: Warning, 2: Leaping
    private int slamTimer = 0;
    private int slamAirTicks = 0;
    private Location slamTarget;
    private double slamVerticalPower = 0.0;
    private final Random random = new Random();

    public StoneclawGolemEnemy(Mob entity, WaveContext context, StoneclawGolem_Type type) {
        super(entity, context, 5L, type); // Update every 5 ticks
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) return;

        if (slamState != 0) {
            handleSlam();
            return;
        }

        GamePlayer target = getNearestPlayer();
        if (target != null && target.getMinecraftPlayer() != null) {
            entity.setTarget(target.getMinecraftPlayer());
        }

        if (abilityCooldown > 0) {
            abilityCooldown -= 5;
        } else {
            if (target != null && target.getMinecraftPlayer() != null) {
                Player p = target.getMinecraftPlayer();
                double dist = getDistance(p);

                if (dist < 10) {
                    // Close range: Sundering Slam
                    startSlam(p.getLocation());
                } else {
                    // Medium range: Rock Barrage
                    performRockBarrage(p);
                    abilityCooldown = 60; // 3 seconds
                }
            }
        }
    }

    private void performRockBarrage(Player target) {
        Location spawnLoc = entity.getEyeLocation();
        Vector direction = target.getEyeLocation().toVector().subtract(spawnLoc.toVector()).normalize();

        // Fan pattern: 3 projectiles
        for (int i = -1; i <= 1; i++) {
            Location projLoc = spawnLoc.clone();
            Vector projDir = direction.clone();
            // Rotate vector around Y axis slightly
            double angle = Math.toRadians(i * 15);
            double x = projDir.getX() * Math.cos(angle) - projDir.getZ() * Math.sin(angle);
            double z = projDir.getX() * Math.sin(angle) + projDir.getZ() * Math.cos(angle);
            projDir.setX(x).setZ(z);

            projLoc.setDirection(projDir);
            new StoneclawProjectile(projLoc, this);
        }

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.5f);
    }

    private void startSlam(Location targetHint) {
        Location targetCenter = targetHint;
        if (targetCenter == null) {
            return;
        }

        slamState = 1; // Warning
        slamTimer = 40; // 2 seconds
        slamTarget = targetCenter;
        slamAirTicks = 0;
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 0.5f);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 255, false, false));
        entity.setAI(false);
    }

    private void handleSlam() {
        if (slamState == 1) { // Warning Phase
            // Draw Red Ring
            drawRing(slamTarget, 5, Color.RED);

            slamTimer -= 5;
            if (slamTimer <= 0) {
                beginLeap();
            }
        }
    }

    private boolean shouldFinishSlam() {
        if (slamState != 2) {
            return false;
        }
        if (slamAirTicks > 14 && entity.isOnGround()) {
            return true;
        }
        return slamAirTicks > 55; // hard fail-safe so the golem never freezes mid-air
    }

    private void beginLeap() {
        slamState = 2;
        slamAirTicks = 0;

        entity.removePotionEffect(PotionEffectType.SLOWNESS);
        entity.setAI(true);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 255, false, false));

        double heightDelta = Math.max(0, slamTarget.getY() - entity.getLocation().getY());
        slamVerticalPower = 1.35 + Math.min(0.4, heightDelta * 0.1);

        Vector horizontal = computeHorizontalVelocity();
        if (horizontal.lengthSquared() < 0.0001) {
            horizontal = entity.getLocation().getDirection().setY(0);
            if (horizontal.lengthSquared() < 0.0001) {
                horizontal = new Vector(1, 0, 0);
            }
            horizontal.normalize().multiply(1.2);
        }

        Vector initialVelocity = horizontal.clone().setY(slamVerticalPower);
        entity.teleport(entity.getLocation().add(0, 0.3, 0));
        entity.setVelocity(initialVelocity);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.5f);
    }

    private void finishSlam() {
        if (slamState != 2) {
            return;
        }
        performSlamEffect();
        slamState = 0;
        abilityCooldown = 100;
        slamVerticalPower = 0.0;
        entity.removePotionEffect(PotionEffectType.RESISTANCE);
        entity.setAI(true);
    }

    private void performSlamEffect() {
        entity.getWorld().createExplosion(entity.getLocation(), 0F, false);
        entity.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, entity.getLocation(), 1);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);

        for (Entity e : entity.getNearbyEntities(5, 3, 5)) {
            if (e instanceof Player) {
                Player p = (Player) e;
                GamePlayer gp = Main.game.GetPlayer(p);
                if (gp != null) {
                    gp.damage(10.0f); // Significant damage
                    gp.applySpeedBoost(-50f, 60); // -50 speed for 3 seconds
                    p.sendMessage(org.bukkit.ChatColor.RED + "You have been sundered!");
                }
            }
        }
    }

    private void drawRing(Location center, double radius, Color color) {
        for (int i = 0; i < 360; i += 10) {
            double angle = Math.toRadians(i);
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location loc = center.clone().add(x, 0.2, z);
            Particle.DustOptions dust = new Particle.DustOptions(color, 1);
            center.getWorld().spawnParticle(Particle.DUST, loc, 1, dust);
        }
    }

    private Location pickRandomPlayerCenter() {
        List<GamePlayer> candidates = new ArrayList<>();
        for (GamePlayer gp : Main.game.PLAYER_LIST.values()) {
            Player player = gp.getMinecraftPlayer();
            if (player == null || !player.isOnline() || player.isDead()) {
                continue;
            }
            if (!player.getWorld().equals(entity.getWorld())) {
                continue;
            }
            candidates.add(gp);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        GamePlayer chosen = candidates.get(random.nextInt(candidates.size()));
        return chosen.getMinecraftPlayer().getLocation().clone();
    }

    private Vector computeHorizontalVelocity() {
        if (slamTarget == null) {
            return new Vector(0, 0, 0);
        }
        Location current = entity.getLocation();
        Vector delta = slamTarget.clone().subtract(current).toVector();
        delta.setY(0);
        double distanceSquared = delta.lengthSquared();
        if (distanceSquared < 0.0001) {
            return new Vector(0, 0, 0);
        }
        double distance = Math.sqrt(distanceSquared);
        double speed = Math.min(2.2, Math.max(0.65, distance * 0.22));
        return delta.normalize().multiply(speed);
    }

    @Override
    public void tick() {
        if (slamState == 2) {
            slamAirTicks++;
            entity.setFallDistance(0);
            Vector horizontal = computeHorizontalVelocity();
            double verticalComponent = entity.getVelocity().getY();
            if (slamAirTicks <= 12) {
                verticalComponent = Math.max(0.25, slamVerticalPower - (0.18 * slamAirTicks));
            }
            Vector boost = horizontal;
            boost.setY(verticalComponent);
            entity.setVelocity(boost);
            if (shouldFinishSlam()) {
                finishSlam();
            }
        }
    }

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}
}
