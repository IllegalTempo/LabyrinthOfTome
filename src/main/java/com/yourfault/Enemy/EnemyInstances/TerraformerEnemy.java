package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.Terraformer_Type;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TerraformerEnemy extends Enemy implements Listener {
    private static final double QUAKE_RANGE = 12.0;
    private static final int QUAKE_COOLDOWN = 120;
    private static final int PILLAR_COOLDOWN = 160;
    private static final int MUD_COOLDOWN = 200;

    private int quakeCd = 40;
    private int pillarCd = 80;
    private int mudCd = 120;
    private float rockArmor = 0f;

    public TerraformerEnemy(Mob entity, WaveContext context, Terraformer_Type type) {
        super(entity, context, 5L, type);
        Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
        rockArmor = 0.5f; // 50% damage reduction
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) return;
        if (quakeCd > 0) quakeCd -= 5;
        if (pillarCd > 0) pillarCd -= 5;
        if (mudCd > 0) mudCd -= 5;

        if (quakeCd <= 0) {
            performQuake();
            quakeCd = QUAKE_COOLDOWN;
        }
        if (pillarCd <= 0) {
            performPillarSlam();
            pillarCd = PILLAR_COOLDOWN;
        }
        if (mudCd <= 0) {
            createMudPit();
            mudCd = MUD_COOLDOWN;
        }
    }

    private void performQuake() {
        Location loc = entity.getLocation();
        entity.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc.add(0,1,0), 6, 0.6,0.3,0.6,0.02);
        entity.getWorld().playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.8f);
        for (Player p : entity.getNearbyEntities(QUAKE_RANGE, QUAKE_RANGE, QUAKE_RANGE).stream().filter(e->e instanceof Player).map(e->(Player)e).toList()) {
            Vector up = new Vector(0,1.2,0);
            p.setVelocity(up);
            GamePlayer gp = Main.game.GetPlayer(p);
            if (gp != null) gp.applyDamage(4.0f, this, false);
        }
    }

    private void performPillarSlam() {
        Player target = getNearestPlayer() != null ? getNearestPlayer().getMinecraftPlayer() : null;
        if (target == null) return;
        Location tloc = target.getLocation().clone();
        // spawn a falling block pillar
        FallingBlock pillar = entity.getWorld().spawnFallingBlock(tloc.add(0,0,0), org.bukkit.Material.COBBLESTONE.createBlockData());
        pillar.setDropItem(false);
        target.setVelocity(new Vector(0,1.8,0));
        // throw a boulder after a short delay
        Bukkit.getScheduler().runTaskLater(Main.plugin, () -> {
            FallingBlock boulder = entity.getWorld().spawnFallingBlock(entity.getLocation().add(0,1,0), org.bukkit.Material.MOSSY_COBBLESTONE.createBlockData());
            boulder.setDropItem(false);
            Vector dir = target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(1.2).setY(0.6);
            boulder.setVelocity(dir);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.7f, 0.9f);
        }, 8L);
    }

    private void createMudPit() {
        Location center = entity.getLocation().clone();
        entity.getWorld().spawnParticle(Particle.SPLASH, center.add(0,0.2,0), 40, 1.5,0.2,1.5,0.02);
        for (Entity e : entity.getNearbyEntities(6,2,6)) {
            if (e instanceof Player p) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 2, true, true, true));
            }
        }
        entity.getWorld().playSound(center, Sound.BLOCK_GRAVEL_BREAK, 0.8f, 0.9f);
    }

    @Override
    public void applyDamage(float damage, com.yourfault.system.LabyrinthCreature damageDealer, boolean bypassChain) {
        // rock armor reduces incoming damage
        float effective = damage;
        if (rockArmor > 0) {
            effective = damage * (1.0f - rockArmor);
        }
        super.applyDamage(effective, damageDealer, bypassChain);
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
