package com.yourfault.weapon.Excalibur;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.weapon.WeaponType;
import org.bukkit.entity.LivingEntity;
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
            GamePlayer gamePlayer = Main.game.GetPlayer(e.getPlayer().getUniqueId());
            if (gamePlayer == null) return;
            WeaponType selectedWeapon = gamePlayer.SELECTED_WEAPON;
            if(selectedWeapon != WeaponType.Excalibur) return;
            if(e.getPlayer().getInventory().getItemInMainHand().getItemMeta() == null) return;
            String itemname = e.getPlayer().getInventory().getItemInMainHand().getItemMeta().getCustomModelDataComponent().getStrings().get(0);
            if(itemname.equals("excalibur"))
            {
                gamePlayer.ChangeMana(-5f);
                new Sword_Aura(e.getPlayer().getEyeLocation() , 10f);

            }
        }

    }
}
