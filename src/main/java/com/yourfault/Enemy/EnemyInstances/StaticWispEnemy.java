package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class StaticWispEnemy extends Enemy {
    private int attackCooldown = 0;

    public StaticWispEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 10L, type);
    }

    @Override
    public void update() {
        if (entity.isDead()) return;

        if (entity.getTarget() == null) {
            if (getNearestPlayer() != null) {
                entity.setTarget(getNearestPlayer().MINECRAFT_PLAYER);
            }
        }

        if (attackCooldown > 0) {
            attackCooldown = Math.max(0, attackCooldown - 1);
        }

        if (attackCooldown <= 0) {
            List<Player> nearbyPlayers = getNearbyPlayers(8); // 8 block radius
            if (nearbyPlayers.size() >= 1) {
                
                Player p1 = nearbyPlayers.get(0);
                drawLine(entity.getEyeLocation(), p1.getEyeLocation());
                damagePlayer(p1);

                Player currentSource = p1;
                int chains = 0;
                
                for (int i = 1; i < nearbyPlayers.size() && chains < 2; i++) {
                    Player nextTarget = nearbyPlayers.get(i);
                    drawLine(currentSource.getEyeLocation(), nextTarget.getEyeLocation());
                    damagePlayer(nextTarget);
                    currentSource = nextTarget;
                    chains++;
                }
                
                attackCooldown = 10;
            }
        }
    }

    private List<Player> getNearbyPlayers(double radius) {
        List<Player> players = new ArrayList<>();
        for (Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player) {
                players.add((Player) e);
            }
        }
        return players;
    }

    private void damagePlayer(Player p) {
        GamePlayer gp = Main.game.GetPlayer(p);
        if (gp != null) {
            gp.applyDamage(4.0f * damageMultiplier,this,false); //todo check
            //maybe add a stun or slow?
        }
    }

    private void drawLine(Location start, Location end) {
        double distance = start.distance(end);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        for (double d = 0; d < distance; d += 0.5) {
            Location particleLoc = start.clone().add(direction.clone().multiply(d));
            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.AQUA, 1);
            start.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, dustOptions);
        }
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}
}
