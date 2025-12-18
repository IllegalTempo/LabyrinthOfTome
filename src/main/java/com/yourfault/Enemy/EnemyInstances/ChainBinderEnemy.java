package com.yourfault.Enemy.EnemyInstances;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.yourfault.projectiles.BindingChainProjectile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.listener.ChainLinkManager;
import com.yourfault.wave.WaveContext;

public class ChainBinderEnemy extends Enemy {
    private static final int MIN_LINK_SIZE = 3;
    private static final int MAX_LINK_SIZE = 5;
    private static final long LINK_DURATION = 20L * 12;
    private static final int LINK_HEALTH = 65;
    private static final double CAST_RANGE = 18.0;
    private int attackCooldown = 0;
    private final Random random = new Random();

    public ChainBinderEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 5L, type);
        if (entity.getEquipment() != null) {
            entity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_AXE));
        }
    }

    @Override
    public void update() {
        if (entity.isDead()) {
            return;
        }
        GamePlayer nearest = getNearestPlayer();
        if (nearest != null && nearest.getMinecraftPlayer() != null) {
            entity.setTarget(nearest.getMinecraftPlayer());
        }
        if (attackCooldown > 0) {
            attackCooldown = Math.max(0, attackCooldown - 5);
        }
        Player target = entity.getTarget() instanceof Player p ? p : null;
        if (target == null || !target.isOnline()) {
            return;
        }
        double distanceSq = entity.getLocation().distanceSquared(target.getLocation());
        if (distanceSq > CAST_RANGE * CAST_RANGE) {
            return;
        }
        if (attackCooldown <= 0 && entity.hasLineOfSight(target)) {
            castBindingChains(target);
            attackCooldown = 80;
        }
    }

    private void castBindingChains(Player target) {
        Location eye = entity.getEyeLocation();
        eye.setDirection(target.getEyeLocation().toVector().subtract(eye.toVector()));
        new BindingChainProjectile(eye, this);
        entity.swingMainHand();
    }

    public void onChainProjectileHit(GamePlayer struck) {
        if (struck == null || struck.getMinecraftPlayer() == null) {
            return;
        }
        List<GamePlayer> ordered = new ArrayList<>(Main.game.PLAYER_LIST.values());
        ordered.sort(Comparator.comparingDouble(player -> player.getMinecraftPlayer().getLocation().distanceSquared(struck.getMinecraftPlayer().getLocation())));
        if (ordered.size() < MIN_LINK_SIZE) {
            return;
        }
        int desired = Math.min(MAX_LINK_SIZE, MIN_LINK_SIZE + random.nextInt((MAX_LINK_SIZE - MIN_LINK_SIZE) + 1));
        ChainLinkManager.bindPlayers(entity.getUniqueId(), ordered, desired, LINK_HEALTH, LINK_DURATION);
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}

    @Override
    public void Destroy(GamePlayer killer) {
        ChainLinkManager.breakLinksOwnedBy(entity.getUniqueId());
        super.Destroy(killer);
    }
}
