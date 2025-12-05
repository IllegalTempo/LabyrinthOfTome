package com.yourfault.perks.shop;

import com.yourfault.Main;
import com.yourfault.perks.PerkType;
import com.yourfault.system.Game;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PerkShopManager implements Listener {

    private static final long CONFIRM_WINDOW_MS = 2000L;
    private static final double RAYTRACE_DISTANCE = 8.0;
    private static final List<Vector> SLOT_POSITIONS = List.of(
            new Vector(300.5, -56, 53.5),
            new Vector(299.5, -56, 48.5),
            new Vector(302.5, -56, 43.5),
            new Vector(309.5, -56, 41.5),
            new Vector(316.5, -56, 43.5),
            new Vector(319.5, -56, 48.5),
            new Vector(318.5, -56, 53.5)
    );

    private final Game game;
    private final Map<UUID, ShopSession> sessions = new HashMap<>();
    private final Map<UUID, ConfirmationState> confirmations = new HashMap<>();
    private BukkitTask highlightTask;

    public PerkShopManager(Game game) {
        this.game = game;
        Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
    }

    public void openShopForPlayers(Collection<GamePlayer> players) {
        closeShop();
        World world = Main.world;
        if (world == null) {
            Main.plugin.getLogger().warning("Cannot open perk shop without a loaded world.");
            return;
        }
        for (GamePlayer player : players) {
            spawnSession(player, world);
        }
        if (!sessions.isEmpty()) {
            startHighlightTask();
        }
    }

    public void closeShop() {
        confirmations.clear();
        for (ShopSession session : sessions.values()) {
            session.destroy();
        }
        sessions.clear();
        stopHighlightTask();
    }

    private void spawnSession(GamePlayer gamePlayer, World world) {
        Player player = gamePlayer.getMinecraftPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }
        List<PerkType> rolled = rollOffers(gamePlayer);
        if (rolled.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No perks available right now.");
            return;
        }
        List<PerkOfferDisplay> offers = new ArrayList<>();
        for (int i = 0; i < rolled.size() && i < SLOT_POSITIONS.size(); i++) {
            Location slotLocation = SLOT_POSITIONS.get(i).toLocation(world);
            offers.add(spawnOfferDisplay(slotLocation, rolled.get(i), player));
        }
        ShopSession session = new ShopSession(gamePlayer, offers);
        sessions.put(player.getUniqueId(), session);
        player.sendMessage(ChatColor.GOLD + "Perk shop ready. Tokens available: " + gamePlayer.getPerkSelectionTokens());
        player.sendMessage(ChatColor.GRAY + "Look at a perk and left-click twice to confirm your choice.");
    }

    private List<PerkType> rollOffers(GamePlayer gamePlayer) {
        List<PerkType> pool = new ArrayList<>(game.ALL_PERKS.values());
        pool.removeIf(perk -> gamePlayer.PLAYER_PERKS.hasPerk(perk));
        if (pool.isEmpty()) {
            pool.addAll(game.ALL_PERKS.values());
        }
        Collections.shuffle(pool);
        int count = Math.min(SLOT_POSITIONS.size(), pool.size());
        return new ArrayList<>(pool.subList(0, count));
    }

    private PerkOfferDisplay spawnOfferDisplay(Location location, PerkType perkType, Player viewer) {
        ItemDisplay icon = location.getWorld().spawn(location.clone().add(0, 0.4, 0), ItemDisplay.class, display -> {
            display.setItemStack(perkType.buildShopIcon());
            display.setBillboard(Display.Billboard.CENTER);
            display.setGlowing(false);
            display.setInterpolationDuration(0);
            display.setRotation(0f, 0f);
            display.setVisibleByDefault(false);
        });
        TextDisplay label = location.getWorld().spawn(location.clone().add(0, 1.3, 0), TextDisplay.class, display -> {
            Component text = Component.text(perkType.displayName, NamedTextColor.GOLD)
                    .append(Component.text("\n" + perkType.getCategory().name(), NamedTextColor.GRAY));
            for (String loreLine : perkType.description) {
                String clean = ChatColor.stripColor(loreLine);
                if (clean == null) {
                    clean = loreLine;
                }
                text = text.append(Component.text("\n" + clean, NamedTextColor.GRAY));
            }
            display.text(text);
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadowed(true);
            display.setInterpolationDuration(0);
            display.setVisibleByDefault(false);
        });
        showEntityToViewer(icon, viewer);
        showEntityToViewer(label, viewer);
        return new PerkOfferDisplay(perkType, icon, label);
    }

    private void showEntityToViewer(Entity entity, Player viewer) {
        if (entity == null || viewer == null) {
            return;
        }
        viewer.showEntity(Main.plugin, entity);
    }

    private void startHighlightTask() {
        if (highlightTask != null) {
            return;
        }
        highlightTask = Bukkit.getScheduler().runTaskTimer(Main.plugin, this::tickHighlights, 2L, 2L);
    }

    private void stopHighlightTask() {
        if (highlightTask != null) {
            highlightTask.cancel();
            highlightTask = null;
        }
    }

    private void tickHighlights() {
        Iterator<Map.Entry<UUID, ShopSession>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ShopSession> entry = iterator.next();
            ShopSession session = entry.getValue();
            Player player = session.getBukkitPlayer();
            if (player == null || !player.isOnline()) {
                session.destroy();
                iterator.remove();
                continue;
            }
            Entity target = findLookTarget(player, session);
            session.updateHighlight(target);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        ShopSession session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            session.destroy();
        }
        confirmations.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> {
                if (handleSelectionAttempt(event.getPlayer())) {
                    event.setCancelled(true);
                }
            }
            default -> {
            }
        }
    }

    private boolean handleSelectionAttempt(Player player) {
        ShopSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return false;
        }
        Entity target = findLookTarget(player, session);
        if (target == null) {
            return false;
        }
        PerkOfferDisplay offer = session.getOffer(target.getUniqueId());
        if (offer == null || offer.claimed) {
            return false;
        }
        long now = System.currentTimeMillis();
        ConfirmationState confirmation = confirmations.get(player.getUniqueId());
        if (confirmation != null && confirmation.offerId().equals(offer.iconId()) && confirmation.expiresAt() >= now) {
            confirmations.remove(player.getUniqueId());
            finalizeSelection(session, offer);
        } else {
            confirmations.put(player.getUniqueId(), new ConfirmationState(offer.iconId(), now + CONFIRM_WINDOW_MS));
            player.sendMessage(ChatColor.YELLOW + "Are you sure to select " + ChatColor.GOLD + offer.perkType.displayName + ChatColor.YELLOW + "? Click again to confirm.");
        }
        return true;
    }

    private Entity findLookTarget(Player player, ShopSession session) {
        if (player == null || session == null) {
            return null;
        }
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        double maxDistanceSquared = RAYTRACE_DISTANCE * RAYTRACE_DISTANCE;
        double bestDistance = maxDistanceSquared;
        Entity best = null;
        for (PerkOfferDisplay offer : session.getDisplays()) {
            ItemDisplay icon = offer.icon();
            if (icon == null || icon.isDead()) {
                continue;
            }
            Location iconLocation = icon.getLocation();
            if (!Objects.equals(iconLocation.getWorld(), eye.getWorld())) {
                continue;
            }
            Vector toIcon = iconLocation.toVector().subtract(eye.toVector());
            double distanceSquared = toIcon.lengthSquared();
            if (distanceSquared > bestDistance) {
                continue;
            }
            Vector directionToIcon = toIcon.clone().normalize();
            double alignment = direction.dot(directionToIcon);
            if (alignment < 0.97) {
                continue;
            }
            if (!player.hasLineOfSight(icon)) {
                continue;
            }
            bestDistance = distanceSquared;
            best = icon;
        }
        return best;
    }

    private void finalizeSelection(ShopSession session, PerkOfferDisplay offer) {
        GamePlayer gamePlayer = session.gamePlayer;
        Player player = session.getBukkitPlayer();
        if (gamePlayer == null || player == null) {
            return;
        }
        if (gamePlayer.PLAYER_PERKS.hasPerk(offer.perkType)) {
            player.sendMessage(ChatColor.RED + "You already own this perk.");
            return;
        }
        if (!gamePlayer.consumePerkSelectionToken()) {
            player.sendMessage(ChatColor.RED + "No perk selections left. Level up or defeat bosses to earn more.");
            return;
        }
        boolean applied = gamePlayer.PLAYER_PERKS.applyPerkSelection(offer.perkType);
        if (!applied) {
            player.sendMessage(ChatColor.RED + "Unable to apply perk right now. Try again later.");
            gamePlayer.grantPerkSelectionTokens(1);
            return;
        }
        offer.claim();
        session.removeOffer(offer.iconId());
        player.sendMessage(ChatColor.GREEN + "Selected perk: " + ChatColor.GOLD + offer.perkType.displayName + ChatColor.GRAY + ". Remaining selections: " + gamePlayer.getPerkSelectionTokens());
    }

    private record ConfirmationState(UUID offerId, long expiresAt) {
    }

    private static class ShopSession {
        private final GamePlayer gamePlayer;
        private final Map<UUID, PerkOfferDisplay> offers = new HashMap<>();
        private PerkOfferDisplay highlighted;

        private ShopSession(GamePlayer gamePlayer, List<PerkOfferDisplay> displayList) {
            this.gamePlayer = gamePlayer;
            for (PerkOfferDisplay display : displayList) {
                offers.put(display.iconId(), display);
            }
        }

        private Player getBukkitPlayer() {
            return gamePlayer != null ? gamePlayer.getMinecraftPlayer() : null;
        }

        private boolean isOfferEntity(UUID uniqueId) {
            return offers.containsKey(uniqueId);
        }

        private PerkOfferDisplay getOffer(UUID uniqueId) {
            return offers.get(uniqueId);
        }

        private void updateHighlight(Entity entity) {
            UUID id = entity != null ? entity.getUniqueId() : null;
            if (highlighted != null && (id == null || !Objects.equals(highlighted.iconId(), id))) {
                highlighted.setHighlighted(false);
                highlighted = null;
            }
            if (id == null) {
                return;
            }
            PerkOfferDisplay offer = offers.get(id);
            if (offer == null || offer == highlighted) {
                if (offer != null) {
                    offer.setHighlighted(true);
                }
                highlighted = offer;
                return;
            }
            offer.setHighlighted(true);
            highlighted = offer;
        }

        private void destroy() {
            offers.values().forEach(PerkOfferDisplay::destroy);
            offers.clear();
            highlighted = null;
        }

        private void removeOffer(UUID offerId) {
            PerkOfferDisplay removed = offers.remove(offerId);
            if (removed != null && highlighted != null && Objects.equals(highlighted.iconId(), offerId)) {
                highlighted = null;
            }
        }

        private Collection<PerkOfferDisplay> getDisplays() {
            return offers.values();
        }
    }

    private static class PerkOfferDisplay {
        private final PerkType perkType;
        private final ItemDisplay icon;
        private final TextDisplay label;
        private boolean claimed;

        private PerkOfferDisplay(PerkType perkType, ItemDisplay icon, TextDisplay label) {
            this.perkType = perkType;
            this.icon = icon;
            this.label = label;
        }

        private UUID iconId() {
            return icon.getUniqueId();
        }

        private void setHighlighted(boolean highlighted) {
            if (icon != null) {
                icon.setGlowing(highlighted);
            }
        }

        private ItemDisplay icon() {
            return icon;
        }

        private void claim() {
            claimed = true;
            if (icon != null) {
                icon.remove();
            }
            if (label != null) {
                label.text(Component.text(perkType.displayName + " selected", NamedTextColor.GRAY));
            }
        }

        private void destroy() {
            if (icon != null) {
                icon.remove();
            }
            if (label != null) {
                label.remove();
            }
        }
    }
}
