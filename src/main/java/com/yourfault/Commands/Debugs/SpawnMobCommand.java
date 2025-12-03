package com.yourfault.Commands.Debugs;

import com.yourfault.system.Game;
import com.yourfault.Enemy.mob.WaveEnemyInstance;
import com.yourfault.Enemy.mob.WaveEnemyType;
import com.yourfault.wave.WaveManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

public class SpawnMobCommand implements CommandExecutor {
    private static final Map<String, WaveEnemyType> ALIASES = Map.of(
            "laser_zombie", WaveEnemyType.LASER_ZOMBIE,
            "lasr_zombie", WaveEnemyType.LASER_ZOMBIE,
            "grunt", WaveEnemyType.GRUNT,
            "archer", WaveEnemyType.ARCHER,
            "brute", WaveEnemyType.BRUTE,
            "mage", WaveEnemyType.MAGE,
            "boss", WaveEnemyType.BOSS
    );

    private final Game game;

    public SpawnMobCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        WaveManager manager = game.getWaveManager();
        if (manager == null) {
            sender.sendMessage(ChatColor.RED + "Wave manager not ready.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <enemy_type> [amount]");
            return true;
        }
        WaveEnemyType type = resolveType(args[0]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown enemy type '" + args[0] + "'.");
            sender.sendMessage(ChatColor.YELLOW + "Valid types: " + listTypes());
            return true;
        }
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                return true;
            }
        }
        Location origin = player.getLocation();
        int spawned = 0;
        for (int i = 0; i < amount; i++) {
            Location spawn = origin.clone().add((i % 3) - 1, 0, (i / 3));
            WaveEnemyInstance instance = manager.spawnEnemyAt(spawn, type, false);
            if (instance != null) {
                spawned++;
            }
        }
        sender.sendMessage(ChatColor.GREEN + "Spawned " + spawned + " " + type.displayName() + (spawned == 1 ? "" : "s") + ".");
        if (!manager.isActive()) {
            sender.sendMessage(ChatColor.YELLOW + "(Wave manager inactive: custom damage may not process until a session is started.)");
        }
        return true;
    }

    private WaveEnemyType resolveType(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String normalized = token.toLowerCase(Locale.ROOT);
        if (ALIASES.containsKey(normalized)) {
            return ALIASES.get(normalized);
        }
        normalized = normalized.replace('-', '_');
        try {
            return WaveEnemyType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String listTypes() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (WaveEnemyType type : WaveEnemyType.values()) {
            if (!first) {
                builder.append(ChatColor.GRAY).append(", ");
            }
            builder.append(ChatColor.AQUA).append(type.name().toLowerCase(Locale.ROOT));
            first = false;
        }
        return builder.toString();
    }
}

