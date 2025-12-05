package com.yourfault.listener;

import com.yourfault.Items.gui.General;
import com.yourfault.Main;
import com.yourfault.perks.PerkType;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.GeneralPlayer.Perks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static com.yourfault.NBT_namespace.PERK_TYPE;

public class PerkSelectionListener implements Listener {
    private static final int SELECTOR_SLOT = 8;
    private static final String GUI_TITLE = "Level Perk Upgrades";
    private static final long PERK_REMOVE_CONFIRM_WINDOW_MS = 1500L;

    private final NamespacedKey selectorKey;
    private final ItemStack selectorItem;
    //private final Map<UUID, RemovalPrompt> pendingRemoval = new HashMap<>();

    public PerkSelectionListener(JavaPlugin plugin) {
        this.selectorKey = new NamespacedKey(plugin, "perk_selector_item");
        this.selectorItem = buildSelectorItem();
    }

    public void preparePlayer(GamePlayer player) {
        player.PLAYER_PERKS.preparePerkSlots();
        ItemStack selector = selectorItem.clone();
        selector.setAmount(1);
        player.getMinecraftPlayer().getInventory().setItem(SELECTOR_SLOT, selector);
    }

    private ItemStack buildSelectorItem() {
        ItemStack stack = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Manage Level Perks");
            meta.setLore(List.of(ChatColor.GRAY + "Right-click to upgrade owned level perks."));
            meta.getPersistentDataContainer().set(selectorKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @EventHandler
    public void onSelectorUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !isSelectorItem(item)) return;
        event.setCancelled(true);
        openSelection(event.getPlayer());
    }

    private boolean isSelectorItem(ItemStack itemStack) {
        if (itemStack.getType() != Material.NETHER_STAR) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;
        Byte stored = meta.getPersistentDataContainer().get(selectorKey, PersistentDataType.BYTE);
        return stored != null && stored == (byte) 1;
    }

    private void openSelection(Player player) {
        GamePlayer gamePlayer = Main.game.GetPlayer(player);
        if (gamePlayer == null) {
            player.sendMessage(ChatColor.RED + "You must be in a run to upgrade perks.");
            return;
        }
        player.openInventory(buildUpgradeInventory(gamePlayer));
    }

