package com.yourfault.wave;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

/**
 * Keeps enemy name tags in sync with their health for quick readability.
 */
public class EnemyHealthDisplay implements Listener {
    private final JavaPlugin plugin;

    public EnemyHealthDisplay(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void applyInitialTag(LivingEntity entity, WaveEnemyType type) {
        if (entity == null) {
            return;
        }
        updateDisplay(entity, type.displayName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onWaveEnemyDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (!isWaveEnemy(living)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> refresh(living));
    }

    @EventHandler(ignoreCancelled = true)
    public void onWaveEnemyHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (!isWaveEnemy(living)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> refresh(living));
    }

    public void refresh(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        String displayName = resolveDisplayName(entity);
        updateDisplay(entity, displayName);
    }

    private void updateDisplay(LivingEntity entity, String displayName) {
        double maxHealth = resolveMaxHealth(entity);
        double currentHealth = Math.max(0, Math.min(entity.getHealth(), maxHealth));
        String label = ChatColor.RED + String.format(Locale.US, "%.0f/%.0f HP ", currentHealth, maxHealth)
                + ChatColor.GRAY + displayName;
        entity.setCustomName(label);
        entity.setCustomNameVisible(true);
    }

    private boolean isWaveEnemy(LivingEntity entity) {
        return entity.getScoreboardTags().stream().anyMatch(tag -> tag.startsWith("lot_wave_enemy"));
    }

    private String resolveDisplayName(LivingEntity entity) {
        for (WaveEnemyType type : WaveEnemyType.values()) {
            String tag = "lot_wave_enemy_" + type.name().toLowerCase(Locale.ROOT);
            if (entity.getScoreboardTags().contains(tag)) {
                return type.displayName();
            }
        }
        return entity.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private double resolveMaxHealth(LivingEntity entity) {
        return Math.max(1.0, entity.getMaxHealth());
    }
}
