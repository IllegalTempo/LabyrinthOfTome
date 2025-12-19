package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.LabyrinthCreature;
import com.yourfault.wave.WaveContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class BloodKnightEnemy extends Enemy implements Listener {
    private static final double LIFE_STEAL_RATIO = 0.5;
    private static final float MAX_BLOOD_SHIELD = 140.0f;
    private static final float SHIELD_GAIN_RATIO = 0.6f;
    private static final double TAUNT_RANGE = 12.0;
    private static final int TAUNT_DURATION_TICKS = 100;
    private static final int TAUNT_COOLDOWN_TICKS = 200;
    private static final String TAUNT_METADATA = "lot_blood_knight_taunt";

    private final Map<UUID, Integer> tauntedPlayers = new HashMap<>();
    private final Map<UUID, Long> tauntWarnCooldowns = new HashMap<>();
    private final Random random = new Random();

    private float bloodShield = 0.0f;
    private int tauntCooldown = 60;

    public BloodKnightEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 5L, type);
        Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) {
            return;
        }
        if (entity.getTarget() == null || entity.getTarget().isDead()) {
            GamePlayer nearest = getNearestPlayer();
            if (nearest != null && nearest.getMinecraftPlayer() != null) {
                entity.setTarget(nearest.getMinecraftPlayer());
            }
        }
        tickTaunts();
        if (tauntCooldown > 0) {
            tauntCooldown -= 5;
        } else if (attemptTaunt()) {
            tauntCooldown = TAUNT_COOLDOWN_TICKS;
        }
        updateShieldBar();
    }

    private void tickTaunts() {
        Iterator<Map.Entry<UUID, Integer>> iterator = tauntedPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 5;
            if (remaining <= 0) {
                releaseTaunt(entry.getKey(), ChatColor.GRAY + "You shake off the Blood Knight's taunt.");
                iterator.remove();
                continue;
            }
            entry.setValue(remaining);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                drawTauntLine(player);
            }
        }
    }

    private boolean attemptTaunt() {
        List<Player> candidates = getNearbyPlayers(TAUNT_RANGE);
        if (candidates.isEmpty()) {
            return false;
        }
        Collections.shuffle(candidates, random);
        int applied = 0;
        for (Player player : candidates) {
            applyTaunt(player);
            applied++;
            if (applied >= 3) {
                break;
            }
        }
        if (applied > 0) {
            Location center = entity.getLocation().add(0, 1, 0);
            entity.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.1f);
            entity.getWorld().spawnParticle(Particle.CRIMSON_SPORE, center, 30, 1.0, 0.5, 1.0, 0.03);
        }
        return applied > 0;
    }

    private void applyTaunt(Player player) {
        tauntedPlayers.put(player.getUniqueId(), TAUNT_DURATION_TICKS);
        player.setMetadata(TAUNT_METADATA, new FixedMetadataValue(Main.plugin, entity.getUniqueId().toString()));
        player.sendMessage(ChatColor.DARK_RED + "The Blood Knight taunts you! Face him or be punished.");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.6f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, TAUNT_DURATION_TICKS, 0, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, TAUNT_DURATION_TICKS, 0, false, true, true));

        Vector pull = entity.getLocation().toVector().subtract(player.getLocation().toVector());
        if (pull.lengthSquared() > 0.1) {
            pull.normalize().multiply(0.45).setY(0.25);
            player.setVelocity(pull);
        }
    }

    private void drawTauntLine(Player player) {
        Location start = entity.getLocation().add(0, 1.2, 0);
        Location end = player.getLocation().add(0, 1.0, 0);
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = start.distance(end);
        if (distance < 0.5) {
            return;
        }
        direction.normalize();
        for (double d = 0; d < distance; d += 0.4) {
            Location point = start.clone().add(direction.clone().multiply(d));
            entity.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(150, 14, 36), 1.2f));
        }
    }

    private List<Player> getNearbyPlayers(double radius) {
        if (Main.game == null) {
            return Collections.emptyList();
        }
        double radiusSq = radius * radius;
        List<Player> players = new ArrayList<>();
        for (GamePlayer gp : Main.game.PLAYER_LIST.values()) {
            Player p = gp.getMinecraftPlayer();
            if (p == null || !p.isOnline() || p.isDead()) {
                continue;
            }
            if (!p.getWorld().equals(entity.getWorld())) {
                continue;
            }
            if (p.getLocation().distanceSquared(entity.getLocation()) <= radiusSq) {
                players.add(p);
            }
        }
        return players;
    }

    private void releaseTaunt(UUID playerId, String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.removeMetadata(TAUNT_METADATA, Main.plugin);
            if (message != null) {
                player.sendMessage(message);
            }
        }
        tauntWarnCooldowns.remove(playerId);
    }

    private boolean isPlayerTaunted(UUID uuid) {
        return tauntedPlayers.containsKey(uuid);
    }

    private void warnTauntedPlayer(Player player) {
        long now = System.currentTimeMillis();
        Long last = tauntWarnCooldowns.get(player.getUniqueId());
        if (last == null || now - last > 1200) {
            player.sendMessage(ChatColor.RED + "The Blood Knight's command compels you!");
            tauntWarnCooldowns.put(player.getUniqueId(), now);
        }
        GamePlayer gp = Main.game != null ? Main.game.GetPlayer(player) : null;
        if (gp != null) {
            gp.applyDamage(3.0f, this, true);
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 0.6f, 1.3f);
    }

    private void updateShieldBar() {
        if (entity != null && entity.isValid()) {
            entity.setAbsorptionAmount(Math.min(40.0, bloodShield));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBloodKnightDamage(EntityDamageByEntityEvent event) {
        if (entity == null || !entity.isValid()) {
            return;
        }
        if (!event.getDamager().getUniqueId().equals(entity.getUniqueId())) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        double heal = event.getFinalDamage() * LIFE_STEAL_RATIO;
        if (heal <= 0) {
            return;
        }
        HEALTH = Math.min(MAX_HEALTH, HEALTH + (float) heal);
        updateDisplay();
        player.getWorld().spawnParticle(Particle.CRIMSON_SPORE, entity.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.02);
        player.getWorld().playSound(entity.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 0.5f, 0.7f);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTauntedPlayerAttack(EntityDamageByEntityEvent event) {
        if (entity == null || !entity.isValid()) {
            return;
        }
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!isPlayerTaunted(player.getUniqueId())) {
            return;
        }
        Entity target = event.getEntity();
        if (target.getUniqueId().equals(entity.getUniqueId())) {
            tauntedPlayers.remove(player.getUniqueId());
            releaseTaunt(player.getUniqueId(), ChatColor.GREEN + "You answer the Blood Knight's challenge.");
            return;
        }
        event.setCancelled(true);
        warnTauntedPlayer(player);
    }

    @Override
    public void applyDamage(float damage, LabyrinthCreature damageDealer, boolean bypassChain) {
        if (entity == null || !entity.isValid()) {
            return;
        }
        if (damage <= 0) {
            return;
        }
        float absorbed = 0.0f;
        if (bloodShield > 0) {
            absorbed = Math.min(bloodShield, damage);
            bloodShield -= absorbed;
            damage -= absorbed;
            entity.getWorld().spawnParticle(
                    Particle.DUST,
                    entity.getLocation().add(0, 1, 0),
                    12,
                    0.25,
                    0.35,
                    0.25,
                    0.0,
                    new Particle.DustOptions(Color.fromRGB(180, 36, 54), 1.0f)
            );
        }
        if (damage > 0) {
            super.applyDamage(damage, damageDealer, bypassChain);
            float gained = damage * SHIELD_GAIN_RATIO;
            bloodShield = Math.min(MAX_BLOOD_SHIELD, bloodShield + gained);
        }
        updateShieldBar();
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
        for (UUID id : new ArrayList<>(tauntedPlayers.keySet())) {
            releaseTaunt(id, null);
        }
        tauntedPlayers.clear();
        tauntWarnCooldowns.clear();
        super.Destroy();
    }
}
