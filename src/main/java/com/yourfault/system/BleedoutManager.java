package com.yourfault.system;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;
import java.util.UUID;

/**
 * Handles the bleeding (downed) state for players, including timers and revive interactions.
 */
public class BleedoutManager implements Listener {
    public static final int BLEED_OUT_SECONDS = 60;
    public static final int REVIVE_SECONDS = 5;
    public static final double REVIVE_RADIUS = 3.5;

    private static final PotionEffectType EFFECT_SLOW = resolveEffect("SLOW", "SLOWNESS");
    private static final PotionEffectType EFFECT_WEAKNESS = resolveEffect("WEAKNESS");
    private static final PotionEffectType EFFECT_BLINDNESS = resolveEffect("BLINDNESS");

    private final JavaPlugin plugin;
    private final Game game;
//    private final Map<UUID, BleedoutState> downedPlayers = new HashMap<>();
//    private final Map<UUID, ReviveProcess> activeRevives = new HashMap<>();

    public BleedoutManager(JavaPlugin plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }


//    @EventHandler(ignoreCancelled = true) //to be removed
//    public void onPlayerDamage(EntityDamageEvent event) {
//        if (!(event.getEntity() instanceof Player player)) {
//            return;
//        }
//        GamePlayer gamePlayer = game.GetPlayer(player);
//        if (gamePlayer == null) {
//            return;
//        }
//        UUID playerId = player.getUniqueId();
//        if (gamePlayer.IsDowned) {
//            event.setCancelled(true);
//            return;
//        }
//        double resultingHealth = player.getHealth() - event.getFinalDamage();
//        if (resultingHealth > 0) {
//            return;
//        }
//        event.setCancelled(true);
//        startBleedout(gamePlayer);
//    }

//    @EventHandler(priority = EventPriority.MONITOR)
//    public void onPlayerDeath(PlayerDeathEvent event) {
//        Player player = event.getEntity();
//        GamePlayer gamePlayer = game.GetPlayer(player);
//        if (gamePlayer == null) {
//            return;
//        }
//        UUID playerId = player.getUniqueId();
//        if (player.getGameMode() == GameMode.SPECTATOR || downedPlayers.containsKey(playerId)) {
//            return;
//        }
//        EntityDamageEvent lastDamage = player.getLastDamageCause();
//        if (lastDamage == null) {
//            return;
//        }
//        Location deathLocation = player.getLocation().clone();
//        event.getDrops().clear();
//        event.setKeepInventory(true);
//        event.setKeepLevel(true);
//        event.setDroppedExp(0);
//        event.deathMessage(null);
//        Bukkit.getScheduler().runTask(plugin, () -> {
//            if (!player.isDead()) {
//                return;
//            }
//            player.spigot().respawn();
//            player.teleport(deathLocation);
//            player.setVelocity(new Vector());
//            player.setFallDistance(0f);
//            startBleedout(gamePlayer);
//        });
//    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player rescuer = event.getPlayer();
        GamePlayer rescuerGamePlayer = Main.game.GetPlayer(rescuer);
        if (rescuerGamePlayer.CurrentState != GamePlayer.SurvivalState.ALIVE) {
            rescuer.sendMessage(ChatColor.RED + "You cannot revive others while bleeding out.");
            return;
        }
        if(event.isSneaking())
        {
            GamePlayer Target = findNearbyDowned(rescuer);
            if (Target == null) {
                return;
            }
            rescuerGamePlayer.beginRevive(Target);
        } else {
            rescuerGamePlayer.CancelRevive();
        }



    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Main.game.GetPlayer(event.getPlayer()).CancelRevive();
    }

//    public void resetAll() {
//        downedPlayers.values().forEach(state -> {
//            state.cancelTimer();
//            Player player = Bukkit.getPlayer(state.playerId);
//            if (player != null) {
//                restorePlayer(player, state);
//            }
//        });
//        downedPlayers.clear();
//        activeRevives.values().forEach(BukkitRunnable::cancel);
//        activeRevives.clear();
//    }

//    private void startBleedout(GamePlayer gamePlayer) {
//        Player player = gamePlayer.getMinecraftPlayer();
//        if (player == null) {
//            return;
//        }
//        UUID playerId = player.getUniqueId();
//        if (downedPlayers.containsKey(playerId)) {
//            return;
//        }
//        BleedoutState state = new BleedoutState(player);
//        downedPlayers.put(playerId, state);
//        applyDownedEffects(player, gamePlayer, state);
//        Bukkit.broadcastMessage(ChatColor.RED + player.getName() + " is bleeding out! Hold SHIFT on them for 5 seconds to revive.");
//        BukkitTask timer = new BukkitRunnable() {
//            private int ticksRemaining = BLEED_OUT_SECONDS * 20;
//            @Override
//            public void run() {
//                if (!player.isOnline()) {
//                    cancel();
//                    return;
//                }
//                ticksRemaining -= 20;
//                if (ticksRemaining <= 0) {
//                    cancel();
//                    handleBleedoutFailure(playerId);
//                    return;
//                }
//                int seconds = Math.max(0, ticksRemaining / 20);
//                player.sendActionBar(Component.text("Bleeding out - " + seconds + "s", NamedTextColor.RED));
//            }
//        }.runTaskTimer(plugin, 0L, 20L);
//        state.bleedoutTask = timer;
//        checkForTeamBleedout();
//    }

