// java
package com.yourfault.system.GeneralPlayer;

import com.mojang.authlib.GameProfile;
import com.yourfault.Main;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import java.util.Collections;
import java.util.UUID;

public class TabManager {
    public final GamePlayer player;

    public TabManager(GamePlayer player) {
        this.player = player;
    }

    public void setHeaderFooter(String header, String footer) {
        if (player == null) return;
        net.kyori.adventure.text.Component h = net.kyori.adventure.text.Component.text(header).color(net.kyori.adventure.text.format.NamedTextColor.WHITE);
        net.kyori.adventure.text.Component f = net.kyori.adventure.text.Component.text(footer).color(net.kyori.adventure.text.format.NamedTextColor.GRAY);
        player.MINECRAFT_PLAYER.sendPlayerListHeaderAndFooter(h, f);
    }
    public void sendFakePlayer()
    {
        GameProfile profile = new GameProfile(UUID.randomUUID(),"HEHE test");
        ServerPlayer serverPlayer = ((CraftPlayer)player.MINECRAFT_PLAYER).getHandle();
        ServerPlayer npc = new ServerPlayer(serverPlayer.level().getServer(),serverPlayer.level(), profile, ClientInformation.createDefault());

        npc.connection = new ServerGamePacketListenerImpl(serverPlayer.level().getServer(), new Connection(PacketFlow.SERVERBOUND),npc, CommonListenerCookie.createInitial(profile, false));

        player.sendPacket(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collections.singleton(npc)));
    }


}
