package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

public class SunderMawEnemy extends Enemy {

    private boolean isBurrowed = false;
    private int burrowTicks = 0;
    private static final int BURROW_DURATION = 60;
    private static final int SURFACE_DURATION = 100;

    public SunderMawEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 5L, type);
    }

    @Override
    public void update() {
        if (entity.isDead()) return;

        if (entity.getTarget() == null && getNearestPlayer() != null) {
            entity.setTarget(getNearestPlayer().MINECRAFT_PLAYER);
        }

        burrowTicks++;

        if (!isBurrowed && burrowTicks > SURFACE_DURATION) {
            isBurrowed = true;
            burrowTicks = 0;
            entity.setInvisible(true);
            entity.setInvulnerable(true);
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.5f);
        } else if (isBurrowed) {
            if (entity.getTarget() != null) {
                Location targetLoc = entity.getTarget().getLocation();
                entity.getPathfinder().moveTo(targetLoc);
                // Particles at feet
                entity.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation(), 10, 0.5, 0.1, 0.5, 0.1, org.bukkit.Material.DIRT.createBlockData());
            }

            if (burrowTicks > BURROW_DURATION) {
                isBurrowed = false;
                burrowTicks = 0;
                entity.setInvisible(false);
                entity.setInvulnerable(false);
                entity.setVelocity(new Vector(0, 0.8, 0));
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
                entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), 1);
            }
        }
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {
        if (entity.getTarget() instanceof Player) {
            Player p = (Player) entity.getTarget();
            applyArmorCrack(p);
        }
    }

    private void applyArmorCrack(Player p) {
        AttributeInstance armor = p.getAttribute(Attribute.valueOf("GENERIC_ARMOR"));
        if (armor != null) {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(Main.plugin, "armor_crack_" + UUID.randomUUID().toString());
            AttributeModifier modifier = new AttributeModifier(key, -3.0, AttributeModifier.Operation.ADD_NUMBER, org.bukkit.inventory.EquipmentSlotGroup.ANY);
            armor.addModifier(modifier);
            p.sendMessage("§cArmor Cracked! -3 Defense");
            p.playSound(p.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
            trackModifier(p, modifier);
        }
    }
    
    private final java.util.Map<UUID, java.util.List<AttributeModifier>> appliedModifiers = new java.util.HashMap<>();

    private void trackModifier(Player p, AttributeModifier modifier) {
        appliedModifiers.computeIfAbsent(p.getUniqueId(), k -> new java.util.ArrayList<>()).add(modifier);
    }

    @Override
    public void Destroy() {
        for (java.util.Map.Entry<UUID, java.util.List<AttributeModifier>> entry : appliedModifiers.entrySet()) {
            Player p = org.bukkit.Bukkit.getPlayer(entry.getKey());
            if (p != null) {
                AttributeInstance armor = p.getAttribute(Attribute.valueOf("GENERIC_ARMOR"));
                if (armor != null) {
                    for (AttributeModifier mod : entry.getValue()) {
                        armor.removeModifier(mod);
                    }
                }
                p.sendMessage("§aArmor Crack removed.");
            }
        }
        super.Destroy();
    }
}
