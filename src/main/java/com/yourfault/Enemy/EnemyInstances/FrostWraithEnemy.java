package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.projectiles.FrostWraithProjectile;
import com.yourfault.Enemy.EnemyTypes.FrostWraith_Type;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public class FrostWraithEnemy extends Enemy {

    private int attackCooldown = 0;

    public FrostWraithEnemy(Mob entity, WaveContext context, FrostWraith_Type type) {
        super(entity, context, 10L, type); // update every 10 ticks (0.5s)
        entity.getEquipment().clear();
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) return;

        // frost Aura (Every 0.5s, which is every update call)
        performFrostAura();

        // Ice Shard Attack
        if (attackCooldown > 0) {
            attackCooldown -= 10;
        } else {
            GamePlayer target = getNearestPlayer();
            if (target != null && target.getMinecraftPlayer() != null) {
                Player p = target.getMinecraftPlayer();
                if (getDistance(p) < 20) {
                    performIceShard(p);
                    attackCooldown = 40; // 2 seconds
                }
            }
        }
    }

    private void performFrostAura() {
        // Particles
        Location loc = entity.getLocation();
        for (int i = 0; i < 360; i += 20) {
            double angle = i * Math.PI / 180;
            double x = 2 * Math.cos(angle);
            double z = 2 * Math.sin(angle);
            loc.add(x, 0, z);

            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.AQUA, 1);
            entity.getWorld().spawnParticle(Particle.DUST, loc, 1, dustOptions);
            loc.subtract(x, 0, z);
        }

        // Damage
        for (Entity e : entity.getNearbyEntities(2, 2, 2)) {
            if (e instanceof Player) {
                ((Player) e).damage(0.5, entity);
            }
        }
    }

    private void performIceShard(Player target) {
        Location spawnLoc = entity.getEyeLocation();
        Location targetLoc = target.getEyeLocation();
        spawnLoc.setDirection(targetLoc.toVector().subtract(spawnLoc.toVector()));

        new FrostWraithProjectile(spawnLoc, this);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHOOT, 1f, 0.5f);
    }

    @Override
    public void tick() {

    }

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}
}
