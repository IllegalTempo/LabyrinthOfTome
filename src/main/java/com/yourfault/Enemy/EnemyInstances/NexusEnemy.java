package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.projectiles.SoulMissileProjectile;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.LabyrinthCreature;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class NexusEnemy extends Enemy {

    private List<ArmorStand> shields = new ArrayList<>();
    private double shieldAngle = 0;
    private int attackCooldown = 0;
    private boolean channeling = false;
    private float damageTakenDuringChannel = 0;
    private final float CHANNEL_INTERRUPT_THRESHOLD = 50.0f;

    public NexusEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 5L, type);
        createShields();
    }

    private void createShields() {
        for (int i = 0; i < 3; i++) {
            ArmorStand as = (ArmorStand) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.ARMOR_STAND);
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.setSmall(true);
            as.getEquipment().setHelmet(new ItemStack(Material.SHIELD));
            shields.add(as);
        }
    }

    @Override
    public void update() {
        if (entity.isDead()) {
            removeShields();
            return;
        }

        updateShields();

        if (channeling) {
            performSoulDrain();
            return;
        }
        if (HEALTH <= MAX_HEALTH * 0.4 && !channeling) {
            if (attackCooldown <= 0) {
                startChanneling();
            }
        }
        if (attackCooldown > 0) {
            attackCooldown = Math.max(0, attackCooldown - 1);
        }
        if (!channeling && entity.getTarget() instanceof Player) {
            Player target = (Player) entity.getTarget();
            if (target != null && entity.hasLineOfSight(target)) {
                if (attackCooldown <= 0) {
                    shootMissile(target);
                    attackCooldown = 20;
                }
            }
        }

        if (entity.getTarget() == null && getNearestPlayer() != null) {
            entity.setTarget(getNearestPlayer().MINECRAFT_PLAYER);
        }
    }

    private void updateShields() {
        shieldAngle += 0.1;
        double radius = 2.0;
        for (int i = 0; i < shields.size(); i++) {
            double angle = shieldAngle + (i * (2 * Math.PI / shields.size()));
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location loc = entity.getLocation().add(x, 1, z);
            // Face away from center
            loc.setYaw((float) Math.toDegrees(Math.atan2(z, x)) + 90);
            shields.get(i).teleport(loc);
        }
    }

    private void removeShields() {
        for (ArmorStand as : shields) {
            as.remove();
        }
        shields.clear();
    }

    private void shootMissile(Player target) {
        Location spawnLoc = entity.getEyeLocation();
        new SoulMissileProjectile(spawnLoc, this, target);
    }

    private void startChanneling() {
        channeling = true;
        damageTakenDuringChannel = 0;
        entity.setAI(false); // Stop moving
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);
    }

    private void performSoulDrain() {
        entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation().add(0, 2, 0), 5);

        if (getNearestPlayer() != null) {
            Player p = getNearestPlayer().MINECRAFT_PLAYER;
            if (p != null && entity.hasLineOfSight(p)) {
                GamePlayer gp = Main.game.GetPlayer(p);
                gp.applyDamage(1.0f,this,false);//todo check
                HEALTH = Math.min(MAX_HEALTH, HEALTH + 2.0f);
                updateDisplay();
                drawBeam(entity.getEyeLocation(), p.getEyeLocation());
            }
        }
    }

    private void drawBeam(Location start, Location end) {
        double distance = start.distance(end);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        for (double d = 0; d < distance; d += 0.5) {
            Location particleLoc = start.clone().add(direction.clone().multiply(d));
            start.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    private void interruptChanneling() {
        channeling = false;
        entity.setAI(true);
        attackCooldown = 100;
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), 10);
    }

    @Override
    public void applyDamage(float damage, LabyrinthCreature damageDealer, boolean bypassChain) {
        if (damageDealer != null && damageDealer.minecraftEntity != null) {
            Vector toAttacker = damageDealer.minecraftEntity.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
            boolean blocked = false;
            for (ArmorStand shield : shields) {
                Vector toShield = shield.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
                if (toAttacker.dot(toShield) > 0.8) {
                    blocked = true;
                    break;
                }
            }

            if (blocked) {
                entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1, 0), 10);
                return;
            }
        }

        super.applyDamage(damage, damageDealer,bypassChain);

        if (channeling) {
            damageTakenDuringChannel += damage;
            if (damageTakenDuringChannel >= CHANNEL_INTERRUPT_THRESHOLD) {
                interruptChanneling();
            }
        }
    }

    @Override
    public void Destroy(GamePlayer killer) {
        removeShields();
        super.Destroy(killer);
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}
}
