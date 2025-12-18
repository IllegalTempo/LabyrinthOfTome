package com.yourfault.Commands.Debugs;

import com.yourfault.Items.weapons;
import com.yourfault.Main;
import com.yourfault.NBT_namespace;
import com.yourfault.weapon.WeaponType;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class SummonNPC implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        Player player = ((Player)commandSender);
        if(Objects.equals(strings[0], "excalibur"))
        {
            SpawnNPC_Excalibur(player.getLocation());
        }

        return false;
    }
    private void SpawnNPC_Excalibur(Location loc)
    {
        Main.world.spawn(loc, Mannequin.class, mannequin -> {
            mannequin.setCustomName("§bExcalibur");
            mannequin.setCustomNameVisible(true);
            mannequin.getEquipment().setItemInMainHand(weapons.EXCALIBUR().clone());
            mannequin.setInvulnerable(true);
            mannequin.setCollidable(false);
            mannequin.getPersistentDataContainer().set(NBT_namespace.SELECT_NPC_Weapon, PersistentDataType.STRING, WeaponType.Excalibur.toString());

            mannequin.setDescription(Component.text("Right Click\u2f00"));
            PlayerProfile displaySkin = Bukkit.createPlayerProfile("excalibur_npc");
            URL skinurl = null;
            try {
                skinurl = new URL("https://textures.minecraft.net/texture/5c29216c211059d7c0e681d74588eb53cb263c200ccdbe2efc9cd63c1c596784");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            displaySkin.getTextures().setSkin(skinurl);
            ResolvableProfile profile = ResolvableProfile.resolvableProfile((com.destroystokyo.paper.profile.PlayerProfile) displaySkin);
            mannequin.setProfile(profile);
            //mannequin.setPlayerProfile(displaySkin);

        });
    }
}
