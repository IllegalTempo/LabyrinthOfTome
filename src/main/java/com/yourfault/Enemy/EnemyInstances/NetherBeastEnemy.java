package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyProjectiles.NetherBeastProjectile;
import com.yourfault.Enemy.EnemyTypes.NetherBeast_Type;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.Locale;

public class NetherBeastEnemy extends Enemy {
    private int shieldCount = 3;
    private int attackCooldown = 0;

    public NetherBeastEnemy(Mob entity, WaveContext context, NetherBeast_Type type) {
        super(entity, context, 5L, type);
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) return;

        if (attackCooldown > 0) {
            attackCooldown -= 5;
        } else {
            GamePlayer target = getNearestPlayer();
            if (target != null && target.getMinecraftPlayer() != null) {
                Player p = target.getMinecraftPlayer();
                if (getDistance(p) < 20) {
                    performFireCharge(p);
                    attackCooldown = 60;
                }
            }
        }
    }

    private void performFireCharge(Player target) {
        Location spawnLoc = entity.getEyeLocation();
        Location targetLoc = target.getEyeLocation();
        spawnLoc.setDirection(targetLoc.toVector().subtract(spawnLoc.toVector()));

        new NetherBeastProjectile(spawnLoc, this);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
    }

    @Override
    public void tick() {}

    @Override
    public void OnBeingDamage(float damage, GamePlayer damageDealer) {
        if (shieldCount > 0) {
            shieldCount--;
            entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
            entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0,1,0), 10);
            updateDisplay();
            return;
        }
        super.OnBeingDamage(damage, damageDealer);
    }

    @Override
    protected void updateDisplay() {
        StringBuilder shieldStr = new StringBuilder();
        for (int i = 0; i < shieldCount; i++) {
            shieldStr.append("\\");
        }

        String label = ChatColor.RED + String.format(Locale.US, "%.0f/%.0f HP ", HEALTH, MaxHealth)
                + ChatColor.GOLD + shieldStr.toString() + " "
                + ChatColor.GRAY + enemyType.displayName;
        entity.setCustomName(label);

        if(enemyType.isBoss) {
            Main.game.BossHealthBar.progress(HEALTH/MaxHealth);
        }
    }

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}
}
