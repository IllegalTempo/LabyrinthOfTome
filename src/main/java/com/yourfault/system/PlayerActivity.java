package com.yourfault.system;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerActivity implements Listener {
    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent e)
    {
        Player player = e.getPlayer();
        Main.game.AddPlayer(player);
        GamePlayer gp = Main.game.GetPlayer(player);
        gp.PLAYER_TAB.initTab();

    }
    @EventHandler
    public void OnPlayerLeave(org.bukkit.event.player.PlayerQuitEvent e)
    {
        Player player = e.getPlayer();
        Main.game.RemovePlayer(player);
    }
}
