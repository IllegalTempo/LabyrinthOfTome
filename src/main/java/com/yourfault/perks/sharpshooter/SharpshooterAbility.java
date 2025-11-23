package com.yourfault.perks.sharpshooter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import com.yourfault.perks.PerkType;

public class SharpshooterAbility implements Listener {
    private static final double VELOCITY_MULTIPLIER = 1.25;
    private static final double BONUS_DAMAGE = 3.0;

    private final Set<UUID> empoweredArrows = new HashSet<>();

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        ProjectileSource shooter = arrow.getShooter();
        if (!(shooter instanceof Player player)) return;
        GamePlayer gamePlayer = Main.game.GetPlayer(player);
        if (gamePlayer == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!gamePlayer.PLAYER_PERKS.hasPerk(PerkType.SHARPSHOOTER)) return;

        Vector current = arrow.getVelocity();
        arrow.setVelocity(current.multiply(VELOCITY_MULTIPLIER));
        empoweredArrows.add(arrow.getUniqueId());
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Arrow arrow)) return;
        if (!empoweredArrows.remove(arrow.getUniqueId())) return;
        ProjectileSource shooter = arrow.getShooter();
        if (!(shooter instanceof Player player)) return;
        GamePlayer gamePlayer = Main.game.GetPlayer(player);
        if (gamePlayer == null) {
            return;
        }
        UUID uuid = player.getUniqueId();

        if (!gamePlayer.PLAYER_PERKS.hasPerk(PerkType.SHARPSHOOTER)) return;
        event.setDamage(event.getDamage() + BONUS_DAMAGE);
        player.sendMessage(ChatColor.AQUA + "Sharpshooter bonus applied! +3 damage");
    }
}

