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

public final class SharpshooterPerk extends PerkType{


    public SharpshooterPerk() {
        super("Sharpshooter",
                List.of(
                ChatColor.GRAY + "A steady hand and a keen eye",
                ChatColor.GRAY + "Perk Ability:",
                ChatColor.WHITE + "Arrows travel 25% faster",
                ChatColor.WHITE + "Deal +3 bow damage"
        ),15,400);
    }
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
        if (!gamePlayer.PLAYER_PERKS.hasPerk(this)) return;

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

        if (!gamePlayer.PLAYER_PERKS.hasPerk(this)) return;
        event.setDamage(event.getDamage() + BONUS_DAMAGE);
        player.sendMessage(ChatColor.AQUA + "Sharpshooter bonus applied! +3 damage");
    }
}

