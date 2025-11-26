package com.yourfault.system;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;

public class TabInfo {
    private final String BLACK_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTY4MzM0NTk1ODU0NSwKICAicHJvZmlsZUlkIiA6ICIzYjgwOTg1YWU4ODY0ZWZlYjA3ODg2MmZkOTRhMTVkOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJLaWVyYW5fVmF4aWxpYW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTgwZmMwNDNlYTgwMDNjOTBjYTEzOTUzYTAzNTY3NjAxOTk2YTE3NDMyMzgyMWY2Y2QwOGRjZDQ1MDdiY2VlMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
    private final String BLACK_SIGNATURE = "ml7x6W9cRrzxGHe6y4K2UQ5vm/houO9E+rk5mI8YauRNepcoi58dC91Pp8yQwhAMR53lT0gPHes1rNZunUtviql27zaieHmkHw0hkasaiepiX9TvnW93dGYgUiNqE7k05wd2uRmYyPQNONGCurQ98+1JKJBGhq9NR8n5p6D4W/YhHkjoIM9DBa+ENJroJRyvZ0IfQDwToAsZc4+aunIIm8nxmwZt1ujA6L6DNjbfaHF0JF7Pb+rDPbjqylQ2uSvneLlkouPzoWgF+rB6WbwSj1yburF+Ii1nzZNZUYujYRnUtgDViXyiFf33ThTmhpoD2l2kVCsu6whAeFyQauLaNMsCj/hqSZmAm6pWKjhVnPAuJfSjY64uwrtOwLtbuGHFOFgA5Gf7q98d50M2Fi+zYfkEjAUW9HAVvBIkAE88LQZU4rte4xS5mr4CnqrZVZ1VOzz1BR0DjitmJTzOLkgUpIeKj/6jAvyA/q7RyHJ6hiGlV3OUyOtFNWgmQ9Zefd35SPik0DTeBaNzo6RmdpamlzDqjJzfnAtfwHZrTs02e/NRgeCqKmkDyVvkoop8u/AqztD+cnhTIg47R1DX4nkMxRKt5lFMVJS50DcNogLQitIvC/8ziZa+Q+S+9xocFLP9hRFj7savi1ntfhGxL24LIDji0ptsAgF3bdDVLyz/+iE=";
    private final Property textureProp = new Property("textures", BLACK_VALUE, BLACK_SIGNATURE);
    private Multimap<String, Property> multimap = ArrayListMultimap.create();

    public final PropertyMap pm = new PropertyMap(multimap);
    public final String[] ORDER_TEAMS = {
            "00_PLAYERLIST_TOP",
            "01_PLAYERLIST_ALIVE",
            "02_PLAYERLIST_DOWN",
            "03_PLAYERLIST_DEAD",
            "04_PLAYERLIST_PLACEHOLDER",
            "10_PERKLIST_TOP",
            "11_PERKLIST",
            "20_WAVEINFO_TOP",
            "21_WAVEINFO"
    };
    public TabInfo()
    {
        multimap.put("textures", textureProp);
        InitOrderTeam();
    }
    public void InitOrderTeam()
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (String teamName : ORDER_TEAMS) {
            if (board.getTeam(teamName) == null) {
                board.registerNewTeam(teamName);
            }
        }
        board.getTeam("01_PLAYERLIST_ALIVE").color(NamedTextColor.GREEN);
        board.getTeam("02_PLAYERLIST_DOWN").color(NamedTextColor.RED);
        board.getTeam("02_PLAYERLIST_DOWN").suffix(Component.text(" DOWN").color(NamedTextColor.RED));
        board.getTeam("03_PLAYERLIST_DEAD").color(NamedTextColor.DARK_RED);
        board.getTeam("03_PLAYERLIST_DEAD").suffix(Component.text(" DEAD").color(NamedTextColor.DARK_RED));

    }
}
