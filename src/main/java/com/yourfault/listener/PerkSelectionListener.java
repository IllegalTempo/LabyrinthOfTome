package com.yourfault.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.yourfault.Items.gui.General;
import com.yourfault.Main;
import com.yourfault.NBT_namespace;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import com.yourfault.perks.PerkType;

import static com.yourfault.NBT_namespace.PERK_TYPE;

public class PerkSelectionListener implements Listener {
    private static final int SELECTOR_SLOT = 8;
    private static final String GUI_TITLE = "Select your perk";
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
            meta.setDisplayName(ChatColor.GREEN + "Buy Perks");
            meta.setLore(List.of(ChatColor.GRAY + "Right-click to open the perk menu."));
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
        player.openInventory(Perk_Shop());
    }

    private Inventory Perk_Shop() {
        //Build Perk display in inventory!
        Inventory inventory = Bukkit.createInventory(null, 27, GUI_TITLE);
        for (PerkType perkType : Main.game.ALL_PERKS.values())
        {
            inventory.setItem(perkType.menuSlot, perkType.shop_getPerkIcon());
        }
        fillEmpty(inventory);
        return inventory;
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
            handleSelectionClick(event);
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

    private void handleSelectionClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        PerkType perkType = resolvePerkType(clicked);
        if (perkType == null) return;
        Player player = (Player) event.getWhoClicked();
        GamePlayer gamePlayer = Main.game.GetPlayer(player);
        if (gamePlayer == null) {
            player.sendMessage(ChatColor.RED + "You must join the game before selecting perks.");
            player.closeInventory();
            return;
        }
        if (gamePlayer.PLAYER_PERKS.hasPerk(perkType)) {
            player.sendMessage(ChatColor.YELLOW + "You already have " + perkType.displayName + ChatColor.YELLOW + ".");
            player.closeInventory();
            return;
        }

        int cost = perkType.cost;
        if (!gamePlayer.spendCoins(cost)) {
            player.sendMessage(ChatColor.RED + "You need " + cost + " coins to buy " + perkType.displayName + ChatColor.RED + ".");
            player.closeInventory();
            return;
        }

        boolean applied = gamePlayer.PLAYER_PERKS.applyPerkSelection(perkType);
        if (applied) {
            player.sendMessage(ChatColor.GREEN + perkType.displayName + ChatColor.GRAY + " purchased for " + ChatColor.GOLD + cost + ChatColor.GRAY + " coins.");
        } else {
            gamePlayer.addCoins(cost); // refund if application failed for any reason
            player.sendMessage(ChatColor.RED + "Unable to equip perk. Your coins were refunded.");
        }
        player.closeInventory();
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