//    private void applyDownedEffects(Player player, GamePlayer gamePlayer, BleedoutState state) {
//        double maxHealth = Math.max(1.0, getMaxHealth(player));
//        player.setHealth(Math.max(1.0, maxHealth * 0.15));
//        player.setSprinting(false);
//        player.setVelocity(new Vector());
//        player.setFreezeTicks(Integer.MAX_VALUE);
//        player.setSneaking(false);
//        if (EFFECT_SLOW != null) {
//            player.addPotionEffect(new PotionEffect(EFFECT_SLOW, Integer.MAX_VALUE, 6, false, false, false));
//        }
//        if (EFFECT_WEAKNESS != null) {
//            player.addPotionEffect(new PotionEffect(EFFECT_WEAKNESS, Integer.MAX_VALUE, 2, false, false, false));
//        }
//        if (EFFECT_BLINDNESS != null) {
//            player.addPotionEffect(new PotionEffect(EFFECT_BLINDNESS, 60, 0, false, false, true));
//        }
//        state.anchorY = player.getLocation().getY();
//        if (gamePlayer != null) {
//            gamePlayer.beginBleedVisual(plugin, () -> downedPlayers.containsKey(player.getUniqueId()));
//        }
//        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.6f);
//        player.sendTitle(ChatColor.DARK_RED + "Bleeding Out", ChatColor.YELLOW + "Teammates must revive you", 10, 40, 20);
//    }
//
//    private void handleBleedoutFailure(UUID playerId) {
//        BleedoutState state = downedPlayers.remove(playerId);
//        if (state == null) {
//            return;
//        }
//        Player player = Bukkit.getPlayer(playerId);
//        if (player != null) {
//            restorePlayer(player, state);
//            player.setGameMode(GameMode.SPECTATOR);
//            player.sendMessage(ChatColor.RED + "You died because no one revived you in time.");
//            player.sendTitle(ChatColor.DARK_RED + "You Died", ChatColor.GRAY + "Bleed-out timer expired", 10, 60, 20);
//        }
//        cancelRevivesTargeting(playerId);
//        checkForTeamBleedout();
//    }
//
//
//
//
//
//    private boolean isWithinRange(Player rescuer, UUID targetId) {
//        Player target = Bukkit.getPlayer(targetId);
//        if (target == null || !target.isOnline()) {
//            return false;
//        }
//        if (!Objects.equals(target.getWorld(), rescuer.getWorld())) {
//            return false;
//        }
//        return rescuer.getLocation().distanceSquared(target.getLocation()) <= REVIVE_RADIUS * REVIVE_RADIUS;
//    }
//
    private GamePlayer findNearbyDowned(Player rescuer) {
        Location rescuerLocation = rescuer.getLocation();
        double maxDistanceSquared = REVIVE_RADIUS * REVIVE_RADIUS;
        for (Player targetPlayer : Bukkit.getOnlinePlayers()) {
            if (targetPlayer.equals(rescuer)) {
                continue;
            }
            GamePlayer targetState = Main.game.GetPlayer(targetPlayer);
            if (targetState == null || targetState.CurrentState != GamePlayer.SurvivalState.DOWNED) {
                continue;
            }
            if (!Objects.equals(targetPlayer.getWorld(), rescuerLocation.getWorld())) {
                continue;
            }
            Location reviveLocation = targetPlayer.getLocation().clone().subtract(GamePlayer.Downed_WatchOffset);
            if (reviveLocation.distanceSquared(rescuerLocation) <= maxDistanceSquared) {
                return targetState;
            }
        }
        return null;
    }

//    private void cancelRevive(UUID rescuerId, String message) {
//        ReviveProcess process = activeRevives.remove(rescuerId);
//        boolean hadProcess = process != null;
//        if (process != null) {
//            process.cancel();
//            BleedoutState state = downedPlayers.get(process.targetId);
//            if (state != null && Objects.equals(state.activeRescuer, rescuerId)) {
//                state.activeRescuer = null;
//            }
//        }
//        if (message != null && hadProcess) {
//            Player rescuer = Bukkit.getPlayer(rescuerId);
//            if (rescuer != null) {
//                rescuer.sendMessage(message);
//            }
//        }
//    }

