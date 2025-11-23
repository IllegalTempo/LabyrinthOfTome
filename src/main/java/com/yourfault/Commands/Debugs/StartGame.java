package com.yourfault.Commands.Debugs;

import com.yourfault.Main;
import com.yourfault.wave.WaveDifficulty;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StartGame implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        WaveDifficulty difficulty = WaveDifficulty.EASY;
        if (args.length > 0) {
            difficulty = WaveDifficulty.fromInput(args[0]);
        }
        Main.game.StartGame(difficulty);
        commandSender.sendMessage("§aGame started on " + difficulty.displayName() + " difficulty.");
        commandSender.sendMessage("§7Available difficulties: " + WaveDifficulty.optionsList());
        return true;
    }
}
