package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.CreeperlingSwarmer_Type;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CreeperlingSwarmerEnemy extends Enemy {

    private boolean ignited = false;

    public CreeperlingSwarmerEnemy(Mob entity, WaveContext context, CreeperlingSwarmer_Type type) {
        super(entity,context,2L,type);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
        if (entity instanceof Creeper) {
            ((Creeper) entity).setMaxFuseTicks(60);
        }
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) return;
        if (!(entity instanceof Creeper)) return;

        if (entity.getTicksLived() < 20) return;

        Creeper creeper = (Creeper) entity;
        int maxFuse = creeper.getMaxFuseTicks();
        int currentFuse = creeper.getFuseTicks();
        if (currentFuse > 1 && currentFuse % 5 == 0) {
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1f, 2.0f);
        }
        if (currentFuse >= maxFuse - 1) {
            explode();
            entity.remove();
            return;
        }
        GamePlayer target = getNearestPlayer();
        if (target != null && target.getMinecraftPlayer() != null) {
            if (getDistance(target.getMinecraftPlayer()) < 3) {
                if (!ignited) {
                    creeper.ignite();
                    ignited = true;
                }
            }
        }
    }

    public void ignite() {
        if (entity instanceof Creeper && !ignited) {
            ((Creeper) entity).ignite();
            ignited = true;
        }
    }

    public void explode() {
        entity.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, entity.getLocation(), 1);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 2.0f);
        for (Entity e : entity.getNearbyEntities(4,4,4)) {
            if (e instanceof Player p) {
                p.damage(4.0, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 160, 1));
            } else if (e instanceof Creeper c) {
                Enemy enemy = Main.game.ENEMY_LIST.get(e.getUniqueId());
                if (enemy instanceof CreeperlingSwarmerEnemy swarmer) {
                    if (!swarmer.ignited) {
                        swarmer.ignite();
                        c.setFuseTicks(0);
                    }
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