//    private void cancelRevivesTargeting(UUID targetId) {
//        activeRevives.entrySet().removeIf(entry -> {
//            ReviveProcess process = entry.getValue();
//            if (!Objects.equals(process.targetId, targetId)) {
//                return false;
//            }
//            process.cancel();
//            Player rescuer = Bukkit.getPlayer(entry.getKey());
//            if (rescuer != null) {
//                rescuer.sendMessage(ChatColor.RED + "Revive target died.");
//            }
//            return true;
//        });
//        BleedoutState state = downedPlayers.get(targetId);
//        if (state != null) {
//            state.activeRescuer = null;
//        }
//    }
//
//    private void clearDownedState(UUID playerId) {
//        BleedoutState state = downedPlayers.remove(playerId);
//        if (state == null) {
//            return;
//        }
//        state.cancelTimer();
//        Player player = Bukkit.getPlayer(playerId);
//        if (player != null) {
//            restorePlayer(player, state);
//        }
//    }
//
//    private void restorePlayer(Player player, BleedoutState state) {
//        GamePlayer gamePlayer = game.GetPlayer(player);
//        if (gamePlayer != null) {
//            gamePlayer.endBleedVisual();
//        }
//        player.setFreezeTicks(state.previousFreezeTicks);
//        player.setWalkSpeed(state.previousWalkSpeed);
//        player.setCollidable(state.wasCollidable);
//        player.setInvulnerable(state.wasInvulnerable);
//        if (EFFECT_SLOW != null) {
//            player.removePotionEffect(EFFECT_SLOW);
//        }
//        if (EFFECT_WEAKNESS != null) {
//            player.removePotionEffect(EFFECT_WEAKNESS);
//        }
//        if (EFFECT_BLINDNESS != null) {
//            player.removePotionEffect(EFFECT_BLINDNESS);
//        }
//        player.setSneaking(false);
//        try {
//            player.setPose(Pose.STANDING);
//        } catch (NoSuchMethodError ignored) {
//            // ignore
//        }
//    }

//    private void checkForTeamBleedout() {
//        if (game.PLAYER_LIST.isEmpty()) {
//            return;
//        }
//        boolean allBleeding = game.PLAYER_LIST.values().stream()
//                .map(GamePlayer::getMinecraftPlayer)
//                .filter(Objects::nonNull)
//                .filter(player -> player.getGameMode() != GameMode.SPECTATOR)
//                .allMatch(player -> downedPlayers.containsKey(player.getUniqueId()));
//        if (!allBleeding) {
//            return;
//        }
//        Bukkit.broadcastMessage(ChatColor.DARK_RED + "All players are bleeding out! The run has ended.");
//        Main.plugin.getServer().getScheduler().runTask(plugin, () -> {
//            Main.game.EndGame();
//            resetAll();
//        });
//    }

//    private double getMaxHealth(Player player) {
//        return Math.max(1.0, player.getMaxHealth());
//    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = Main.game.GetPlayer(player);
        if(gamePlayer.CurrentState == GamePlayer.SurvivalState.DOWNED)
        {
            event.setCancelled(true);
        }
    }

//    private final class ReviveProcess extends BukkitRunnable {
//        private final UUID rescuerId;
//        private final UUID targetId;
//        private int ticksRemaining = REVIVE_SECONDS * 20;
//
//        private ReviveProcess(UUID rescuerId, UUID targetId) {
//            this.rescuerId = rescuerId;
//            this.targetId = targetId;
//        }
//
//        @Override
//        public void run() {
//            Player rescuer = Bukkit.getPlayer(rescuerId);
//            Player target = Bukkit.getPlayer(targetId);
//            if (rescuer == null || target == null || !rescuer.isOnline() || !target.isOnline()) {
//                cancelRevive(rescuerId, ChatColor.RED + "Revive interrupted.");
//                return;
//            }
//            if (!rescuer.isSneaking() || !isWithinRange(rescuer, targetId)) {
//                cancelRevive(rescuerId, ChatColor.RED + "Revive interrupted.");
//                return;
//            }
//            if (!downedPlayers.containsKey(targetId)) {
//                cancelRevive(rescuerId, null);
//                return;
//            }
//            ticksRemaining--;
//            double progress = 1.0 - (ticksRemaining / (double) (REVIVE_SECONDS * 20));
//            rescuer.sendActionBar(Component.text(String.format(Locale.US, "Reviving %.0f%%", progress * 100), NamedTextColor.GOLD));
//            target.sendActionBar(Component.text(rescuer.getName() + " reviving...", NamedTextColor.YELLOW));
//            if (ticksRemaining <= 0) {
//                cancel();
//                activeRevives.remove(rescuerId);
//                BleedoutState state = downedPlayers.get(targetId);
//                if (state != null && Objects.equals(state.activeRescuer, rescuerId)) {
//                    state.activeRescuer = null;
//                }
//                revivePlayer(targetId, rescuer);
//            }
//        }
//    }

//    private static final class BleedoutState {
//        private final UUID playerId;
//        private final float previousWalkSpeed;
//        private final int previousFreezeTicks;
//        private final boolean wasCollidable;
//        private final boolean wasInvulnerable;
//        private double anchorY;
//        private BukkitTask bleedoutTask;
//        private UUID activeRescuer;
//
//        private BleedoutState(Player player) {
//            this.playerId = player.getUniqueId();
//            this.previousWalkSpeed = player.getWalkSpeed();
//            this.previousFreezeTicks = player.getFreezeTicks();
//            this.wasCollidable = player.isCollidable();
//            this.wasInvulnerable = player.isInvulnerable();
//            this.anchorY = player.getLocation().getY();
//        }
//
//        private void cancelTimer() {
//            if (bleedoutTask != null) {
//                bleedoutTask.cancel();
//                bleedoutTask = null;
//            }
//        }
//    }

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
