package com.yourfault.Commands.Debugs;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetHeaderAndFooter implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        GamePlayer p = Main.game.GetPlayer((Player) commandSender);
        p.PLAYER_TAB.setHeaderFooter(strings[0], strings[1]);

        return true;
    }
}
