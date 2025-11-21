package com.yourfault.weapon.Excalibur;

import com.yourfault.Main;
import com.yourfault.listener.WeaponSelectionListener;
import com.yourfault.system.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class Excalibur_Main implements Listener {
    @EventHandler
    public void OnRightClick(PlayerInteractEvent e)
    {
        if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR)
        {
            Player player = Main.game.GetPlayer(e.getPlayer().getUniqueId());
            WeaponSelectionListener.WeaponType selectedWeapon = player.SELECTED_WEAPON;
            if(selectedWeapon != WeaponSelectionListener.WeaponType.Excalibur) return;
            if(e.getPlayer().getInventory().getItemInMainHand().getItemMeta() == null) return;
            String itemname = e.getPlayer().getInventory().getItemInMainHand().getItemMeta().getItemName();
            if(itemname.equals("excalibur"))
            {
                player.ChangeMana(-5f);
                new Sword_Aura(e.getPlayer().getLocation(),1f);

            }
        }

    }
}
