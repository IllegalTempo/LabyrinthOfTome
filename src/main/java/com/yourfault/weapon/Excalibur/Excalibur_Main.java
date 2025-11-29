package com.yourfault.weapon.Excalibur;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
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
    private static final long ANIMATION_LC_LENGTH = 18L;
    @EventHandler
    public void OnRightClick(PlayerInteractEvent e)
    {
        GamePlayer gamePlayer = Main.game.GetPlayer(e.getPlayer().getUniqueId());
        if (gamePlayer == null) return;
        WeaponType selectedWeapon = gamePlayer.SELECTED_WEAPON;
        if(selectedWeapon != WeaponType.Excalibur) return;
        if(e.getPlayer().getInventory().getItemInMainHand().getItemMeta() == null) return;
        List<String> customData = e.getPlayer().getInventory().getItemInMainHand().getItemMeta().getCustomModelDataComponent().getStrings();
        if(customData.size() == 0) return;
        String itemname = customData.get(0);
        if(itemname.equals("excalibur")) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {

                gamePlayer.ChangeMana(-5f);
                new Sword_Aura(e.getPlayer().getEyeLocation(), 10f);


            }
            if(e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)
            {
                ItemMeta meta = e.getPlayer().getInventory().getItemInMainHand().getItemMeta();
                long frameoffset = ((Main.world.getFullTime() % 24000) - 0) % ANIMATION_LC_LENGTH;
                gamePlayer.MINECRAFT_PLAYER.sendMessage("Frame Offset: " + frameoffset);
                List<ItemUtil.ISpair> strings = new ArrayList<>();
                strings.add(new ItemUtil.ISpair(1,"animation_lc"));
                ItemMeta finalmeta = ItemUtil.SetItemCMDTint(meta,frameoffset);


                e.getPlayer().getInventory().getItemInMainHand().setItemMeta(ItemUtil.SetCustomModelData(finalmeta,strings));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    e.getPlayer().getInventory().getItemInMainHand().setItemMeta(ItemUtil.SetCustomModelData(meta,1,"0"));
                }, ANIMATION_LC_LENGTH);


            }
        }

    }
}
