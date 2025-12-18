package com.yourfault.listener;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages temporary iron-chain links that bind multiple players together.
 */
public final class ChainLinkManager {
    private static final Map<UUID, ChainLink> LINK_BY_ID = new HashMap<>();
    private static final Map<UUID, UUID> PLAYER_TO_LINK = new HashMap<>();
    private static final Map<UUID, UUID> ANCHOR_TO_LINK = new HashMap<>();
    private static final long TICK_INTERVAL = 5L;
    private static final double MAX_DISTANCE = 4.25;
    private static final double PULL_FORCE = 0.18;
    private static final String CHAIN_TAG = "lot_chain_lead";
    private static BukkitTask ticker;

    private ChainLinkManager() {
    }

    public static boolean bindPlayers(UUID ownerId, List<GamePlayer> orderedPlayers, int desiredMembers, int chainHealth, long durationTicks) {
        if (orderedPlayers == null || orderedPlayers.isEmpty()) {
            return false;
        }
        List<GamePlayer> eligible = new ArrayList<>();
        for (GamePlayer player : orderedPlayers) {
            if (eligible.size() >= desiredMembers) {
                break;
            }
            if (player == null || player.getMinecraftPlayer() == null) {
                continue;
            }
            if (player.CurrentState != GamePlayer.SurvivalState.ALIVE) {
                continue;
            }
            eligible.add(player);
        }
        if (eligible.size() < 2) {
            return false;
        }

        for (GamePlayer player : eligible) {
            releasePlayer(player.getMinecraftPlayer().getUniqueId());
        }

        Location anchorLocation = averageLocation(eligible);
        ArmorStand anchor = spawnAnchor(anchorLocation, chainHealth);
        if (anchor == null) {
            return false;
        }
        ChainLink link = new ChainLink(ownerId, anchor, chainHealth, durationTicks);
        for (GamePlayer player : eligible) {
            UUID playerId = player.getMinecraftPlayer().getUniqueId();
            link.members.add(playerId);
            PLAYER_TO_LINK.put(playerId, link.id);
            Player bukkitPlayer = player.getMinecraftPlayer();
            if (bukkitPlayer != null) {
                bukkitPlayer.sendMessage(ChatColor.DARK_RED + "Iron chains bind you to nearby allies! Stay together or break the tether.");
            }
        }
        LINK_BY_ID.put(link.id, link);
        ANCHOR_TO_LINK.put(anchor.getUniqueId(), link.id);
        ensureTicker();
        return true;
    }

    public static void breakLinksOwnedBy(UUID ownerId) {
        if (ownerId == null) {
            return;
        }
        List<ChainLink> owned = new ArrayList<>();
        for (ChainLink link : LINK_BY_ID.values()) {
            if (ownerId.equals(link.ownerId)) {
                owned.add(link);
            }
        }
        owned.forEach(link -> breakLink(link, ChatColor.GRAY + "The iron chains unravel."));
    }

    public static boolean handleSharedDamage(GamePlayer victim, float incomingDamage) {
        if (victim == null || incomingDamage <= 0 || victim.getMinecraftPlayer() == null) {
            return false;
        }
        UUID linkId = PLAYER_TO_LINK.get(victim.getMinecraftPlayer().getUniqueId());
        if (linkId == null) {
            return false;
        }
        ChainLink link = LINK_BY_ID.get(linkId);
        if (link == null || link.members.isEmpty()) {
            PLAYER_TO_LINK.remove(victim.getMinecraftPlayer().getUniqueId());
            return false;
        }
        float splitDamage = incomingDamage / link.members.size();
        for (UUID playerId : new ArrayList<>(link.members)) {
            GamePlayer player = Main.game.PLAYER_LIST.get(playerId);
            if (player != null) {
                //player.ApplyLinkedDamage(splitDamage); before
                player.applyDamage(splitDamage,null,true);//todo check if this is ok
            }
        }
        return true;
    }

