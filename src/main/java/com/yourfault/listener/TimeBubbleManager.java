package com.yourfault.listener;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles time distortion bubbles that alter player speed and outgoing damage.
 */
public final class TimeBubbleManager {
    private static final Map<UUID, TimeBubble> BUBBLES = new HashMap<>();
    private static final Map<UUID, ZoneState> PLAYER_STATES = new HashMap<>();
    private static final Map<UUID, Float> BASE_DAMAGE = new HashMap<>();
    private static final long TICK_INTERVAL = 5L;
    private static BukkitTask ticker;

    private TimeBubbleManager() {
    }

    public static UUID registerBubble(UUID ownerId, Location center, double radius, long durationTicks) {
        if (center == null || center.getWorld() == null) {
            return null;
        }
        Location clone = center.clone();
        clone.setY(Math.max(center.getWorld().getMinHeight() + 1, clone.getY()));
        TimeBubble bubble = new TimeBubble(ownerId, clone, radius, durationTicks);
        BUBBLES.put(bubble.id, bubble);
        ensureTicker();
        return bubble.id;
    }

    public static void removeBubblesOwnedBy(UUID ownerId) {
        if (ownerId == null) {
            return;
        }
        List<UUID> expired = new ArrayList<>();
        for (TimeBubble bubble : BUBBLES.values()) {
            if (ownerId.equals(bubble.ownerId)) {
                expired.add(bubble.id);
            }
        }
        expired.forEach(id -> BUBBLES.remove(id));
        if (BUBBLES.isEmpty()) {
            resetAllPlayers();
            stopTicker();
        }
    }

    public static int getOwnedBubbleCount(UUID ownerId) {
        if (ownerId == null) {
            return 0;
        }
        int count = 0;
        for (TimeBubble bubble : BUBBLES.values()) {
            if (ownerId.equals(bubble.ownerId)) {
                count++;
            }
        }
        return count;
    }

    private static void ensureTicker() {
        if (ticker != null) {
            return;
        }
        ticker = Bukkit.getScheduler().runTaskTimer(Main.plugin, TimeBubbleManager::tick, 0L, TICK_INTERVAL);
    }

    private static void stopTicker() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    private static void tick() {
        if (BUBBLES.isEmpty()) {
            resetAllPlayers();
            stopTicker();
            return;
        }
        Iterator<TimeBubble> iterator = BUBBLES.values().iterator();
        while (iterator.hasNext()) {
            TimeBubble bubble = iterator.next();
            bubble.remainingTicks -= TICK_INTERVAL;
            if (bubble.remainingTicks <= 0 || bubble.center.getWorld() == null) {
                iterator.remove();
                continue;
            }
            drawBubble(bubble);
        }
        if (BUBBLES.isEmpty()) {
            resetAllPlayers();
            stopTicker();
            return;
        }
        updatePlayerStates();
    }

    private static void drawBubble(TimeBubble bubble) {
        World world = bubble.center.getWorld();
        if (world == null) {
            return;
        }
        double radius = Math.sqrt(bubble.radiusSq);
        Location base = bubble.center.clone().add(0, 0.2, 0);
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 10) {
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            world.spawnParticle(Particle.PORTAL, base.getX() + x, base.getY(), base.getZ() + z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private static void updatePlayerStates() {
        boolean hasBubbles = !BUBBLES.isEmpty();
        for (GamePlayer player : Main.game.PLAYER_LIST.values()) {
            Player bukkit = player.getMinecraftPlayer();
            if (bukkit == null) {
                clearState(player);
                continue;
            }
            ZoneState newState = resolveState(player, hasBubbles);
            applyState(player, newState);
        }
        if (!hasBubbles) {
            resetAllPlayers();
        }
    }

    private static ZoneState resolveState(GamePlayer player, boolean hasBubbles) {
        if (!hasBubbles) {
            return ZoneState.NEUTRAL;
        }
        Player bukkit = player.getMinecraftPlayer();
        if (bukkit == null) {
            return ZoneState.NEUTRAL;
        }
        Location loc = bukkit.getLocation();
        for (TimeBubble bubble : BUBBLES.values()) {
            if (!Objects.equals(bubble.center.getWorld(), loc.getWorld())) {
                continue;
            }
            if (loc.distanceSquared(bubble.center) <= bubble.radiusSq) {
                return ZoneState.INSIDE;
            }
        }
        return ZoneState.OUTSIDE;
    }

    private static void applyState(GamePlayer player, ZoneState newState) {
        Player bukkit = player.getMinecraftPlayer();
        if (bukkit == null) {
            clearState(player);
            return;
        }
        UUID playerId = bukkit.getUniqueId();
        ZoneState current = PLAYER_STATES.getOrDefault(playerId, ZoneState.NEUTRAL);
        if (current == newState) {
            refreshEffects(player, newState);
            return;
        }
        if (newState == ZoneState.NEUTRAL) {
            restoreDamage(player, playerId);
            PLAYER_STATES.remove(playerId);
            return;
        }
        BASE_DAMAGE.putIfAbsent(playerId, player.damageMultiplier);
        switch (newState) {
            case INSIDE -> player.damageMultiplier = 3.0f;
            case OUTSIDE -> player.damageMultiplier = 0.25f;
            default -> {
            }
        }
        PLAYER_STATES.put(playerId, newState);
        refreshEffects(player, newState);
        if (newState == ZoneState.INSIDE) {
            bukkit.sendActionBar(ChatColor.AQUA + "Time compresses around you!");
        } else if (newState == ZoneState.OUTSIDE) {
            bukkit.sendActionBar(ChatColor.LIGHT_PURPLE + "Time speeds up outside the bubble.");
        }
    }

    private static void refreshEffects(GamePlayer player, ZoneState state) {
        Player bukkit = player.getMinecraftPlayer();
        if (bukkit == null) {
            return;
        }
        PotionEffect slow = new PotionEffect(PotionEffectType.SLOWNESS, (int) (TICK_INTERVAL * 3), 4, true, false, false);
        PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, (int) (TICK_INTERVAL * 3), 3, true, false, false);
        switch (state) {
            case INSIDE -> bukkit.addPotionEffect(slow);
            case OUTSIDE -> bukkit.addPotionEffect(speed);
            default -> {
            }
        }
    }

    private static void clearState(GamePlayer player) {
        Player bukkit = player.getMinecraftPlayer();
        if (bukkit == null) {
            return;
        }
        UUID id = bukkit.getUniqueId();
        if (PLAYER_STATES.remove(id) != null) {
            restoreDamage(player, id);
        }
    }

    private static void restoreDamage(GamePlayer player, UUID id) {
        Float base = BASE_DAMAGE.remove(id);
        player.damageMultiplier = base != null ? base : 1.0f;
    }

    private static void resetAllPlayers() {
        for (UUID id : new ArrayList<>(PLAYER_STATES.keySet())) {
            GamePlayer player = Main.game.PLAYER_LIST.get(id);
            if (player != null) {
                restoreDamage(player, id);
            }
        }
        PLAYER_STATES.clear();
        BASE_DAMAGE.clear();
    }

    private enum ZoneState {
        NEUTRAL,
        INSIDE,
        OUTSIDE
    }

    private static final class TimeBubble {
        private final UUID id = UUID.randomUUID();
        private final UUID ownerId;
        private final Location center;
        private final double radiusSq;
        private long remainingTicks;

        private TimeBubble(UUID ownerId, Location center, double radius, long durationTicks) {
            this.ownerId = ownerId;
            this.center = center;
            this.radiusSq = Math.max(4, radius * radius);
            this.remainingTicks = Math.max(40L, durationTicks);
        }
    }
}
