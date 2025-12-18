package com.yourfault.npcinteraction;

import com.yourfault.Main;
import com.yourfault.NBT_namespace;
import com.yourfault.weapon.WeaponType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

public class WeaponSelect implements Listener {

    @EventHandler
    public void OnRightClickNPC(PlayerInteractEntityEvent e)
    {
        if(e.getHand() != EquipmentSlot.HAND) return;
        Player player = e.getPlayer();
        Entity rightclicked = e.getRightClicked();

        if(rightclicked.getType().equals(EntityType.MANNEQUIN))
        {
            String weapontype = rightclicked.getPersistentDataContainer().getOrDefault(NBT_namespace.SELECT_NPC_Weapon, PersistentDataType.STRING,"");
            if(weapontype.equals("")) return;
            try{
                WeaponType npcweapon = WeaponType.valueOf(weapontype);
                Mannequin npc = (Mannequin) rightclicked;
                EquipWeapon(player, npcweapon);
            }
            catch (IllegalArgumentException ex)
            {
                Bukkit.getLogger().warning("Invalid weapon type stored in NPC: " + weapontype);
                return;
            }


        }


    }
    public void EquipWeapon(Player player, WeaponType weaponType)
    {
        Main.game.GetPlayer(player.getUniqueId()).onPlayerSelectWeapon(weaponType);

    }


}
