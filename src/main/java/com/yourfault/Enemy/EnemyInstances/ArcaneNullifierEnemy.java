package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.ArcaneNullifier_Type;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class ArcaneNullifierEnemy extends Enemy implements Listener {
    private static final double DRAIN_RANGE = 15.0;
    private static final int REFLECT_DURATION_TICKS = 100; //5s
    private static final int REFLECT_COOLDOWN_TICKS = 400; //20s
    private static final double DISPEL_RADIUS = 10.0;

    private int reflectCooldown = 0;
    private int reflectTicks = 0;
    private int drainCooldown = 0;
    private final Map<UUID, Integer> tempDamageBuffExpiry = new HashMap<>();
    private int lastDispelCount = 0;

    public ArcaneNullifierEnemy(Mob entity, WaveContext context, ArcaneNullifier_Type type) {
        super(entity, context, 5L, type);
        Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) return;

        if (reflectTicks > 0) {
            reflectTicks -= 5;
            if (reflectTicks <= 0) {
                entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 0.9f);
            }
        }
        if (reflectCooldown > 0) reflectCooldown -= 5;
        if (drainCooldown > 0) drainCooldown -= 5;

        // Mana Drain every ~3s
        if (drainCooldown <= 0) {
            performManaDrain();
            drainCooldown = 60;
        }

        // Occasionally trigger dispel + implosion combo
        if ((new Random()).nextInt(350) == 0) {
            performDispelField();
        }

        // Tick damage buff expiry
        Iterator<Map.Entry<UUID, Integer>> it = tempDamageBuffExpiry.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            int v = e.getValue() - 5;
            if (v <= 0) {
                it.remove();
            } else {
                e.setValue(v);
            }
        }
    }

    private void performManaDrain() {
        int drained = 0;
        for (GamePlayer gp : Main.game.PLAYER_LIST.values()) {
            Player p = gp.getMinecraftPlayer();
            if (p == null || p.isDead() || !p.isOnline()) continue;
            if (!p.getWorld().equals(entity.getWorld())) continue;
            if (p.getLocation().distance(entity.getLocation()) > DRAIN_RANGE) continue;
            Collection<PotionEffect> effects = p.getActivePotionEffects();
            if (effects.isEmpty()) continue;
            int count = effects.size();
            for (PotionEffect eff : effects) {
                p.removePotionEffect(eff.getType());
            }
            // Convert into temporary damage buff
            float buff = 0.12f * count; // each effect -> +12% damage
            int durationTicks = 100; //5s
            UUID id = entity.getUniqueId();
            tempDamageBuffExpiry.put(id, durationTicks);
            this.damageMultiplier += buff;
            drained += count;
            p.sendMessage("§5Your magical effects are siphoned away!");
        }
        if (drained > 0) {
            entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0,1,0), 30, 0.4,0.6,0.4,0.02);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 0.8f, 0.7f);
        }
    }

    private void performDispelField() {
        lastDispelCount = 0;
        List<Player> affected = new ArrayList<>();
        for (GamePlayer gp : Main.game.PLAYER_LIST.values()) {
            Player p = gp.getMinecraftPlayer();
            if (p == null || p.isDead() || !p.isOnline()) continue;
            if (!p.getWorld().equals(entity.getWorld())) continue;
            if (p.getLocation().distance(entity.getLocation()) > DISPEL_RADIUS) continue;
            Collection<PotionEffect> effects = p.getActivePotionEffects();
            lastDispelCount += effects.size();
            for (PotionEffect eff : effects) {
                p.removePotionEffect(eff.getType());
            }
            // Apply a short anti-enchant/anti-buff marker: weakness and mining fatigue
            p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 1, true, true, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 1, true, true, true));
            affected.add(p);
        }
        if (!affected.isEmpty()) {
            entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation().add(0,1,0), 8, 0.5,0.5,0.5,0.02);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.8f);
            // After short delay, implode
            Bukkit.getScheduler().runTaskLater(Main.plugin, () -> performArcaneImplosion(affected), 20L);
        }
    }

    private void performArcaneImplosion(List<Player> players) {
        // Pull players in
        Location center = entity.getLocation().add(0,1,0);
        for (Player p : players) {
            Vector dir = center.toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.2).setY(0.8);
            p.setVelocity(dir);
        }
        // Delay then damage
        int baseDamage = 8;
        int extra = lastDispelCount; // damage increases per buff dispelled
        Bukkit.getScheduler().runTaskLater(Main.plugin, () -> {
            for (Player p : players) {
                if (p == null || p.isDead()) continue;
                float dmg = (float) (baseDamage + extra * 1.5);
                GamePlayer gp = Main.game.GetPlayer(p);
                if (gp != null) gp.applyDamage(dmg, this, false);
                p.getWorld().spawnParticle(Particle.CRIT, p.getLocation().add(0,1,0), 12, 0.2,0.3,0.2,0.01);
            }
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.8f);
        }, 12L);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (entity == null || !entity.isValid()) return;
        if (reflectTicks <= 0) return;
        if (event.getEntity().getShooter() instanceof Player shooter) {
            // reflect spells back to shooter
            if (shooter.getLocation().distance(entity.getLocation()) <= DRAIN_RANGE) {
                event.setCancelled(true);
                GamePlayer gp = Main.game.GetPlayer(shooter);
                if (gp != null) gp.applyDamage(6.0f * damageMultiplier, this, false);
                shooter.getWorld().spawnParticle(Particle.DUST, shooter.getLocation().add(0,1,0), 12, 0.2,0.3,0.2,0.01);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (entity == null || !entity.isValid()) return;
        if (!(event.getEntity() instanceof Player)) return;
        // if the arcane nullifier is hit while reflecting, reflect some magic back
        if (event.getDamager() != null && reflectTicks > 0 && event.getDamager() instanceof Player attacker) {
            GamePlayer gp = Main.game.GetPlayer(attacker);
            if (gp != null) gp.applyDamage(4.0f * damageMultiplier, this, false);
        }
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}

    @Override
    public void Destroy() {
        HandlerList.unregisterAll(this);
        super.Destroy();
    }
}
