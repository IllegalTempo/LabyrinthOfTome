package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.projectiles.StoneclawProjectile;
import com.yourfault.Enemy.EnemyTypes.StoneclawGolem_Type;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class StoneclawGolemEnemy extends Enemy {

    private int abilityCooldown = 0;
    private boolean isLeaping = false;
    private int leapTicks = 0;

    public StoneclawGolemEnemy(Mob entity, WaveContext context, StoneclawGolem_Type type) {
        super(entity, context, 5L, type); // Update every 5 ticks
        // Make it faster
        entity.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) return;

        if (isLeaping) {
            checkSlamImpact();
            return;
        }

        if (abilityCooldown > 0) {
            abilityCooldown -= 5;
            return;
        }

        GamePlayer target = getNearestPlayer();
        if (target != null && target.getMinecraftPlayer() != null) {
            Player p = target.getMinecraftPlayer();
            double dist = getDistance(p);

            if (dist < 8) {
                // Close range: Sundering Slam
                performSunderingSlam(p);
                abilityCooldown = 100; // 5 seconds
            } else if (dist < 25) {
                // Medium range: Rock Barrage
                performRockBarrage();
                abilityCooldown = 60; // 3 seconds
            }
        }
    }

    private void performRockBarrage() {
        int projectiles = ThreadLocalRandom.current().nextInt(3, 6); // 3 to 5

        // Find potential targets
        List<Player> targets = new ArrayList<>();
        for (Entity e : entity.getNearbyEntities(20, 20, 20)) {
            if (e instanceof Player p) {
                targets.add(p);
            }
        }

        if (targets.isEmpty()) return;

        for (int i = 0; i < projectiles; i++) {
            final int delay = i * 4;
            Bukkit.getScheduler().runTaskLater(Main.plugin, () -> {
                if (entity == null || !entity.isValid()) return;

                // Pick a random target for each shot
                Player target = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));

                Location spawnLoc = entity.getEyeLocation();
                Location targetLoc = target.getEyeLocation();

                Vector dir = targetLoc.toVector().subtract(spawnLoc.toVector()).normalize();
                // Slight spread
                dir.add(new Vector((Math.random() - 0.5) * 0.1, (Math.random() - 0.5) * 0.1, (Math.random() - 0.5) * 0.1));
                spawnLoc.setDirection(dir);

                new StoneclawProjectile(spawnLoc, this);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHOOT, 1f, 0.5f);
            }, delay);
        }
    }

    private void performSunderingSlam(Player target) {
        isLeaping = true;
        leapTicks = 0;
        Vector jump = target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(1.5).setY(1.0);
        entity.setVelocity(jump);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.5f);
    }

    private void checkSlamImpact() {
        leapTicks += 5;
        // Wait at least a bit before checking ground to avoid instant landing if jump hasn't started properly
        if (leapTicks > 10 && entity.isOnGround()) {
            // Landed
            isLeaping = false;
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
            entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), 1);

            // Damage and slow
            for (Entity e : entity.getNearbyEntities(5, 3, 5)) {
                if (e instanceof Player p) {
                    p.damage(10.0, entity); // Significant damage

                    // Find GamePlayer
                    for (GamePlayer gp : Main.game.PLAYER_LIST.values()) {
                        if (gp.getMinecraftPlayer().getUniqueId().equals(p.getUniqueId())) {
                            gp.applySpeedBoost(-50f, 100); // -50 speed for 5 seconds
                            break;
                        }
                    }
                }
            }
        }
        // Failsafe
        if (leapTicks > 100) {
            isLeaping = false;
        }
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}
}
