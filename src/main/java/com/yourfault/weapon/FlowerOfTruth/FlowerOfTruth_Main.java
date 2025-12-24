package com.yourfault.weapon.FlowerOfTruth;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.yourfault.Main;
import static com.yourfault.Main.plugin;
import com.yourfault.projectiles.HomingPetalProjectile;
import com.yourfault.projectiles.PetalProjectile;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.LabyrinthCreature;
import com.yourfault.utils.AnimationInfo;
import com.yourfault.weapon.WeaponAttachment;
import com.yourfault.weapon.WeaponType;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public class FlowerOfTruth_Main extends WeaponAttachment {

    private static final AnimationInfo ANIMATION_LC = new AnimationInfo("animation_lc", 10L);
    private static final AnimationInfo ANIMATION_RC_CHARGE = new AnimationInfo("animation_rc_charge", 100L); // Long animation for holding
    private static final AnimationInfo ANIMATION_RC_DASH = new AnimationInfo("animation_rc_dash", 10L);
    private static final AnimationInfo ANIMATION_FC = new AnimationInfo("animation_fc", 20L);

    private boolean isCharging = false;
    private long lastRightClickTime = 0;
    private BukkitRunnable chargeTask;
    private BukkitRunnable releaseCheckTask;

    public FlowerOfTruth_Main(GamePlayer player) {
        super(WeaponType.FlowerOfTruth, player);
    }

    @Override
    public void onSwitchorRemoveWeapon() {

    }

    @Override
    public void LC() {
        player.playAnimation(ANIMATION_LC);
        player.ChangeMana(-type.lc_mana);

        Location loc = player.getMinecraftPlayer().getLocation();
        loc.getWorld().playSound(Sound.sound(Key.key("minecraft:entity.snowball.throw"), Sound.Source.PLAYER, 1.0f, 1.5f), loc.getX(), loc.getY(), loc.getZ());

        int projectileCount = Math.max(1, (int)(1 * player.projectileMultiplier));
        float totalSpreadDegrees = 30f;
        float angleStep = projectileCount > 1 ? totalSpreadDegrees / (projectileCount - 1) : 0f;
        double midpoint = (projectileCount - 1) / 2.0;

        for (int i = 0; i < projectileCount; i++) {
            Location spawnLoc = player.getMinecraftPlayer().getEyeLocation();

            if (projectileCount > 1) {
                float angleOffset = (float) ((i - midpoint) * angleStep);
                spawnLoc.setYaw(spawnLoc.getYaw() + angleOffset);
            }

            spawnLoc.add(spawnLoc.getDirection().multiply(1));
            new PetalProjectile(spawnLoc, 8f, player);
        }
    }

    @Override
    public void RC() {
        long now = System.currentTimeMillis();

        if (!isCharging) {
            isCharging = true;
            lastRightClickTime = now;
            player.ChangeMana(-type.rc_mana);

            // Start Charging Visuals
            startChargingVisuals();

            // Start Release Check
            startReleaseCheck();

            player.playAnimation(ANIMATION_RC_CHARGE);
        } else {
            lastRightClickTime = now;
        }
    }

    private void startChargingVisuals() {
        chargeTask = new BukkitRunnable() {
            double angle = 0;
            @Override
            public void run() {
                if (!player.MINECRAFT_PLAYER.isOnline()) {
                    cancel();
                    return;
                }
                Location loc = player.MINECRAFT_PLAYER.getLocation().add(0, 1, 0);
                double radius = 1.5;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                loc.add(x, 0, z);
                player.MINECRAFT_PLAYER.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc, 1, 0, 0, 0, 0);
                angle += 0.5;
            }
        };
        chargeTask.runTaskTimer(plugin, 0L, 2L);
    }

    private void startReleaseCheck() {
        releaseCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastRightClickTime > 250) { // 250ms timeout
                    performDash();
                    cancel();
                }
            }
        };
        releaseCheckTask.runTaskTimer(plugin, 0L, 1L);
    }

    private void performDash() {
        isCharging = false;
        if (chargeTask != null) {
            chargeTask.cancel();
        }

        player.playAnimation(ANIMATION_RC_DASH);

        // Dash Logic
        Vector direction = player.MINECRAFT_PLAYER.getLocation().getDirection();
        direction.setY(0).normalize().multiply(2.5); // Dash strength
        direction.setY(0.2); // Slight hop
        player.MINECRAFT_PLAYER.setVelocity(direction);

        // Riptide effect
        player.MINECRAFT_PLAYER.setRiptiding(true);

        player.MINECRAFT_PLAYER.getWorld().playSound(Sound.sound(Key.key("minecraft:entity.horse.jump"), Sound.Source.PLAYER, 1.0f, 1.0f), player.MINECRAFT_PLAYER.getLocation().getX(), player.MINECRAFT_PLAYER.getLocation().getY(), player.MINECRAFT_PLAYER.getLocation().getZ());
        player.MINECRAFT_PLAYER.getWorld().playSound(Sound.sound(Key.key("minecraft:item.trident.riptide_3"), Sound.Source.PLAYER, 1.0f, 1.0f), player.MINECRAFT_PLAYER.getLocation().getX(), player.MINECRAFT_PLAYER.getLocation().getY(), player.MINECRAFT_PLAYER.getLocation().getZ());

        // Burst at end of dash (approx 10 ticks later)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.MINECRAFT_PLAYER.setRiptiding(false);
            performBurst();
        }, 10L);
    }

    private void performBurst() {
        Location loc = player.MINECRAFT_PLAYER.getLocation().add(0, 1, 0);

        player.MINECRAFT_PLAYER.getWorld().playSound(Sound.sound(Key.key("minecraft:entity.generic.explode"), Sound.Source.PLAYER, 0.5f, 2f), loc.getX(), loc.getY(), loc.getZ());

        int petalCount = (int) (2 * player.projectileMultiplier);
        for (int i = 0; i < petalCount; i++) {
            Location spawnLoc = loc.clone().add(Math.random() * 2 - 1, Math.random() * 2 - 1, Math.random() * 2 - 1);
            new HomingPetalProjectile(spawnLoc, 12f, player);
        }
    }

    @Override
    public void FC() {
        player.playAnimation(ANIMATION_FC);
        player.ChangeMana(-type.fc_mana);

        Location center = player.MINECRAFT_PLAYER.getLocation().clone();
        center.setPitch(0);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 160; // 8 seconds
            final double radius = 8.0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                // Visuals
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.sqrt(Math.random()) * radius;
                    double x = r * Math.cos(angle);
                    double z = r * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, 0.2, z);
                    particleLoc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, particleLoc, 1, 0, 0, 0, 0);
                }

                // Logic (every second)
                if (ticks % 20 == 0) {
                    Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, radius, 4, radius);
                    for (Entity e : nearby) {
                        if (e instanceof LivingEntity) {
                            // Check if teammate or enemy
                            LabyrinthCreature creature = Main.game.CREATURE_LIST.get(e.getUniqueId());
                            if (creature != null) {
                                if (creature.team == player.team) {
                                    // Heal Teammate and refill mana every seconds
                                    creature.HEALTH = Math.min(creature.MAX_HEALTH, creature.HEALTH + 10);
                                    creature.MANA = Math.min(creature.MAX_MANA, creature.MANA + 5);
                                    if (creature instanceof GamePlayer) {
                                        ((GamePlayer) creature).refillVanillaHealth();
                                    }
                                    creature.minecraftEntity.getWorld().spawnParticle(Particle.HEART, creature.minecraftEntity.getLocation().add(0, 2, 0), 1);
                                } else {
                                    // Slow Enemy
                                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                                }
                            }
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
