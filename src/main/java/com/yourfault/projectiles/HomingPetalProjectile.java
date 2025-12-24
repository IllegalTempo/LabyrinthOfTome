package com.yourfault.projectiles;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.Comparator;

import static com.yourfault.Main.world;

public class HomingPetalProjectile extends Projectile {

    private LabyrinthCreature target;
    private final float turnSpeed = 0.6f;

    public HomingPetalProjectile(Location StartLocation, float damage, GamePlayer owner) {
        super(StartLocation, 1.0f, damage, 1.5f, false, 40, owner);
    }

    @Override
    public void ChildUpdate()
    {
        world.spawnParticle(Particle.CHERRY_LEAVES, getDisplayedLocation(), 5, 0.1, 0.1, 0.1, 0.02);
        world.spawnParticle(Particle.WAX_ON, getDisplayedLocation(), 1, 0.05, 0.05, 0.05, 0.01);

        if (target == null || target.HEALTH <= 0 || !target.minecraftEntity.isValid()) {
            findTarget();
        }

        if (target != null) {
            Vector directionToTarget = target.minecraftEntity.getLocation().add(0, target.minecraftEntity.getHeight() / 2, 0).toVector().subtract(entity.getLocation().toVector()).normalize();
            Vector currentDirection = entity.getLocation().getDirection();
            Vector newDirection = currentDirection.add(directionToTarget.multiply(turnSpeed)).normalize();
            setDirection(newDirection);
        }
    }

    private void findTarget() {
        target = Main.game.CREATURE_LIST.values().stream()
                .filter(c -> c.team != owner.team && c.HEALTH > 0 && c.minecraftEntity.isValid())
                .filter(c -> c.minecraftEntity.getLocation().distanceSquared(entity.getLocation()) < 400)
                .min(Comparator.comparingDouble(c -> c.minecraftEntity.getLocation().distanceSquared(entity.getLocation())))
                .orElse(null);
    }
}