    public static boolean handleLeadDamage(LivingEntity entity, float amount, Player damager) {
        if (entity == null) {
            return false;
        }
        UUID linkId = ANCHOR_TO_LINK.get(entity.getUniqueId());
        if (linkId == null) {
            return false;
        }
        ChainLink link = LINK_BY_ID.get(linkId);
        if (link == null) {
            return false;
        }
        link.health -= Math.round(amount);
        updateAnchorName(link);
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.9f, 1.2f);
        if (damager != null) {
            damager.sendActionBar(ChatColor.GOLD + "Chain integrity " + Math.max(0, link.health) + " HP");
        }
        if (link.health <= 0) {
            breakLink(link, ChatColor.GREEN + "The iron chains shatter!");
        }
        return true;
    }

    private static void releasePlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        UUID linkId = PLAYER_TO_LINK.remove(playerId);
        if (linkId == null) {
            return;
        }
        ChainLink link = LINK_BY_ID.get(linkId);
        if (link != null) {
            link.members.remove(playerId);
        }
    }

    private static Location averageLocation(List<GamePlayer> players) {
        double x = 0;
        double y = 0;
        double z = 0;
        World world = null;
        int counted = 0;
        for (GamePlayer player : players) {
            if (player == null || player.getMinecraftPlayer() == null) {
                continue;
            }
            Location loc = player.getMinecraftPlayer().getLocation();
            if (world == null) {
                world = loc.getWorld();
            }
            x += loc.getX();
            y += loc.getY();
            z += loc.getZ();
            counted++;
        }
        if (counted == 0 || world == null) {
            return null;
        }
        return new Location(world, x / counted, y / counted, z / counted);
    }

    private static ArmorStand spawnAnchor(Location location, int chainHealth) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return location.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setInvulnerable(false);
            stand.setCollidable(false);
            stand.getEquipment().setHelmet(new ItemStack(Material.IRON_CHAIN));
            stand.addScoreboardTag(CHAIN_TAG);
            stand.setCustomNameVisible(true);
            stand.setMarker(false);
            stand.setRemoveWhenFarAway(false);
            stand.getEquipment().setHelmet(new ItemStack(Material.IRON_CHAIN));
            stand.setCustomName(ChatColor.GRAY + "Iron Chain " + chainHealth + " HP");
        });
    }

    private static void updateAnchorName(ChainLink link) {
        if (link.anchor == null) {
            return;
        }
        link.anchor.setCustomName(ChatColor.GRAY + "Iron Chain " + Math.max(0, link.health) + " HP");
    }

    private static void ensureTicker() {
        if (ticker != null) {
            return;
        }
        ticker = Bukkit.getScheduler().runTaskTimer(Main.plugin, ChainLinkManager::tick, 0L, TICK_INTERVAL);
    }

    private static void stopTicker() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    private static void tick() {
        if (LINK_BY_ID.isEmpty()) {
            stopTicker();
            return;
        }
        Iterator<ChainLink> iterator = LINK_BY_ID.values().iterator();
        List<ChainLink> expired = new ArrayList<>();
        while (iterator.hasNext()) {
            ChainLink link = iterator.next();
            if (link.anchor == null || !link.anchor.isValid()) {
                expired.add(link);
                iterator.remove();
                continue;
            }
            link.remainingTicks -= TICK_INTERVAL;
            if (link.remainingTicks <= 0) {
                expired.add(link);
                iterator.remove();
                continue;
            }
            updateLink(link);
            if (link.members.size() < 2) {
                expired.add(link);
                iterator.remove();
            }
        }
        for (ChainLink link : expired) {
            breakLink(link, ChatColor.GRAY + "The chains crumble.");
        }
        if (LINK_BY_ID.isEmpty()) {
            stopTicker();
        }
    }

    private static void updateLink(ChainLink link) {
        Location centroid = computeCentroid(link);
        if (centroid == null) {
            return;
        }
        link.anchor.teleport(centroid);
        updateAnchorName(link);
        spawnAura(link);
        for (Iterator<UUID> iterator = link.members.iterator(); iterator.hasNext(); ) {
            UUID playerId = iterator.next();
            GamePlayer gamePlayer = Main.game.PLAYER_LIST.get(playerId);
            if (gamePlayer == null || gamePlayer.getMinecraftPlayer() == null || gamePlayer.CurrentState != GamePlayer.SurvivalState.ALIVE) {
                PLAYER_TO_LINK.remove(playerId);
                iterator.remove();
                continue;
            }
            Player player = gamePlayer.getMinecraftPlayer();
            Location playerLoc = player.getLocation();
            double distance = playerLoc.distance(centroid);
            if (distance > MAX_DISTANCE) {
                Vector pull = centroid.toVector().subtract(playerLoc.toVector()).normalize().multiply(PULL_FORCE);
                player.setVelocity(player.getVelocity().add(pull));
                player.spawnParticle(Particle.CRIT, playerLoc.clone().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.0);
            }
        }
    }

    private static void spawnAura(ChainLink link) {
        Location center = link.anchor.getLocation().add(0, 1.2, 0);
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            center.getWorld().spawnParticle(Particle.ENCHANT, center.clone().add(x, 0, z), 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private static Location computeCentroid(ChainLink link) {
        double x = 0;
        double y = 0;
        double z = 0;
        int counted = 0;
        World world = null;
        for (UUID playerId : link.members) {
            GamePlayer player = Main.game.PLAYER_LIST.get(playerId);
            if (player == null || player.getMinecraftPlayer() == null) {
                continue;
            }
            Location loc = player.getMinecraftPlayer().getLocation();
            if (world == null) {
                world = loc.getWorld();
            }
            x += loc.getX();
            y += loc.getY();
            z += loc.getZ();
            counted++;
        }
        if (counted == 0 || world == null) {
            return null;
        }
        return new Location(world, x / counted, y / counted, z / counted);
    }

    private static void breakLink(ChainLink link, String message) {
        if (link == null) {
            return;
        }
        LINK_BY_ID.remove(link.id);
        for (UUID playerId : link.members) {
            PLAYER_TO_LINK.remove(playerId);
            GamePlayer player = Main.game.PLAYER_LIST.get(playerId);
            if (player != null && player.getMinecraftPlayer() != null && message != null) {
                player.getMinecraftPlayer().sendMessage(message);
            }
        }
        if (link.anchor != null && link.anchor.isValid()) {
            ANCHOR_TO_LINK.remove(link.anchor.getUniqueId());
            link.anchor.remove();
        } else if (link.anchor != null) {
            ANCHOR_TO_LINK.remove(link.anchor.getUniqueId());
        }
    }

    private static final class ChainLink {
        private final UUID id = UUID.randomUUID();
        private final UUID ownerId;
        private final ArmorStand anchor;
        private final List<UUID> members = new ArrayList<>();
        private final int maxHealth;
        private int health;
        private long remainingTicks;

        private ChainLink(UUID ownerId, ArmorStand anchor, int health, long duration) {
            this.ownerId = ownerId;
            this.anchor = anchor;
            this.health = Math.max(1, health);
            this.maxHealth = this.health;
            this.remainingTicks = Math.max(40L, duration);
        }
    }
}
