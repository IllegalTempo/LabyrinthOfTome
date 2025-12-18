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

public abstract class WeaponListener implements Listener {
    WeaponType type;
    protected float lc_mana;
    protected float rc_mana;
    protected float fc_mana;

    public WeaponListener(WeaponType type,float lc_mana,float rc_mana,float fc_mana){
        this.type = type;
        this.lc_mana = lc_mana;
        this.rc_mana = rc_mana;
        this.fc_mana = fc_mana;
    }
    @EventHandler
    public void OnRightClick(PlayerInteractEvent e)
    {
        GamePlayer gamePlayer = Main.game.GetPlayer(e.getPlayer().getUniqueId());
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            if(!gamePlayer.ActionReady(type,rc_mana)) return;
            RC(gamePlayer);
        }
        if(e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)
        {
            if(!gamePlayer.ActionReady(type,lc_mana)) return;
            LC(gamePlayer);
        }
    }
    @EventHandler
    public void OnMelee(PrePlayerAttackEntityEvent e)
    {
        GamePlayer gamePlayer = Main.game.GetPlayer(e.getPlayer().getUniqueId());
        if(!gamePlayer.ActionReady(type,lc_mana)) return;
        LC(gamePlayer);
    }
    @EventHandler
    public void on_F_clicked(PlayerSwapHandItemsEvent e)
    {
        e.setCancelled(true);
        GamePlayer gamePlayer = Main.game.GetPlayer(e.getPlayer().getUniqueId());
        if(!gamePlayer.ActionReady(type,fc_mana)) return;
        FC(gamePlayer);

    }
    public abstract void LC(GamePlayer player);
    public abstract void RC(GamePlayer player);
    public abstract void FC(GamePlayer player);
}
