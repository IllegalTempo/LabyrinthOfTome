package com.yourfault.weapon.Excalibur;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.utils.AnimationInfo;
import com.yourfault.utils.ItemUtil;
import com.yourfault.weapon.WeaponType;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static com.yourfault.Main.plugin;

public class Excalibur_Main implements Listener {
    private static final AnimationInfo ANIMATION_LC = new AnimationInfo("animation_lc",15L);
    private static final AnimationInfo ANIMATION_RC = new AnimationInfo("animation_rc",10L);
    @EventHandler
    public void OnRightClick(PlayerInteractEvent e)
    {
        GamePlayer gamePlayer = Main.game.GetPlayer(e.getPlayer().getUniqueId());
        if (gamePlayer == null) return;
        if(gamePlayer.inActionTicks > 0) return;
        WeaponType selectedWeapon = gamePlayer.SELECTED_WEAPON;
        if(selectedWeapon != WeaponType.Excalibur) return;
        if(e.getPlayer().getInventory().getItemInMainHand().getItemMeta() == null) return;
        List<String> customData = e.getPlayer().getInventory().getItemInMainHand().getItemMeta().getCustomModelDataComponent().getStrings();
        if(customData.size() == 0) return;
        String itemname = customData.get(0);
        if(itemname.equals("excalibur")) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {


                gamePlayer.playAnimation(ANIMATION_RC);
                Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                    gamePlayer.ChangeMana(-5f);
                    new Sword_Aura(e.getPlayer(), 10f);
                }, 7);

            }
            if(e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)
            {
                gamePlayer.playAnimation(ANIMATION_LC);

            }
        }

    }
}
