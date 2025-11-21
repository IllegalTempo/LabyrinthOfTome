package com.yourfault.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;

import java.util.*;

public class WeaponSelectionListener implements Listener {

    private final Map<UUID, Boolean> hasSelectedWeapon = new HashMap<>();
    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    public enum WeaponType {
        Excalibur,
    }



}
