package com.yourfault.system;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;

public class TabInfo {
    private final String BLACK_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTY4MzM0NTk1ODU0NSwKICAicHJvZmlsZUlkIiA6ICIzYjgwOTg1YWU4ODY0ZWZlYjA3ODg2MmZkOTRhMTVkOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJLaWVyYW5fVmF4aWxpYW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTgwZmMwNDNlYTgwMDNjOTBjYTEzOTUzYTAzNTY3NjAxOTk2YTE3NDMyMzgyMWY2Y2QwOGRjZDQ1MDdiY2VlMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
    private final String BLACK_SIGNATURE = "ml7x6W9cRrzxGHe6y4K2UQ5vm/houO9E+rk5mI8YauRNepcoi58dC91Pp8yQwhAMR53lT0gPHes1rNZunUtviql27zaieHmkHw0hkasaiepiX9TvnW93dGYgUiNqE7k05wd2uRmYyPQNONGCurQ98+1JKJBGhq9NR8n5p6D4W/YhHkjoIM9DBa+ENJroJRyvZ0IfQDwToAsZc4+aunIIm8nxmwZt1ujA6L6DNjbfaHF0JF7Pb+rDPbjqylQ2uSvneLlkouPzoWgF+rB6WbwSj1yburF+Ii1nzZNZUYujYRnUtgDViXyiFf33ThTmhpoD2l2kVCsu6whAeFyQauLaNMsCj/hqSZmAm6pWKjhVnPAuJfSjY64uwrtOwLtbuGHFOFgA5Gf7q98d50M2Fi+zYfkEjAUW9HAVvBIkAE88LQZU4rte4xS5mr4CnqrZVZ1VOzz1BR0DjitmJTzOLkgUpIeKj/6jAvyA/q7RyHJ6hiGlV3OUyOtFNWgmQ9Zefd35SPik0DTeBaNzo6RmdpamlzDqjJzfnAtfwHZrTs02e/NRgeCqKmkDyVvkoop8u/AqztD+cnhTIg47R1DX4nkMxRKt5lFMVJS50DcNogLQitIvC/8ziZa+Q+S+9xocFLP9hRFj7savi1ntfhGxL24LIDji0ptsAgF3bdDVLyz/+iE=";
    private final Property textureProp = new Property("textures", BLACK_VALUE, BLACK_SIGNATURE);

    public enum TabType {
        PLAYERLIST_TOP("00_PLAYERLIST_TOP"),
        PLAYERLIST_ALIVE("01_PLAYERLIST_ALIVE"),
        PLAYERLIST_DOWN("02_PLAYERLIST_DOWN"),
        PLAYERLIST_DEAD("03_PLAYERLIST_DEAD"),
        PLAYERLIST_PLACEHOLDER("04_PLAYERLIST_PLACEHOLDER"),
        PERKLIST_TOP("10_PERKLIST_TOP"),
        PERKLIST("11_PERKLIST"),
        WAVEINFO_TOP("20_WAVEINFO_TOP"),
        WAVEINFO_CURRENTWAVE("21_WAVEINFO_CURRENTWAVE"),
        WAVEINFO("22_WAVEINFO");

        private final String teamName;

        TabType(String teamName) {
            this.teamName = teamName;
        }

        public String getTeamName() {
            return teamName;
        }



        // optional: reverse lookup
        public static TabType fromTeamName(String name) {
            for (TabType t : values()) {
                if (t.teamName.equals(name)) return t;
            }
            return null; // or throw IllegalArgumentException
        }
    }
    public HashMap<TabType, Team> GetTeam = new HashMap<>();
    public static int TAB_HEIGHT = 14;
    public TabInfo()
    {
        InitOrderTeam();
    }
    public PropertyMap getBlackPM()
    {
        Multimap<String,Property> multimap = ArrayListMultimap.create();
        multimap.put("textures", textureProp);
        return new PropertyMap(multimap);
    }
    public void InitOrderTeam()
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (TabType t : TabType.values()) {
            Team team = board.getTeam(t.teamName);
            if (team == null) {
                team = board.registerNewTeam(t.teamName);
            }
            GetTeam.put(t,team);
        }
        GetTeam.get(TabType.PLAYERLIST_ALIVE).color(NamedTextColor.GREEN);
        GetTeam.get(TabType.PLAYERLIST_DOWN).color(NamedTextColor.RED);
        GetTeam.get(TabType.PLAYERLIST_DOWN).suffix(Component.text(" DOWN").color(NamedTextColor.RED));
        GetTeam.get(TabType.PLAYERLIST_DEAD).color(NamedTextColor.DARK_RED);
        GetTeam.get(TabType.PLAYERLIST_DEAD).suffix(Component.text(" DEAD").color(NamedTextColor.DARK_RED));
        GetTeam.get(TabType.PLAYERLIST_PLACEHOLDER).addEntry("　　　　　　　　　　　　　　　　");
        GetTeam.get(TabType.PERKLIST).addEntry("　　　　　　　　　　　　　　　");
        GetTeam.get(TabType.WAVEINFO_CURRENTWAVE).suffix(Component.text(": 0"));
        GetTeam.get(TabType.WAVEINFO_CURRENTWAVE).addEntry("Current Wave");
        GetTeam.get(TabType.WAVEINFO).addEntry("　　　　　　　　　　　　　");

    }
}
