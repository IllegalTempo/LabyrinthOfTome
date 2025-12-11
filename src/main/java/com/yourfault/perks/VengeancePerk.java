package com.yourfault.perks;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;

public class VengeancePerk extends PerkType {
    public VengeancePerk() {
        super(
                "Vengeance",
                List.of(
                        ChatColor.GRAY + "Their pain is your strength",
                        ChatColor.GRAY + "Perk Ability:",
                        ChatColor.WHITE + "Each kill grants player speed",
                        ChatColor.WHITE + "and health regen"
                ),
                PerkCategory.LEVEL,
                20,
                100,
                50
        );
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (Main.game.ENEMY_LIST.containsKey(event.getEntity().getUniqueId())) {
            try {
                if (Main.game.getWaveManager() != null && Main.game.getWaveManager().isWaveInProgress()) {
                    // Active wave handling will apply Vengeance via PlayerRewarder
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        if (event.getEntity().getKiller() != null) {
            GamePlayer player = Main.game.GetPlayer(event.getEntity().getKiller());
            if (player != null && player.PLAYER_PERKS.hasPerk(this)) {
                int level = player.PLAYER_PERKS.getPerkLevel(this);
                if (level > 0) applyKillBonus(player, level);
            }
        }
    }

    public void applyKillBonus(GamePlayer player, int level) {
        if (player == null || level <= 0) return;

        float healAmount = 2.0f + (level * 1.0f);
        player.setHealth(player.GetHealth() + healAmount);

        // Speed boost: +100 speed (doubles base 100) for 3 seconds (60 ticks)
        int speedBonus = 100;
        int duration = 60;
        player.applySpeedBoost(speedBonus, duration);
    }

    @Override
    protected Material resolveIconMaterial() {
        return Material.REDSTONE;
    }
}
