package com.yourfault.handler;

import com.yourfault.system.Player;
import com.yourfault.listener.WeaponSelectionListener;
import com.yourfault.system.Player;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;



public class WeaponSelectionHandler {
    private final HashMap<UUID, Player> playerList;

    public WeaponSelectionHandler(HashMap<UUID, Player> playerList) {
        this.playerList = playerList;
    }


    public void handleWeaponSelection(org.bukkit.entity.Player bukkitPlayer, WeaponSelectionListener.WeaponType weaponType) {
        UUID uuid = bukkitPlayer.getUniqueId();
        Player player = new Player(bukkitPlayer,100f,100f,100f, weaponType);
        playerList.put(uuid, player);
        ItemStack weaponItem = weaponType.createCombatItem();
        bukkitPlayer.getInventory().setItem(0, weaponItem);
        bukkitPlayer.getInventory().setHeldItemSlot(0);
        bukkitPlayer.sendMessage(ChatColor.GREEN + "You selected " + weaponType.displayName() + ChatColor.GREEN + "!");
    }
}
