package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.projectiles.IceShardProjectile;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Enemy.EnemyTypes.FrostShardArcher_Type;
import com.yourfault.Enemy.EnemyTypes.FrostbiteBat_Type;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

public class GlacierheartEnemy extends Enemy {

    private int attackCooldown = 0;
    private int summonCooldown = 0;
    private int novaCooldown = 0;

    public GlacierheartEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 10L, type);
    }

    @Override
    public void update() {
        if (entity.isDead()) return;
        boolean phase2 = HEALTH <= MAX_HEALTH * 0.5;
        if (entity.getTarget() == null && getNearestPlayer() != null) {
            entity.setTarget(getNearestPlayer().MINECRAFT_PLAYER);
        }
        if (attackCooldown > 0) attackCooldown--;
        if (summonCooldown > 0) summonCooldown--;
        if (novaCooldown > 0) novaCooldown--;
        if (entity.getTarget() instanceof Player) {
            Player target = (Player) entity.getTarget();
            if (target != null && entity.hasLineOfSight(target)) {
                if (attackCooldown <= 0) {
                    shootIceShard(target);
                    attackCooldown = 6;
                }
            }
        }
        if (summonCooldown <= 0) {
            summonMinions(phase2);
            summonCooldown = 40;
        }
        if (phase2 && novaCooldown <= 0) {
            castFrostNova();
            novaCooldown = 30;
        }
    }

    private void shootIceShard(Player target) {
        Location spawnLoc = entity.getEyeLocation();
        Vector direction = target.getEyeLocation().toVector().subtract(spawnLoc.toVector()).normalize();
        spawnLoc.setDirection(direction);
        new IceShardProjectile(spawnLoc, this);
    }

    private void summonMinions(boolean phase2) {
        Location loc = entity.getLocation();
        if (phase2) {
            FrostbiteBat_Type batType = new FrostbiteBat_Type();
            for (int i = 0; i < 3; i++) {
                Mob m = batType.SpawnEntity(loc.clone().add(Math.random()*4-2, 1, Math.random()*4-2));
                batType.CreateEnemyInstance(m, context);
            }
        } else {
            FrostShardArcher_Type archerType = new FrostShardArcher_Type();
            for (int i = 0; i < 2; i++) {
                Mob m = archerType.SpawnEntity(loc.clone().add(Math.random()*4-2, 0, Math.random()*4-2));
                archerType.CreateEnemyInstance(m, context);
            }
        }
        entity.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.0f, 2.0f);
    }

    private void castFrostNova() {
        Location loc = entity.getLocation();
        entity.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 100, 5, 1, 5, 0.1);
        entity.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);

        List<Entity> nearby = entity.getNearbyEntities(10, 5, 10);
        for (Entity e : nearby) {
            if (e instanceof Player) {
                Player p = (Player) e;
                GamePlayer gp = Main.game.GetPlayer(p);
                if (gp != null) {
                    gp.applySpeedBoost(-100, 40);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 200));
                    p.setFreezeTicks(140);
                    p.sendMessage("§bYou are frozen by the Glacierheart!");
                }
            }
        }
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}
}
