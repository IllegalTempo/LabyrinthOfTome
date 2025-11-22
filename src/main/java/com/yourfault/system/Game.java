package com.yourfault.system;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.yourfault.Main;
import com.yourfault.handler.PerkSelectionHandler;
import com.yourfault.handler.WeaponSelectionHandler;
import com.yourfault.listener.PerkSelectionListener;
import com.yourfault.listener.WeaponSelectionListener;
import com.yourfault.weapon.General.Projectile;

public class Game {
    public Game(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }
    private JavaPlugin plugin;
    private BukkitRunnable StatDisplayTask;
    public Vector Gravity = new Vector(0,-0.5,0);
    public HashMap<UUID,Player> PLAYER_LIST = new HashMap<>();
    public HashMap<UUID,Projectile> PROJECTILE_LIST = new HashMap<>();
    public HashMap<UUID,Enemy> ENEMY_LIST = new HashMap<>();
    private WeaponSelectionListener weaponSelectionListener;
    private PerkSelectionListener perkSelectionListener;
    private WeaponSelectionHandler weaponSelectionHandler;
    private PerkSelectionHandler perkSelectionHandler;
    public void StartGame()
    {
        PLAYER_LIST.values().forEach(p -> {
            p.clearPerks();
            p.SELECTED_WEAPON = null;
        });
        PLAYER_LIST.clear();
        Main.world.getPlayers().forEach(player -> {
            player.sendTitle("§aGame Started","§7Select your default weapon!",10,40,20);
            if (perkSelectionListener != null) {
                perkSelectionListener.preparePlayer(player);
            }
            if (weaponSelectionListener != null) {
                UUID uuid = player.getUniqueId();
                Bukkit.getScheduler().runTaskLater(Main.plugin, () -> {
                    if (!player.isOnline()) return;
                    if (weaponSelectionHandler != null && weaponSelectionHandler.hasSelectedWeapon(uuid)) return;
                    weaponSelectionListener.openSelection(player);
                }, 40L);
            }
        });
        DisplayPlayerStat();
    }
    public void EndGame()
    {
        PLAYER_LIST.values().forEach(p -> {
            var bukkitPlayer = p.getMinecraftPlayer();
            if (bukkitPlayer != null) {
                bukkitPlayer.getInventory().setItem(0, null);
                if (perkSelectionHandler != null) {
                    perkSelectionHandler.removePerks(bukkitPlayer);
                }
                if (weaponSelectionHandler != null) {
                    weaponSelectionHandler.clearSelection(bukkitPlayer);
                }
            }
            p.clearPerks();
            p.SELECTED_WEAPON = null;
        });
        PLAYER_LIST.clear();
        if(StatDisplayTask != null)StatDisplayTask.cancel();
    }
    public Player GetPlayer(UUID uuid)
    {
        return PLAYER_LIST.get(uuid);
    }
    public void DisplayPlayerStat() {
        StatDisplayTask = new BukkitRunnable() {
            @Override
            public void run() {
                PLAYER_LIST.values().forEach(Player::DisplayStatToPlayer);
            }
        };
        StatDisplayTask.runTaskTimer(Main.plugin, 0L, 1L);
    }

    public void setWeaponSelectionHandler(WeaponSelectionHandler h) {
        this.weaponSelectionHandler = h;
    }

    public void setWeaponSelectionListener(WeaponSelectionListener weaponSelectionListener) {
        this.weaponSelectionListener = weaponSelectionListener;
    }

    public void setPerkSelectionListener(PerkSelectionListener perkSelectionListener) {
        this.perkSelectionListener = perkSelectionListener;
    }

    public void setPerkSelectionHandler(PerkSelectionHandler perkSelectionHandler) {
        this.perkSelectionHandler = perkSelectionHandler;
    }

    public WeaponSelectionHandler getWeaponSelectionHandler() {
        return weaponSelectionHandler;
    }

    public PerkSelectionHandler getPerkSelectionHandler() {
        return perkSelectionHandler;
    }
}
