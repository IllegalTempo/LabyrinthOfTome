package com.yourfault.perks;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SharpshooterPerk extends PerkType {


    public SharpshooterPerk() {
        super(
                "Sharpshooter",
                List.of(
                        ChatColor.GRAY + "A steady hand and a keen eye",
                        ChatColor.GRAY + "Perk Ability:",
                        ChatColor.WHITE + "Arrows travel 25% faster",
                        ChatColor.WHITE + "Deal +3 bow damage (scales with level)"
                ),
                PerkCategory.LEVEL,
                30,
                600,
                250
        );
    }

    private static final double BASE_VELOCITY_MULTIPLIER = 1.25;
    private static final double VELOCITY_PER_LEVEL = 0.01;
    private static final double BASE_DAMAGE = 3.0;
    private static final double DAMAGE_PER_LEVEL = 0.25;

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
        if (!gamePlayer.PLAYER_PERKS.hasPerk(this)) return;

        int level = gamePlayer.PLAYER_PERKS.getPerkLevel(this);
        double velocityMultiplier = BASE_VELOCITY_MULTIPLIER + (Math.max(0, level - 1) * VELOCITY_PER_LEVEL);
        Vector current = arrow.getVelocity();
        arrow.setVelocity(current.multiply(velocityMultiplier));
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

        if (!gamePlayer.PLAYER_PERKS.hasPerk(this)) return;
        int level = gamePlayer.PLAYER_PERKS.getPerkLevel(this);
        double bonusDamage = BASE_DAMAGE + (Math.max(0, level - 1) * DAMAGE_PER_LEVEL);
        event.setDamage(event.getDamage() + bonusDamage);
        player.sendMessage(ChatColor.AQUA + "Sharpshooter bonus applied! +" + String.format("%.1f", bonusDamage) + " damage");
    }
}

