package com.yourfault.Enemy.EnemyInstances;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;

public class VampiricPhantomEnemy extends Enemy {

    private int attackCooldown = 0;
    private boolean strafeLeft = true;
    private int strafeTimer = 0;

    public VampiricPhantomEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 5L, type);
        // Remove items so it doesn't use bow or other items
        if (entity.getEquipment() != null) {
            entity.getEquipment().clear();
        }
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) return;

        if (attackCooldown > 0) {
            attackCooldown -= 5;
        }

        Player target = findNearestPlayer();
        if (target != null) {
            handleMovement(target);
            handleAbilities(target);
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}

    private Player findNearestPlayer() {
        if (Main.game == null) return null;
        Player best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (GamePlayer gp : Main.game.PLAYER_LIST.values()) {
            Player p = gp.getMinecraftPlayer();
            if (p != null && p.isOnline() && !p.isDead() && p.getWorld().equals(entity.getWorld())) {
                double d = p.getLocation().distanceSquared(entity.getLocation());
                if (d < bestDistSq) {
                    bestDistSq = d;
                    best = p;
                }
            }
        }
        return best;
    }

    private void handleMovement(Player target) {
        // Remove target to prevent vanilla AI attacks (potion throwing)
        entity.setTarget(null);

        Location targetLoc = target.getLocation();
        Location entityLoc = entity.getLocation();
        double distSq = targetLoc.distanceSquared(entityLoc);
        double dist = Math.sqrt(distSq);

        Vector dir = targetLoc.toVector().subtract(entityLoc.toVector()).normalize();

        // Manually face the player
        Location lookLoc = entityLoc.clone();
        lookLoc.setDirection(dir);
        entity.setRotation(lookLoc.getYaw(), lookLoc.getPitch());

        // Preserve gravity
        double currentY = entity.getVelocity().getY();

        if (dist < 1.0) {
            // Too close, move away
            entity.setVelocity(dir.clone().multiply(-0.5).setY(currentY));
        } else if (dist < 3.0) {
            // Near, strafe
            // Rotate direction 90 degrees
            Vector strafeDir = new Vector(-dir.getZ(), 0, dir.getX());
            if (!strafeLeft) {
                strafeDir.multiply(-1);
            }

            entity.setVelocity(strafeDir.multiply(0.3).setY(currentY));

            strafeTimer += 5;
            if (strafeTimer > 40) { // Switch every 1 second
                strafeLeft = !strafeLeft;
                strafeTimer = 0;
            }
        } else {
            // Far, move towards
            entity.setVelocity(dir.multiply(0.4).setY(currentY));
        }
    }

    private void handleAbilities(Player target) {
        Location targetLoc = target.getLocation();
        Location entityLoc = entity.getLocation();
        double distSq = targetLoc.distanceSquared(entityLoc);

        if (distSq <= 15.0) { // 5 blocks squared
            // Blood Boil range
            if (attackCooldown <= 0) {
                // Attack
                Vector oldVelocity = target.getVelocity();
                target.damage(3.0, entity); // 3 health damage
                target.setVelocity(oldVelocity); // no knockback woohoo

                // Life Drain (Heal mob)
                this.HEALTH = Math.min(this.HEALTH + 1.0f, this.MaxHealth); // make sure no 100/20 <3 appears
                updateDisplay();

                // Visuals
                spawnHeartParticles(entityLoc.clone().add(0, 1, 0), targetLoc.clone().add(0, 1, 0));

                attackCooldown = 20; // 1 second
            }
        }
    }    private void spawnHeartParticles(Location start, Location end) {
        // Draw a line of hearts
        double distance = start.distance(end);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        for (double d = 0; d < distance; d += 0.5) {
            Location particleLoc = start.clone().add(direction.clone().multiply(d));
            start.getWorld().spawnParticle(Particle.HEART, particleLoc, 1, 0, 0, 0, 0);
        }
    }
}
