// java
package com.yourfault.system.GeneralPlayer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.yourfault.Main;
import com.yourfault.perks.PerkObject;
import com.yourfault.perks.PerkType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
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
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.yourfault.Main.tabInfo;

public class TabManager {
    public final GamePlayer player;

    public TabManager(GamePlayer player) {
        this.player = player;

    }

    // track fake perk players: UUID -> name
    private final Map<UUID, String> TAB_PERKLIST = new LinkedHashMap<>();

    public void initTab() {
        setHeaderFooter("Labyrinth Of Tome","Minecraft No.1 Roguelike PVE");
        FILL_PLAYER_LIST();

    }

    private void FILL_PLAYER_LIST()
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        // create a fake "Players" entry and add it to the top team
        sendFakePlayer("Players");
        Team topTeam = board.getTeam("00_PLAYERLIST_TOP");
        if (topTeam != null) topTeam.addEntry("Players");

        for (GamePlayer gp : Main.game.PLAYER_LIST.values()) {
            if (gp.CurrentState.equals(GamePlayer.SurvivalState.ALIVE)) {
                Team alive = board.getTeam("01_PLAYERLIST_ALIVE");
                if (alive != null) alive.addEntry(gp.MINECRAFT_PLAYER.getName());
            } else if (gp.CurrentState.equals(GamePlayer.SurvivalState.DOWNED)) {
                Team down = board.getTeam("02_PLAYERLIST_DOWN");
                if (down != null) down.addEntry(gp.MINECRAFT_PLAYER.getName());
            } else {
                Team dead = board.getTeam("03_PLAYERLIST_DEAD");
                if (dead != null) dead.addEntry(gp.MINECRAFT_PLAYER.getName());
            }
        }
        // add a single placeholder entry string to the placeholder team
        Team placeholder = board.getTeam("04_PLAYERLIST_PLACEHOLDER");
        if (placeholder != null) placeholder.addEntry("　　　　　　　　　　　　　　　　");

        // send blank fake players (these don't add scoreboard entries here)
        for (int i = 0; i < 14 - Main.game.PLAYER_LIST.size(); i++) {
            sendFakePlayer();

        }

    }

    private void updatePerkTabDisplay()
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        // remove existing perk fake players from the client and scoreboard
        if (!TAB_PERKLIST.isEmpty()) {
            for (Map.Entry<UUID, String> e : TAB_PERKLIST.entrySet()) {
                UUID uuid = e.getKey();
                String name = e.getValue();
                // send player remove packet (packet expects a List)
                player.sendPacket(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(uuid)));
                // remove the scoreboard entry(s) that match the name
                for (Team team : board.getTeams()) {
                    if (team != null && team.hasEntry(name)) team.removeEntry(name);
                }
            }
            TAB_PERKLIST.clear();
        }

        // create new Perks fake player and add it to the team
        UUID perksId = sendFakePlayer("Perks");
        Team perkTop = board.getTeam("10_PERKLIST_TOP");
        if (perkTop != null) perkTop.addEntry("Perks");

        // (additional perk list population would go here)
        for(PerkObject p:player.PLAYER_PERKS.perks)
        {
            //todo show perk list in tab
        }

    }

    public void setHeaderFooter(String header, String footer) {
        if (player == null) return;
        Component h = Component.text(header).color(NamedTextColor.WHITE);
        Component f = Component.text(footer).color(NamedTextColor.GRAY);
        player.MINECRAFT_PLAYER.sendPlayerListHeaderAndFooter(h, f);

    }

    public UUID sendFakePlayer(String name,PropertyMap pm) {
        // prefer provided PropertyMap; if it doesn't contain textures, fall back to the black PM
        PropertyMap effective = pm;
        if (effective == null || effective.get("textures").isEmpty()) {
            effective = tabInfo.getBlackPM();
        }

        GameProfile profile = new GameProfile(UUID.randomUUID(), name, effective);

        ServerPlayer serverPlayer = ((CraftPlayer) player.MINECRAFT_PLAYER).getHandle();
        ServerPlayer npc = new ServerPlayer(serverPlayer.level().getServer(), serverPlayer.level(), profile, ClientInformation.createDefault());

        npc.connection = new ServerGamePacketListenerImpl(
                serverPlayer.level().getServer(),
                new Connection(PacketFlow.SERVERBOUND),
                npc,
                CommonListenerCookie.createInitial(profile, false)
        );

        player.sendPacket(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collections.singleton(npc)));
        return profile.id();
    }

    // Pre-baked black (blank) skin property value and signature

    public UUID sendFakePlayer() {

        return sendFakePlayer("　　　　　　　　　　　　　　　　", tabInfo.getBlackPM());
    }
    public UUID sendFakePlayer(String name) {

        return sendFakePlayer(name, tabInfo.getBlackPM());
    }
}
