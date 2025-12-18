package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.projectiles.ChainWardenHookProjectile;
import com.yourfault.wave.WaveContext;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ChainWardenEnemy extends Enemy implements Listener {

    private final List<UUID> chainedPlayers = new ArrayList<>();
    private float chainHealth = 100.0f;
    private int chainDurationTicks = 0;
    private int hookCooldown = 0;
    private static final int MAX_CHAIN_DURATION = 600;
    private static final double CHAIN_RANGE = 20.0;

    public ChainWardenEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 5L, type);
        Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
    }

    @Override
    public void update() {
        if (entity.isDead()) {
            breakChain();
            return;
        }
        if (!chainedPlayers.isEmpty()) {
            chainDurationTicks += 5;
            if (chainDurationTicks >= MAX_CHAIN_DURATION) {
                breakChain();
            } else {
                if (chainedPlayers.size() >= 2) {
                    Player p1 = Main.plugin.getServer().getPlayer(chainedPlayers.get(0));
                    Player p2 = Main.plugin.getServer().getPlayer(chainedPlayers.get(1));
                    if (p1 != null && p2 != null && p1.isOnline() && p2.isOnline()) {
                        drawChain(p1.getLocation().add(0, 1, 0), p2.getLocation().add(0, 1, 0));
                    } else {
                        breakChain();
                    }
                }
            }
        }
        if (chainedPlayers.size() < 2) {
            if (hookCooldown > 0) {
                hookCooldown -= 5;
            } else {
                Player target = findTarget();
                if (target != null) {
                    shootHook(target);
                    hookCooldown = 60;
                }
            }
        }
    }

    private Player findTarget() {
        List<Entity> nearby = entity.getNearbyEntities(CHAIN_RANGE, CHAIN_RANGE, CHAIN_RANGE);
        List<Player> candidates = new ArrayList<>();
        for (Entity e : nearby) {
            if (e instanceof Player) {
                Player p = (Player) e;
                if (!chainedPlayers.contains(p.getUniqueId())) {
                    candidates.add(p);
                }
            }
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private void shootHook(Player target) {
        Location origin = entity.getEyeLocation();
        Location targetLoc = target.getEyeLocation();
        Vector dir = targetLoc.toVector().subtract(origin.toVector()).normalize();
        origin.add(dir.clone().multiply(0.5));
        ChainWardenHookProjectile hook = new ChainWardenHookProjectile(origin, this);
        hook.setDirection(dir);
        entity.getWorld().playSound(origin, Sound.ENTITY_FISHING_BOBBER_THROW, 1.0f, 0.5f);
    }

    public void onHookHit(Player p) {
        if (chainedPlayers.contains(p.getUniqueId())) return;
        chainedPlayers.add(p.getUniqueId());
        p.sendMessage("§eYou have been tagged by the Chain Warden!");
        p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 2.0f);
        if (chainedPlayers.size() == 2) {
            Player p1 = Main.plugin.getServer().getPlayer(chainedPlayers.get(0));
            Player p2 = Main.plugin.getServer().getPlayer(chainedPlayers.get(1));
            if (p1 != null && p2 != null) {
                p1.sendMessage("§bYou have been chained to " + p2.getName() + "!");
                p2.sendMessage("§bYou have been chained to " + p1.getName() + "!");
                p1.playSound(p1.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 2.0f);
                p2.playSound(p2.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 2.0f);
            }
        }
    }

    private void drawChain(Location l1, Location l2) {
        Vector direction = l2.toVector().subtract(l1.toVector());
        double distance = l1.distance(l2);
        direction.normalize();
        for (double d = 0; d < distance; d += 0.5) {
            Location point = l1.clone().add(direction.clone().multiply(d));
            entity.getWorld().spawnParticle(Particle.DUST, point, 1, new Particle.DustOptions(Color.BLUE, 1));
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (chainedPlayers.isEmpty()) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.CUSTOM) {
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onDamageShared(EntityDamageEvent event) {
        if (chainedPlayers.isEmpty()) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        if (!chainedPlayers.contains(victim.getUniqueId())) return;
        if (victim.hasMetadata("ChainDamageProcessing")) return;
        double damage = event.getFinalDamage();
        if (damage <= 0) return;
        chainHealth -= damage;
        if (chainHealth <= 0) {
            breakChain();
            return;
        }
        for (UUID id : chainedPlayers) {
            if (!id.equals(victim.getUniqueId())) {
                Player partner = Main.plugin.getServer().getPlayer(id);
                if (partner != null && partner.isOnline()) {
                    partner.setMetadata("ChainDamageProcessing", new org.bukkit.metadata.FixedMetadataValue(Main.plugin, true));
                    partner.damage(damage);
                    partner.removeMetadata("ChainDamageProcessing", Main.plugin);
                    partner.sendMessage("§bShared damage from chain!");
                }
            }
        }
    }

    private void breakChain() {
        if (chainedPlayers.isEmpty()) return;
        for (UUID id : chainedPlayers) {
            Player p = Main.plugin.getServer().getPlayer(id);
            if (p != null) {
                p.sendMessage("§aThe spectral chain has broken!");
                p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
            }
        }
        chainedPlayers.clear();
        chainHealth = 100.0f;
        chainDurationTicks = 0;
    }

    @Override
    public void Destroy() {
        breakChain();
        HandlerList.unregisterAll(this);
        super.Destroy();
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
