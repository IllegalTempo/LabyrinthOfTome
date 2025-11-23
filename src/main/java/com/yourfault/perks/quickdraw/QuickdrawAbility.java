package com.yourfault.perks.quickdraw;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.yourfault.perks.PerkType;

public class QuickdrawAbility implements Listener {
    private static final long COOLDOWN_MS = 1000L;
    private static final double ARROW_SPEED = 3.0;
    private static final Set<Material> SUPPORTED_ARROWS = EnumSet.of(
            Material.ARROW,
            Material.SPECTRAL_ARROW,
            Material.TIPPED_ARROW
    );

    private final Map<UUID, Long> cooldowns = new HashMap<>();


    @EventHandler
    public void onBowUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BOW) return;
        Player player = event.getPlayer();
        GamePlayer gamePlayer = Main.game.GetPlayer(player);
        if (gamePlayer == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!gamePlayer.PLAYER_PERKS.hasPerk(PerkType.QUICKDRAW)) return;

        long now = System.currentTimeMillis();
        long lastUse = cooldowns.getOrDefault(uuid, 0L);
        if (now - lastUse < COOLDOWN_MS) {
            event.setCancelled(true);
            long wait = COOLDOWN_MS - (now - lastUse);
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(ChatColor.YELLOW + "Quickdraw recharging: " + wait / 100 + "s"));
            return;
        }

        if (!hasArrow(player)) {
            player.sendMessage(ChatColor.RED + "You need arrows to use " + PerkType.QUICKDRAW.displayName() + ".");
            return;
        }

        event.setCancelled(true);
        fireInstantArrow(player);
        cooldowns.put(uuid, now);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    private boolean hasArrow(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        for (Material material : SUPPORTED_ARROWS) {
            if (player.getInventory().contains(material)) {
                return true;
            }
        }
        return false;
    }

    private void fireInstantArrow(Player player) {
        consumeArrow(player);
        Arrow arrow = player.launchProjectile(Arrow.class);
        Vector velocity = player.getLocation().getDirection().normalize().multiply(ARROW_SPEED);
        arrow.setVelocity(velocity);
        arrow.setCritical(true);
        player.setCooldown(Material.BOW, (int) (COOLDOWN_MS / 50));
    }

    private void consumeArrow(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        ItemStack bow = player.getInventory().getItemInMainHand();
        if (bow != null && bow.containsEnchantment(Enchantment.INFINITY)) {
            return;
        }
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null) continue;
            if (!SUPPORTED_ARROWS.contains(stack.getType())) continue;
            int amount = stack.getAmount();
            if (amount <= 1) {
                player.getInventory().setItem(slot, null);
            } else {
                stack.setAmount(amount - 1);
                player.getInventory().setItem(slot, stack);
            }
            return;
        }
    }
}
