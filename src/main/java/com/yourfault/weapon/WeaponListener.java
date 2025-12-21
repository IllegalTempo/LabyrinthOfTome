package com.yourfault.weapon;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import static com.yourfault.Main.plugin;

public class WeaponListener implements Listener {

    @EventHandler
    public void OnRightClick(PlayerInteractEvent e)
    {
        GamePlayer gamePlayer = Main.game.GetPlayer(e.getPlayer().getUniqueId());
        WeaponType type = gamePlayer.SELECTED_WEAPON;

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            if(!gamePlayer.ActionReady(type,type.rc_mana)) return;
            gamePlayer.weaponObject.RC();
        }
        if(e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)
        {
            if(!gamePlayer.ActionReady(type,type.lc_mana)) return;
            gamePlayer.weaponObject.LC();
        }
    }
    @EventHandler
    public void OnMelee(PrePlayerAttackEntityEvent e)
    {
        GamePlayer gamePlayer = Main.game.GetPlayer(e.getPlayer().getUniqueId());
        WeaponType type = gamePlayer.SELECTED_WEAPON;
        if(!gamePlayer.ActionReady(type,type.lc_mana)) return;
        gamePlayer.weaponObject.LC();
    }
    @EventHandler
    public void on_F_clicked(PlayerSwapHandItemsEvent e)
    {
        e.setCancelled(true);
        GamePlayer gamePlayer = Main.game.GetPlayer(e.getPlayer().getUniqueId());
        WeaponType type = gamePlayer.SELECTED_WEAPON;
        if(!gamePlayer.ActionReady(type,type.fc_mana)) return;
        gamePlayer.weaponObject.FC();

    }
}
