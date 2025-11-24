package com.yourfault.system;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles the bleeding (downed) state for players, including timers and revive interactions.
 */
public class BleedoutManager implements Listener {
    private static final int BLEED_OUT_SECONDS = 60;
    private static final int REVIVE_SECONDS = 5;
    private static final double REVIVE_RADIUS = 1.75;
    private static final PotionEffectType EFFECT_SLOW = resolveEffect("SLOW", "SLOWNESS");
    private static final PotionEffectType EFFECT_WEAKNESS = resolveEffect("WEAKNESS");
    private static final PotionEffectType EFFECT_BLINDNESS = resolveEffect("BLINDNESS");

    private final JavaPlugin plugin;
    private final Game game;
    private final Map<UUID, BleedoutState> downedPlayers = new HashMap<>();
    private final Map<UUID, ReviveProcess> activeRevives = new HashMap<>();

    public BleedoutManager(JavaPlugin plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    public boolean isDowned(UUID playerId) {
        return downedPlayers.containsKey(playerId);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        GamePlayer gamePlayer = game.GetPlayer(player);
        if (gamePlayer == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (isDowned(playerId)) {
            event.setCancelled(true);
            return;
        }
        double resultingHealth = player.getHealth() - event.getFinalDamage();
        if (resultingHealth > 0) {
            return;
        }
        event.setCancelled(true);
        startBleedout(gamePlayer);
    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player rescuer = event.getPlayer();
        if (!event.isSneaking()) {
            cancelRevive(rescuer.getUniqueId(), ChatColor.YELLOW + "Revive cancelled.");
            return;
        }
        if (isDowned(rescuer.getUniqueId())) {
            rescuer.sendMessage(ChatColor.RED + "You cannot revive others while bleeding out.");
            return;
        }
        BleedoutState targetState = findNearbyDowned(rescuer);
        if (targetState == null) {
            return;
        }
        beginRevive(rescuer, targetState);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cancelRevive(playerId, null);
        clearDownedState(playerId);
    }

    public void resetAll() {
        downedPlayers.values().forEach(state -> {
            state.cancelTimer();
            Player player = Bukkit.getPlayer(state.playerId);
            if (player != null) {
                restorePlayer(player, state);
            }
        });
        downedPlayers.clear();
        activeRevives.values().forEach(BukkitRunnable::cancel);
        activeRevives.clear();
    }

    private void startBleedout(GamePlayer gamePlayer) {
        Player player = gamePlayer.getMinecraftPlayer();
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (downedPlayers.containsKey(playerId)) {
            return;
        }
        BleedoutState state = new BleedoutState(player);
        downedPlayers.put(playerId, state);
        applyDownedEffects(player, state);
        Bukkit.broadcastMessage(ChatColor.RED + player.getName() + " is bleeding out! Hold SHIFT on them for 5 seconds to revive.");
        BukkitTask timer = new BukkitRunnable() {
            private int ticksRemaining = BLEED_OUT_SECONDS * 20;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                ticksRemaining -= 20;
                if (ticksRemaining <= 0) {
                    cancel();
                    handleBleedoutFailure(playerId);
                    return;
                }
                int seconds = Math.max(0, ticksRemaining / 20);
                player.sendActionBar(Component.text("Bleeding out - " + seconds + "s", NamedTextColor.RED));
            }
        }.runTaskTimer(plugin, 0L, 20L);
        state.bleedoutTask = timer;
        checkForTeamBleedout();
    }

    private void applyDownedEffects(Player player, BleedoutState state) {
        double maxHealth = Math.max(1.0, getMaxHealth(player));
        player.setHealth(Math.max(1.0, maxHealth * 0.15));
        player.setSprinting(false);
        player.setVelocity(new Vector());
        player.setFreezeTicks(Integer.MAX_VALUE);
        player.setSneaking(false);
        if (EFFECT_SLOW != null) {
            player.addPotionEffect(new PotionEffect(EFFECT_SLOW, Integer.MAX_VALUE, 6, false, false, false));
        }
        if (EFFECT_WEAKNESS != null) {
            player.addPotionEffect(new PotionEffect(EFFECT_WEAKNESS, Integer.MAX_VALUE, 2, false, false, false));
        }
        if (EFFECT_BLINDNESS != null) {
            player.addPotionEffect(new PotionEffect(EFFECT_BLINDNESS, 60, 0, false, false, true));
        }
        state.anchorY = player.getLocation().getY();
        state.hadGravity = player.hasGravity();
        state.allowedFlight = player.getAllowFlight();
        state.wasFlying = player.isFlying();
        state.wasSwimming = player.isSwimming();
        player.setGravity(false);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setSleepingIgnored(true);
        try {
            player.setSwimming(true);
        } catch (NoSuchMethodError ignored) {
            // Older versions do not expose swimming state control
        }
        forceDownedPose(player, state);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.6f);
        player.sendTitle(ChatColor.DARK_RED + "Bleeding Out", ChatColor.YELLOW + "Teammates must revive you", 10, 40, 20);
    }

