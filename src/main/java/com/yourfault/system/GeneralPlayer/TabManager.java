// java
package com.yourfault.system.GeneralPlayer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.yourfault.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.Connection;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Collections;
import java.util.UUID;

public class TabManager {
    public final GamePlayer player;
    private static final String BLACK_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTY4MzM0NTk1ODU0NSwKICAicHJvZmlsZUlkIiA6ICIzYjgwOTg1YWU4ODY0ZWZlYjA3ODg2MmZkOTRhMTVkOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJLaWVyYW5fVmF4aWxpYW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTgwZmMwNDNlYTgwMDNjOTBjYTEzOTUzYTAzNTY3NjAxOTk2YTE3NDMyMzgyMWY2Y2QwOGRjZDQ1MDdiY2VlMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
    private static final String BLACK_SIGNATURE = "ml7x6W9cRrzxGHe6y4K2UQ5vm/houO9E+rk5mI8YauRNepcoi58dC91Pp8yQwhAMR53lT0gPHes1rNZunUtviql27zaieHmkHw0hkasaiepiX9TvnW93dGYgUiNqE7k05wd2uRmYyPQNONGCurQ98+1JKJBGhq9NR8n5p6D4W/YhHkjoIM9DBa+ENJroJRyvZ0IfQDwToAsZc4+aunIIm8nxmwZt1ujA6L6DNjbfaHF0JF7Pb+rDPbjqylQ2uSvneLlkouPzoWgF+rB6WbwSj1yburF+Ii1nzZNZUYujYRnUtgDViXyiFf33ThTmhpoD2l2kVCsu6whAeFyQauLaNMsCj/hqSZmAm6pWKjhVnPAuJfSjY64uwrtOwLtbuGHFOFgA5Gf7q98d50M2Fi+zYfkEjAUW9HAVvBIkAE88LQZU4rte4xS5mr4CnqrZVZ1VOzz1BR0DjitmJTzOLkgUpIeKj/6jAvyA/q7RyHJ6hiGlV3OUyOtFNWgmQ9Zefd35SPik0DTeBaNzo6RmdpamlzDqjJzfnAtfwHZrTs02e/NRgeCqKmkDyVvkoop8u/AqztD+cnhTIg47R1DX4nkMxRKt5lFMVJS50DcNogLQitIvC/8ziZa+Q+S+9xocFLP9hRFj7savi1ntfhGxL24LIDji0ptsAgF3bdDVLyz/+iE=";

    static final Property textureProp = new Property("textures", BLACK_VALUE, BLACK_SIGNATURE);
    static Multimap<String, Property> multimap = ArrayListMultimap.create();
    static final PropertyMap pm = new PropertyMap(multimap);
    static {
        multimap.put("textures", textureProp);
        InitOrderTeam();

    }



    private static final String[] ORDER_TEAMS = {
            "00_PLAYERLIST_TOP",
            "01_PLAYERLIST_ALIVE",
            "02_PLAYERLIST_DOWN",
            "03_PLAYERLIST_DEAD",
            "10_PERKLIST_TOP",
            "11_PERKLIST",
            "20_WAVEINFO_TOP",
            "21_WAVEINFO"
    };

    public TabManager(GamePlayer player) {
        this.player = player;

    }
    public static void InitOrderTeam()
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
    public void initTab() {
        setHeaderFooter("Labyrinth Of Tome","Minecraft No.1 Roguelike PVE");

    }
    private void FILL_PLAYER_LIST()
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        sendFakePlayer("Players");

        //TODO board.getTeam("01_PLAYERLIST_TOP").addEntry();

    }
    public void setHeaderFooter(String header, String footer) {
        if (player == null) return;
        Component h = Component.text(header).color(NamedTextColor.WHITE);
        Component f = Component.text(footer).color(NamedTextColor.GRAY);
        player.MINECRAFT_PLAYER.sendPlayerListHeaderAndFooter(h, f);
        for (int i = 0; i < 14*4 - Main.game.PLAYER_LIST.size(); i++) {
            sendBlankSkinFakePlayer();
        }
    }

    public void sendFakePlayer(String name) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), name,pm);

        ServerPlayer serverPlayer = ((CraftPlayer) player.MINECRAFT_PLAYER).getHandle();
        ServerPlayer npc = new ServerPlayer(serverPlayer.level().getServer(), serverPlayer.level(), profile, ClientInformation.createDefault());

        npc.connection = new ServerGamePacketListenerImpl(
                serverPlayer.level().getServer(),
                new Connection(PacketFlow.SERVERBOUND),
                npc,
                CommonListenerCookie.createInitial(profile, false)
        );

        player.sendPacket(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collections.singleton(npc)));
    }

    // Pre-baked black (blank) skin property value and signature

    public void sendBlankSkinFakePlayer() {
        // create profile

        // create property and a Multimap + PropertyMap containing it

        GameProfile profile = new GameProfile(UUID.randomUUID(), "　　　　　　　　　　　　　　　　",pm);




        // spawn fake player NPC using profile
        ServerPlayer serverPlayer = ((CraftPlayer) player.MINECRAFT_PLAYER).getHandle();
        ServerPlayer npc = new ServerPlayer(serverPlayer.level().getServer(), serverPlayer.level(), profile, ClientInformation.createDefault());

        npc.connection = new ServerGamePacketListenerImpl(
                serverPlayer.level().getServer(),
                new Connection(PacketFlow.SERVERBOUND),
                npc,
                CommonListenerCookie.createInitial(profile, false)
        );

        player.sendPacket(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,npc));
    }
}
