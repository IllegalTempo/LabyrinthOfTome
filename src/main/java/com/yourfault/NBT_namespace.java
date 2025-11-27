package com.yourfault;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class NBT_namespace {
    public static final NamespacedKey SELECT_NPC_Weapon = new NamespacedKey(JavaPlugin.getPlugin(Main.class), "weapon");
    public static final NamespacedKey PERK_TYPE = new NamespacedKey(JavaPlugin.getPlugin(Main.class), "perk_option");

}
