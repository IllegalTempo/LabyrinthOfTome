package com.yourfault.Commands.Debug;

import com.yourfault.Main;
import com.yourfault.system.Game;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StartGame implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        Main.game.StartGame();
        return true;
    }
}
