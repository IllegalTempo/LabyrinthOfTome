package com.yourfault.Commands.Debugs;

import com.yourfault.system.Game;
import com.yourfault.wave.WaveManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SkipWaveCommand implements CommandExecutor {
    private final Game game;

    public SkipWaveCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        WaveManager waveManager = game.getWaveManager();
        if (waveManager == null || !waveManager.isActive()) {
            sender.sendMessage(ChatColor.RED + "Wave manager is not active.");
            return true;
        }
        if (!waveManager.isWaveInProgress()) {
            sender.sendMessage(ChatColor.YELLOW + "No wave is currently in progress to skip.");
            return true;
        }
        waveManager.skipCurrentWave(sender);
        return true;
    }
}
