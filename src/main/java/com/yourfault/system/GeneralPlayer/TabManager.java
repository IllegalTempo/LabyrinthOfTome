// java
package com.yourfault.system.GeneralPlayer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.yourfault.Main;
import com.yourfault.perks.PerkObject;
import com.yourfault.system.TabInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

import static com.yourfault.Main.tabInfo;
import static com.yourfault.system.TabInfo.TAB_HEIGHT;

public class TabManager {
    public final GamePlayer player;
    public List<UUID> AllFakePlayer = new ArrayList<>();

    public TabManager(GamePlayer player) {
        this.player = player;
        initTab();

    }

    // track fake perk players: UUID -> name
    private final Map<UUID, String> TAB_PERKLIST = new LinkedHashMap<>();
    private final List<UUID> PLAYERLIST_PLACEHOLDERS = new ArrayList<>();
    public void initTab() {
        setHeaderFooter("Labyrinth Of Tome","Minecraft No.1 Roguelike PVE");
        FILL_PLAYER_LIST();
        InitPerkTabDisplay();
        updatePerkTabDisplay();
        BuildWaveInfoTabDisplay();

    }
    public void playerlist_addPlaceholder()
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        UUID placeholderUuid = sendFakePlayer();
        PLAYERLIST_PLACEHOLDERS.add(placeholderUuid);


    }
    public void playerlist_removePlaceholder()
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        if (PLAYERLIST_PLACEHOLDERS.isEmpty()) return;
        UUID placeholderuuid = PLAYERLIST_PLACEHOLDERS.removeLast();
        removeFakePlayer(placeholderuuid);


    }
    private void BuildWaveInfoTabDisplay()
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        sendFakePlayer("Game Info");
        Team waveTop = Main.tabInfo.GetTeam.get(TabInfo.TabType.WAVEINFO_TOP);
        sendFakePlayer("Current Wave");
        if (waveTop != null) waveTop.addEntry("Game Info");
        for(int i = 0 ; i < TAB_HEIGHT-2;i++)
        {
            sendFakePlayer("　　　　　　　　　　　　　");

        }
    }
    private void FILL_PLAYER_LIST()
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        // create a fake "Players" entry and add it to the top team
        sendFakePlayer("Players");
        Team topTeam = Main.tabInfo.GetTeam.get(TabInfo.TabType.PLAYERLIST_TOP);
        if (topTeam != null) topTeam.addEntry("Players");

        for (GamePlayer gp : Main.game.PLAYER_LIST.values()) {
            if (gp.CurrentState.equals(GamePlayer.SurvivalState.ALIVE)) {
                Team alive = Main.tabInfo.GetTeam.get(TabInfo.TabType.PLAYERLIST_ALIVE);
                if (alive != null) alive.addEntry(gp.MINECRAFT_PLAYER.getName());
            } else if (gp.CurrentState.equals(GamePlayer.SurvivalState.DOWNED)) {
                Team down = Main.tabInfo.GetTeam.get(TabInfo.TabType.PLAYERLIST_DOWN);
                if (down != null) down.addEntry(gp.MINECRAFT_PLAYER.getName());
            } else {
                Team dead = Main.tabInfo.GetTeam.get(TabInfo.TabType.PLAYERLIST_DEAD);
                if (dead != null) dead.addEntry(gp.MINECRAFT_PLAYER.getName());
            }
        }
        // add a single placeholder entry string to the placeholder team

        // send blank fake players (these don't add scoreboard entries here)
        for (int i = 0; i < TAB_HEIGHT - Main.game.PLAYER_LIST.size(); i++) {
            playerlist_addPlaceholder();

        }

    }
    public void removeFakePlayer(UUID uuid)
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        // send player remove packet (packet expects a List)
        player.sendPacket(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(uuid)));
        // remove the scoreboard entry(s) that match the name


    }
    private void InitPerkTabDisplay()
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        sendFakePlayer("Perks");
        Team perkTop = Main.tabInfo.GetTeam.get(TabInfo.TabType.PERKLIST_TOP);
        if (perkTop != null) perkTop.addEntry("Perks");
    }

    public void updatePerkTabDisplay()
    {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        // remove existing perk fake players from the client and scoreboard
        if (!TAB_PERKLIST.isEmpty()) {
            Set<UUID> keys = new HashSet<>(TAB_PERKLIST.keySet());
            for (UUID e : keys) {
                removeFakePlayer(e);

            }
            TAB_PERKLIST.clear();
        }

        // create new Perks fake player and add it to the team


        // (additional perk list population would go here)
        for(PerkObject p:player.PLAYER_PERKS.perks)
        {
            UUID perkUuid = sendFakePlayer(p.perkType.displayName);
            Team perkTeam = Main.tabInfo.GetTeam.get(TabInfo.TabType.PERKLIST);
            if (perkTeam != null) perkTeam.addEntry(p.perkType.displayName);
            TAB_PERKLIST.put(perkUuid, p.perkType.displayName);
        }
        for(int i = 0; i < TAB_HEIGHT - player.PLAYER_PERKS.perks.size();i++)
        {
            UUID perkUuid = sendFakePlayer("　　　　　　　　　　　　　　　");
            TAB_PERKLIST.put(perkUuid, "　　　　　　　　　　　　　　　");
        }

    }

    public void setHeaderFooter(String header, String footer) {
        if (player == null) return;
        Component h = Component.text(header).color(NamedTextColor.WHITE);
        Component f = Component.text(footer).color(NamedTextColor.GRAY);
        player.MINECRAFT_PLAYER.sendPlayerListHeaderAndFooter(h, f);

    }
    public void removeAllFakePlayer()
    {
        for (UUID uuid : AllFakePlayer) {
            removeFakePlayer(uuid);
        }
        AllFakePlayer.clear();
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
        AllFakePlayer.add(npc.getUUID());

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