    private void handleBleedoutFailure(UUID playerId) {
        BleedoutState state = downedPlayers.remove(playerId);
        if (state == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            restorePlayer(player, state);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.RED + "You died because no one revived you in time.");
            player.sendTitle(ChatColor.DARK_RED + "You Died", ChatColor.GRAY + "Bleed-out timer expired", 10, 60, 20);
        }
        cancelRevivesTargeting(playerId);
        checkForTeamBleedout();
    }

    private void beginRevive(Player rescuer, BleedoutState targetState) {
        UUID rescuerId = rescuer.getUniqueId();
        if (activeRevives.containsKey(rescuerId)) {
            rescuer.sendMessage(ChatColor.YELLOW + "You are already reviving someone.");
            return;
        }
        if (targetState.activeRescuer != null && !Objects.equals(targetState.activeRescuer, rescuerId)) {
            rescuer.sendMessage(ChatColor.YELLOW + "Another teammate is already reviving them.");
            return;
        }
        if (!isWithinRange(rescuer, targetState.playerId)) {
            rescuer.sendMessage(ChatColor.RED + "Move closer to revive.");
            return;
        }
        ReviveProcess process = new ReviveProcess(rescuerId, targetState.playerId);
        process.runTaskTimer(plugin, 0L, 1L);
        activeRevives.put(rescuerId, process);
        targetState.activeRescuer = rescuerId;
        rescuer.sendMessage(ChatColor.GREEN + "Reviving " + Bukkit.getOfflinePlayer(targetState.playerId).getName() + "...");
    }

    private void revivePlayer(UUID targetId, Player rescuer) {
        BleedoutState state = downedPlayers.remove(targetId);
        if (state == null) {
            return;
        }
        state.cancelTimer();
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            return;
        }
        restorePlayer(target, state);
        GamePlayer gamePlayer = game.GetPlayer(target);
        double maxHealth = Math.max(1.0, getMaxHealth(target));
        double revivedHealth = Math.max(1.0, maxHealth * 0.2);
        target.setGameMode(GameMode.ADVENTURE);
        target.setHealth(Math.min(maxHealth, revivedHealth));
        if (gamePlayer != null) {
            gamePlayer.setHealth(gamePlayer.getMaxHealth() * 0.2f);
        }
        target.sendTitle(ChatColor.GREEN + "Revived", ChatColor.GRAY + "Back in the fight", 10, 40, 10);
        if (rescuer != null) {
            rescuer.sendMessage(ChatColor.GOLD + "You revived " + target.getName() + "!");
        }
        Bukkit.broadcastMessage(ChatColor.GREEN + target.getName() + " has been revived." + (rescuer != null ? " Thanks to " + rescuer.getName() + "!" : ""));
    }

    private boolean isWithinRange(Player rescuer, UUID targetId) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            return false;
        }
        if (!Objects.equals(target.getWorld(), rescuer.getWorld())) {
            return false;
        }
        return rescuer.getLocation().distanceSquared(target.getLocation()) <= REVIVE_RADIUS * REVIVE_RADIUS;
    }

    private BleedoutState findNearbyDowned(Player rescuer) {
        UUID closest = null;
        double bestDistance = Double.MAX_VALUE;
        for (BleedoutState state : downedPlayers.values()) {
            Player target = Bukkit.getPlayer(state.playerId);
            if (target == null || !target.isOnline()) {
                continue;
            }
            if (!Objects.equals(target.getWorld(), rescuer.getWorld())) {
                continue;
            }
            double distance = rescuer.getLocation().distanceSquared(target.getLocation());
            if (distance > REVIVE_RADIUS * REVIVE_RADIUS) {
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                closest = state.playerId;
            }
        }
        return closest == null ? null : downedPlayers.get(closest);
    }

    private void cancelRevive(UUID rescuerId, String message) {
        ReviveProcess process = activeRevives.remove(rescuerId);
        boolean hadProcess = process != null;
        if (process != null) {
            process.cancel();
            BleedoutState state = downedPlayers.get(process.targetId);
            if (state != null && Objects.equals(state.activeRescuer, rescuerId)) {
                state.activeRescuer = null;
            }
        }
        if (message != null && hadProcess) {
            Player rescuer = Bukkit.getPlayer(rescuerId);
            if (rescuer != null) {
                rescuer.sendMessage(message);
            }
        }
    }

    private void cancelRevivesTargeting(UUID targetId) {
        activeRevives.entrySet().removeIf(entry -> {
            ReviveProcess process = entry.getValue();
            if (!Objects.equals(process.targetId, targetId)) {
                return false;
            }
            process.cancel();
            Player rescuer = Bukkit.getPlayer(entry.getKey());
            if (rescuer != null) {
                rescuer.sendMessage(ChatColor.RED + "Revive target died.");
            }
            return true;
        });
        BleedoutState state = downedPlayers.get(targetId);
        if (state != null) {
            state.activeRescuer = null;
        }
    }

    private void clearDownedState(UUID playerId) {
        BleedoutState state = downedPlayers.remove(playerId);
        if (state == null) {
            return;
        }
        state.cancelTimer();
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            restorePlayer(player, state);
        }
    }

    private void restorePlayer(Player player, BleedoutState state) {
        player.setFreezeTicks(state.previousFreezeTicks);
        player.setWalkSpeed(state.previousWalkSpeed);
        player.setCollidable(state.wasCollidable);
        player.setInvulnerable(state.wasInvulnerable);
        player.setSleepingIgnored(state.wasSleepingIgnored);
        player.setGravity(state.hadGravity);
        player.setAllowFlight(state.allowedFlight);
        if (state.allowedFlight) {
            player.setFlying(state.wasFlying);
        }
        try {
            player.setSwimming(state.wasSwimming);
        } catch (NoSuchMethodError ignored) {
            // ignore
        }
        if (state.poseTask != null) {
            state.poseTask.cancel();
            state.poseTask = null;
        }
        if (EFFECT_SLOW != null) {
            player.removePotionEffect(EFFECT_SLOW);
        }
        if (EFFECT_WEAKNESS != null) {
            player.removePotionEffect(EFFECT_WEAKNESS);
        }
        if (EFFECT_BLINDNESS != null) {
            player.removePotionEffect(EFFECT_BLINDNESS);
        }
        if (player.isSleeping()) {
            try {
                player.wakeup(true);
            } catch (NoSuchMethodError ignored) {
                player.setPose(Pose.STANDING);
            }
        }
        player.setSneaking(false);
        try {
            player.setPose(state.previousPose);
        } catch (NoSuchMethodError ignored) {
            player.setPose(Pose.STANDING);
        }
    }

    private void checkForTeamBleedout() {
        if (game.PLAYER_LIST.isEmpty()) {
            return;
        }
        boolean allBleeding = game.PLAYER_LIST.values().stream()
                .map(GamePlayer::getMinecraftPlayer)
                .filter(Objects::nonNull)
                .filter(player -> player.getGameMode() != GameMode.SPECTATOR)
                .allMatch(player -> downedPlayers.containsKey(player.getUniqueId()));
        if (!allBleeding) {
            return;
        }
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "All players are bleeding out! The run has ended.");
        Main.plugin.getServer().getScheduler().runTask(plugin, () -> {
            Main.game.EndGame();
            resetAll();
        });
    }

    private double getMaxHealth(Player player) {
        return Math.max(1.0, player.getMaxHealth());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        BleedoutState state = downedPlayers.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        double maxY = state.anchorY + 0.05;
        if (event.getTo().getY() > maxY) {
            event.setTo(event.getFrom());
            player.setVelocity(new Vector());
        }
    }

    private final class ReviveProcess extends BukkitRunnable {
        private final UUID rescuerId;
        private final UUID targetId;
        private int ticksRemaining = REVIVE_SECONDS * 20;

        private ReviveProcess(UUID rescuerId, UUID targetId) {
            this.rescuerId = rescuerId;
            this.targetId = targetId;
        }

        @Override
        public void run() {
            Player rescuer = Bukkit.getPlayer(rescuerId);
            Player target = Bukkit.getPlayer(targetId);
            if (rescuer == null || target == null || !rescuer.isOnline() || !target.isOnline()) {
                cancelRevive(rescuerId, ChatColor.RED + "Revive interrupted.");
                return;
            }
            if (!rescuer.isSneaking() || !isWithinRange(rescuer, targetId)) {
                cancelRevive(rescuerId, ChatColor.RED + "Revive interrupted.");
                return;
            }
            if (!downedPlayers.containsKey(targetId)) {
                cancelRevive(rescuerId, null);
                return;
            }
            ticksRemaining--;
            double progress = 1.0 - (ticksRemaining / (double) (REVIVE_SECONDS * 20));
            rescuer.sendActionBar(Component.text(String.format(Locale.US, "Reviving %.0f%%", progress * 100), NamedTextColor.GOLD));
            target.sendActionBar(Component.text(rescuer.getName() + " reviving...", NamedTextColor.YELLOW));
            if (ticksRemaining <= 0) {
                cancel();
                activeRevives.remove(rescuerId);
                BleedoutState state = downedPlayers.get(targetId);
                if (state != null && Objects.equals(state.activeRescuer, rescuerId)) {
                    state.activeRescuer = null;
                }
                revivePlayer(targetId, rescuer);
            }
        }
    }

    private static final class BleedoutState {
        private final UUID playerId;
        private final float previousWalkSpeed;
        private final int previousFreezeTicks;
        private final boolean wasCollidable;
        private final boolean wasInvulnerable;
        private final Pose previousPose;
        private final boolean wasSleepingIgnored;
        private double anchorY;
        private boolean hadGravity;
        private boolean allowedFlight;
        private boolean wasFlying;
        private boolean wasSwimming;
        private BukkitTask poseTask;
        private BukkitTask bleedoutTask;
        private UUID activeRescuer;

        private BleedoutState(Player player) {
            this.playerId = player.getUniqueId();
            this.previousWalkSpeed = player.getWalkSpeed();
            this.previousFreezeTicks = player.getFreezeTicks();
            this.wasCollidable = player.isCollidable();
            this.wasInvulnerable = player.isInvulnerable();
            this.previousPose = player.getPose();
            this.wasSleepingIgnored = player.isSleepingIgnored();
            this.anchorY = player.getLocation().getY();
            this.hadGravity = player.hasGravity();
            this.allowedFlight = player.getAllowFlight();
            this.wasFlying = player.isFlying();
            this.wasSwimming = player.isSwimming();
        }

        private void cancelTimer() {
            if (bleedoutTask != null) {
                bleedoutTask.cancel();
                bleedoutTask = null;
            }
        }
    }

    private void forceDownedPose(Player player, BleedoutState state) {
        if (state == null) {
            return;
        }
        applyDownedPose(player);
        if (state.poseTask != null) {
            state.poseTask.cancel();
        }
        state.poseTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !downedPlayers.containsKey(player.getUniqueId())) {
                    cancel();
                    state.poseTask = null;
                    return;
                }
                applyDownedPose(player);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void applyDownedPose(Player player) {
        try {
            player.setPose(Pose.SWIMMING);
        } catch (NoSuchMethodError ignored) {
            player.setPose(Pose.SNEAKING);
        }
        try {
            player.setSwimming(true);
        } catch (NoSuchMethodError ignored) {
            // ignore
        }
    }
    private static PotionEffectType resolveEffect(String... names) {
        for (String name : names) {
            PotionEffectType type = PotionEffectType.getByName(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }
}
