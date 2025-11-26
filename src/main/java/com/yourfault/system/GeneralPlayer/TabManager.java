// java
package com.yourfault.system.GeneralPlayer;

import com.mojang.authlib.GameProfile;
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

import static com.yourfault.Main.tabInfo;

public class TabManager {
    public final GamePlayer player;
    public TabManager(GamePlayer player) {
        this.player = player;

    }

    public void initTab() {
        setHeaderFooter("Labyrinth Of Tome","Minecraft No.1 Roguelike PVE");
        FILL_PLAYER_LIST();

    }
    private void FILL_PLAYER_LIST()
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        board.getTeam("00_PLAYERLIST_TOP").addEntry(sendFakePlayer("Players"));
        for (GamePlayer gp : Main.game.PLAYER_LIST.values()) {
            if (gp.CurrentState.equals(GamePlayer.SurvivalState.ALIVE)) {
                board.getTeam("01_PLAYERLIST_ALIVE").addEntry(gp.MINECRAFT_PLAYER.getName());
            } else if (gp.CurrentState.equals(GamePlayer.SurvivalState.DOWNED)) {
                board.getTeam("02_PLAYERLIST_DOWN").addEntry(gp.MINECRAFT_PLAYER.getName());
            } else {
                board.getTeam("03_PLAYERLIST_DEAD").addEntry(gp.MINECRAFT_PLAYER.getName());
            }
        }
        for (int i = 0; i < 14 - Main.game.PLAYER_LIST.size(); i++) {
            board.getTeam("04_PLAYERLIST_PLACEHOLDER").addEntry(sendFakePlayer());

        }

    }
    public void setHeaderFooter(String header, String footer) {
        if (player == null) return;
        Component h = Component.text(header).color(NamedTextColor.WHITE);
        Component f = Component.text(footer).color(NamedTextColor.GRAY);
        player.MINECRAFT_PLAYER.sendPlayerListHeaderAndFooter(h, f);

    }

    public String sendFakePlayer(String name,PropertyMap pm) {
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
        return name;
    }

    // Pre-baked black (blank) skin property value and signature

    public String sendFakePlayer() {

        return sendFakePlayer("　　　　　　　　　　　　　　　　", tabInfo.pm);
    }
    public String sendFakePlayer(String name) {

        return sendFakePlayer(name, tabInfo.pm);
    }
}
