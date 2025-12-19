package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.PlagueDoctor_Type;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlagueDoctorEnemy extends Enemy implements Listener {
    private static final double CLOUD_RADIUS = 6.0;
    private static final double ANTI_HEAL_RADIUS = 10.0;
    private static final int CLOUD_COOLDOWN_TICKS = 80;
    private static final int SUPPORT_COOLDOWN_TICKS = 120;
    private static final float ALLY_HEAL_AMOUNT = 6.0f;
    private static final double CLOUD_PARTICLE_STEP = 0.4;

    private final Map<UUID, Long> healDenyNotifications = new HashMap<>();

    private int diseaseCooldown = 20;
    private int supportCooldown = 40;

    public PlagueDoctorEnemy(Mob entity, WaveContext context, PlagueDoctor_Type type) {
        super(entity, context, 5L, type);
        entity.addScoreboardTag("plague_doctor");
        Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) {
            return;
        }
        if (entity.getTarget() == null) {
            GamePlayer nearest = getNearestPlayer();
            if (nearest != null && nearest.getMinecraftPlayer() != null) {
                entity.setTarget(nearest.getMinecraftPlayer());
            }
        }
        if (diseaseCooldown > 0) {
            diseaseCooldown -= 5;
        } else {
            spawnDiseaseCloud();
            diseaseCooldown = CLOUD_COOLDOWN_TICKS;
        }
        if (supportCooldown > 0) {
            supportCooldown -= 5;
        } else {
            mendNearbyAllies();
            supportCooldown = SUPPORT_COOLDOWN_TICKS;
        }
        outlineSuppressedPlayers();
    }

    private void spawnDiseaseCloud() {
        List<Player> victims = getPlayersWithin(CLOUD_RADIUS);
        if (victims.isEmpty()) {
            return;
        }
        Location center = entity.getLocation().add(0, 0.5, 0);
        entity.getWorld().playSound(center, Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 0.6f);
        entity.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 20, 0.6, 0.2, 0.6, 0.01);

        AreaEffectCloud cloud = (AreaEffectCloud) entity.getWorld().spawnEntity(center, EntityType.AREA_EFFECT_CLOUD);
        cloud.setDuration(80);
        cloud.setRadius((float) CLOUD_RADIUS);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-0.02f);
        cloud.setParticle(Particle.DUST);
        cloud.setColor(Color.fromRGB(58, 132, 68));
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 80, 0, true, true, true), true);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.HUNGER, 120, 1, true, true, true), true);

        for (Player player : victims) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 0, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 120, 1, true, true, true));
            player.sendMessage(ChatColor.DARK_GREEN + "You are engulfed by a disease cloud!");
        }
        drawCloudArcs(center);
    }

    private void drawCloudArcs(Location center) {
        for (double angle = 0; angle < Math.PI * 2; angle += CLOUD_PARTICLE_STEP) {
            double x = Math.cos(angle) * CLOUD_RADIUS;
            double z = Math.sin(angle) * CLOUD_RADIUS;
            Location point = center.clone().add(x, 0.1, z);
            entity.getWorld().spawnParticle(Particle.SNEEZE, point, 1, 0.05, 0.05, 0.05, 0.001);
        }
    }

    private void mendNearbyAllies() {
        if (Main.game == null) {
            return;
        }
        int healed = 0;
        for (Entity nearby : entity.getNearbyEntities(8, 4, 8)) {
            if (!(nearby instanceof Mob mob)) {
                continue;
            }
            if (mob.getUniqueId().equals(entity.getUniqueId())) {
                continue;
            }
            Enemy ally = Main.game.ENEMY_LIST.get(mob.getUniqueId());
            if (ally == null || ally.HEALTH >= ally.MAX_HEALTH) {
                continue;
            }
            ally.HEALTH = Math.min(ally.MAX_HEALTH, ally.HEALTH + ALLY_HEAL_AMOUNT);
            ally.updateDisplay();
            mob.getWorld().spawnParticle(Particle.HEART, mob.getLocation().add(0, 1.0, 0), 3, 0.2, 0.3, 0.2, 0.01);
            healed++;
            if (healed >= 3) {
                break;
            }
        }
        if (healed > 0) {
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.6f, 1.4f);
        }
    }

    private void outlineSuppressedPlayers() {
        for (Player player : getPlayersWithin(ANTI_HEAL_RADIUS)) {
            player.spawnParticle(Particle.SNEEZE, player.getLocation().add(0, 1, 0), 1, 0.1, 0.2, 0.1, 0.001);
        }
    }

    private List<Player> getPlayersWithin(double radius) {
        if (Main.game == null) {
            return Collections.emptyList();
        }
        double radiusSq = radius * radius;
        List<Player> players = new ArrayList<>();
        for (var gp : Main.game.PLAYER_LIST.values()) {
            Player player = gp.getMinecraftPlayer();
            if (player == null || !player.isOnline() || player.isDead()) {
                continue;
            }
            if (!player.getWorld().equals(entity.getWorld())) {
                continue;
            }
            if (player.getLocation().distanceSquared(entity.getLocation()) <= radiusSq) {
                players.add(player);
            }
        }
        return players;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionLaunch(ProjectileLaunchEvent event) {
        if (entity == null || !entity.isValid()) {
            return;
        }
        if (!(event.getEntity() instanceof ThrownPotion potion)) {
            return;
        }
        if (potion.getShooter() instanceof Mob shooter && shooter.getUniqueId().equals(entity.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (entity == null || !entity.isValid()) {
            return;
        }
        if (!player.getWorld().equals(entity.getWorld())) {
            return;
        }
        double distanceSq = player.getLocation().distanceSquared(entity.getLocation());
        if (distanceSq > ANTI_HEAL_RADIUS * ANTI_HEAL_RADIUS) {
            return;
        }
        RegainReason reason = event.getRegainReason();
        if (reason == RegainReason.CUSTOM) {
            return;
        }
        event.setCancelled(true);
        warnHealingDenied(player);
    }

    private void warnHealingDenied(Player player) {
        long now = System.currentTimeMillis();
        Long last = healDenyNotifications.get(player.getUniqueId());
        if (last == null || now - last > 1500) {
            player.sendMessage(ChatColor.DARK_GREEN + "Plague fumes deny your healing!");
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.6f, 0.4f);
            healDenyNotifications.put(player.getUniqueId(), now);
        }
        player.spawnParticle(Particle.SNEEZE, player.getLocation().add(0, 1, 0), 6, 0.2, 0.3, 0.2, 0.01);
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

    @Override
    public void Destroy() {
        HandlerList.unregisterAll(this);
        healDenyNotifications.clear();
        super.Destroy();
    }
}