    private Inventory buildUpgradeInventory(GamePlayer gamePlayer) {
        Inventory inventory = Bukkit.createInventory(null, 54, GUI_TITLE);
        int slot = 10;
        for (PerkType perkType : Main.game.ALL_PERKS.values()) {
            if (!perkType.isLevelPerk()) {
                continue;
            }
            ItemStack icon = buildLevelEntry(perkType, gamePlayer);
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot, icon);
            slot++;
            if ((slot + 1) % 9 == 0) {
                slot += 2;
            }
        }
        fillEmpty(inventory);
        return inventory;
    }

    private ItemStack buildLevelEntry(PerkType perkType, GamePlayer player) {
        int currentLevel = player.PLAYER_PERKS.getPerkLevel(perkType);
        if (currentLevel == 0) {
            ItemStack base = perkType.buildShopIcon();
            ItemMeta meta = base.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(meta.getLore());
                lore.add(" ");
                lore.add(ChatColor.RED + "Unlock this perk at the boss shop first.");
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(PERK_TYPE, PersistentDataType.STRING, perkType.displayName);
                base.setItemMeta(meta);
            }
            return base;
        }
        int maxLevel = perkType.getMaxLevel();
        int nextLevel = Math.min(maxLevel, currentLevel + 1);
        int nextCost = currentLevel >= maxLevel ? 0 : perkType.costForLevel(nextLevel);
        return perkType.buildLevelMenuIcon(currentLevel, maxLevel, nextCost);
    }

    private void fillEmpty(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler.clone());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (isSelectionInventory(event.getView())) {
            handleUpgradeClick(event);
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (isProtectedSlot(event.getSlot()) && isProtectedItem(event.getCurrentItem()))
        {
            event.setCancelled(true);
            player.updateInventory();
        }


    }

    private boolean isSelectionInventory(InventoryView view) {
        return GUI_TITLE.equals(view.getTitle());
    }

    private PerkType resolvePerkType(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String stored = meta.getPersistentDataContainer().get(PERK_TYPE, PersistentDataType.STRING);
        if (stored == null) return null;
        return Main.game.ALL_PERKS.get(stored);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isSelectionInventory(event.getView())) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryView view = event.getView();
        for (int rawSlot : event.getRawSlots()) {
            Inventory targetInventory = view.getInventory(rawSlot);
            if (targetInventory == null || targetInventory != player.getInventory()) {
                continue;
            }
            int playerSlot = view.convertSlot(rawSlot);
            if (isProtectedSlot(playerSlot)) {
                ItemStack protectedItem = player.getInventory().getItem(playerSlot);
                if (isProtectedItem(protectedItem)) {
                    event.setCancelled(true);
                    player.updateInventory();
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (isProtectedItem(dropped)) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (isProtectedItem(event.getMainHandItem()) || isProtectedItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    private void handleUpgradeClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        PerkType perkType = resolvePerkType(clicked);
        if (perkType == null || !perkType.isLevelPerk()) return;
        Player player = (Player) event.getWhoClicked();
        GamePlayer gamePlayer = Main.game.GetPlayer(player);
        if (gamePlayer == null) {
            player.sendMessage(ChatColor.RED + "You must join the game before upgrading perks.");
            player.closeInventory();
            return;
        }
        if (!gamePlayer.PLAYER_PERKS.hasPerk(perkType)) {
            player.sendMessage(ChatColor.YELLOW + "Unlock " + perkType.displayName + ChatColor.YELLOW + " at the boss shop first.");
            return;
        }
        int currentLevel = gamePlayer.PLAYER_PERKS.getPerkLevel(perkType);
        int maxLevel = perkType.getMaxLevel();
        if (currentLevel >= maxLevel) {
            player.sendMessage(ChatColor.GRAY + perkType.displayName + ChatColor.GRAY + " is already max level.");
            return;
        }
        int nextLevel = currentLevel + 1;
        int cost = perkType.costForLevel(nextLevel);
        if (cost <= 0) {
            player.sendMessage(ChatColor.RED + "This perk cannot be upgraded further.");
            return;
        }
        if (!gamePlayer.spendCoins(cost)) {
            player.sendMessage(ChatColor.RED + "You need " + cost + " coins to upgrade " + perkType.displayName + ChatColor.RED + ".");
            return;
        }
        boolean upgraded = gamePlayer.PLAYER_PERKS.levelUpPerk(perkType);
        if (!upgraded) {
            player.sendMessage(ChatColor.RED + "Upgrade failed. Coins refunded.");
            gamePlayer.addCoins(cost);
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Upgraded " + perkType.displayName + ChatColor.GREEN + " to level " + nextLevel + " for " + ChatColor.GOLD + cost + ChatColor.GREEN + " coins.");
        player.openInventory(buildUpgradeInventory(gamePlayer));
    }

    private boolean isProtectedSlot(int slot) {
        return slot == SELECTOR_SLOT || Perks.isPerkSlot(slot);
    }

    private boolean isProtectedItem(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        if (isSelectorItem(stack)) {
            return true;
        }
        return isPerkUiItem(stack);
    }

    private boolean isPerkUiItem(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        if (stack.isSimilar(General.Perk_EmptySlotItem)) {
            return true;
        }
        if(stack.getType() == Material.LIME_DYE)
        {
            return true;
        }
        return false;
    }

//    private ItemStack withCostLore(ItemStack original, int cost) {
//        ItemStack stack = original.clone();
//        ItemMeta meta = stack.getItemMeta();
//        if (meta == null) {
//            return stack;
//        }
//        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
//        lore.add(" ");
//        lore.add(ChatColor.GOLD + "Cost: " + ChatColor.YELLOW + cost + " coins");
//        meta.setLore(lore);
//        stack.setItemMeta(meta);
//        return stack;
//    }

//    private PerkType matchIndicator(ItemStack stack) {
//        if (stack == null) {
//            return null;
//        }
//        for (PerkType perkType : PerkType.values()) {
//            if (stack.isSimilar(perkType.buildIndicatorIcon())) {
//                return perkType;
//            }
//        }
//        return null;
//    }

//    private void handlePerkRemovalClick(Player player, PerkType perkType) {
//        GamePlayer gamePlayer = Main.game.GetPlayer(player);
//        if (gamePlayer == null) {
//            player.sendMessage(ChatColor.RED + "Join the game before managing perks.");
//            return;
//        }
//        if (!gamePlayer.PLAYER_PERKS.hasPerk(perkType)) {
//            player.sendMessage(ChatColor.YELLOW + "You do not own " + perkType.displayName + ChatColor.YELLOW + ".");
//            return;
//        }
//        UUID playerId = player.getUniqueId();
//        long now = System.currentTimeMillis();
//        RemovalPrompt prompt = pendingRemoval.get(playerId);
//        if (prompt != null && prompt.perkType == perkType && prompt.expiresAt >= now) {
//            pendingRemoval.remove(playerId);
//            boolean removed = gamePlayer.PLAYER_PERKS.removePerk(perkType);
//            if (removed) {
//                player.sendMessage(ChatColor.GRAY + "Removed perk " + ChatColor.GOLD + perkType.displayName + ChatColor.GRAY + ". Coins are not refunded.");
//            } else {
//                player.sendMessage(ChatColor.RED + "Unable to remove perk right now.");
//            }
//            player.updateInventory();
//            return;
//        }
//        pendingRemoval.put(playerId, new RemovalPrompt(perkType, now + PERK_REMOVE_CONFIRM_WINDOW_MS));
//        player.sendMessage(ChatColor.YELLOW + "Right-click " + perkType.displayName + ChatColor.YELLOW + " again within 1.5s to remove it. No refund provided.");
//    }
//
//    private record RemovalPrompt(PerkType perkType, long expiresAt) {
//    }
}
