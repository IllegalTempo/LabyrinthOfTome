package com.yourfault.handler;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import com.yourfault.listener.WeaponSelectionListener;
import com.yourfault.system.Player;



public class WeaponSelectionHandler {
    private final HashMap<UUID, Player> playerList;

    public WeaponSelectionHandler(HashMap<UUID, Player> playerList) {
        this.playerList = playerList;
    }

    public boolean hasSelectedWeapon(UUID uuid) {
        Player player = playerList.get(uuid);
        return player != null && player.SELECTED_WEAPON != null;
    }

    public void handleWeaponSelection(org.bukkit.entity.Player bukkitPlayer, WeaponSelectionListener.WeaponType weaponType) {
        UUID uuid = bukkitPlayer.getUniqueId();
        if (hasSelectedWeapon(uuid)) {
            bukkitPlayer.sendMessage(ChatColor.YELLOW + "You already selected a weapon.");
            return;
        }
        Player player = playerList.get(uuid);
        if (player == null) {
            player = new Player(bukkitPlayer,100f,100f,100f, weaponType);
            playerList.put(uuid, player);
        } else {
            player.setMinecraftPlayer(bukkitPlayer);
            player.SELECTED_WEAPON = weaponType;
        }
        ItemStack weaponItem = weaponType.createCombatItem();
        bukkitPlayer.getInventory().setItem(0, weaponItem);
        bukkitPlayer.getInventory().setHeldItemSlot(0);
        bukkitPlayer.sendMessage(ChatColor.GREEN + "You selected " + weaponType.displayName() + ChatColor.GREEN + "!");
    }

    public void clearSelection(org.bukkit.entity.Player bukkitPlayer) {
        Player player = playerList.get(bukkitPlayer.getUniqueId());
        if (player != null) {
            player.SELECTED_WEAPON = null;
        }
    }
}
