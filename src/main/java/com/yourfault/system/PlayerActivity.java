package com.yourfault.system;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerActivity implements Listener {
    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent e)
    {
        Player player = e.getPlayer();
        Main.game.AddPlayer(player);
        for(GamePlayer p: Main.game.PLAYER_LIST.values())
        {
            p.PLAYER_TAB.playerlist_removePlaceholder();
        }



    }
    @EventHandler
    public void OnPlayerLeave(org.bukkit.event.player.PlayerQuitEvent e)
    {
        Player player = e.getPlayer();
        Main.game.RemovePlayer(player);
        for(GamePlayer p: Main.game.PLAYER_LIST.values())
        {
            p.PLAYER_TAB.playerlist_addPlaceholder();
        }
    }
    @EventHandler
    public void OnPlayerClick(PlayerItemHeldEvent e)
    {
        GamePlayer player = Main.game.GetPlayer(e.getPlayer());
        if(player != null)
        {
            player.OnNumKeyPress(e.getNewSlot()+1);
        }
    }
}
