package com.yourfault.listener;

import java.util.ArrayList;
import java.util.List;

import com.yourfault.Items.gui.General;
import com.yourfault.Main;
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

public class PerkSelectionListener implements Listener {
    private static final int SELECTOR_SLOT = 8;
    private static final String GUI_TITLE = "Select your perk";

    private final NamespacedKey perkKey;
    private final NamespacedKey selectorKey;
    private final ItemStack selectorItem;

    public PerkSelectionListener(JavaPlugin plugin) {
        this.perkKey = new NamespacedKey(plugin, "perk_option");
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
        player.openInventory(buildInventory());
    }

    private Inventory buildInventory() {
        Inventory inventory = Bukkit.createInventory(null, 27, GUI_TITLE);
        for (PerkType perkType : PerkType.values()) {
            ItemStack icon = perkType.buildMenuIcon(perkKey);
            inventory.setItem(perkType.menuSlot(), withCostLore(icon, perkType.coinCost()));
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
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !clickedInventory.equals(player.getInventory())) {
            handleHotbarSwap(event);
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }

        int slot = event.getSlot();
        if (slot == SELECTOR_SLOT && isSelectorItem(clicked)) {
            event.setCancelled(true);
        }
    }

    private boolean isSelectionInventory(InventoryView view) {
        return GUI_TITLE.equals(view.getTitle());
    }

    private PerkType resolvePerkType(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String stored = meta.getPersistentDataContainer().get(perkKey, PersistentDataType.STRING);
        if (stored == null) return null;
        try {
            return PerkType.valueOf(stored);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isSelectionInventory(event.getView())) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                continue;
            }
            int playerSlot = rawSlot - topSize;
            if (playerSlot == SELECTOR_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (isSelectorItem(dropped)) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (isSelectorItem(event.getMainHandItem()) || isSelectorItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
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
            player.sendMessage(ChatColor.YELLOW + "You already have " + perkType.displayName() + ChatColor.YELLOW + ".");
            player.closeInventory();
            return;
        }

        int cost = perkType.coinCost();
        if (!gamePlayer.spendCoins(cost)) {
            player.sendMessage(ChatColor.RED + "You need " + cost + " coins to buy " + perkType.displayName() + ChatColor.RED + ".");
            player.closeInventory();
            return;
        }

        boolean applied = gamePlayer.PLAYER_PERKS.applyPerkSelection(perkType);
        if (applied) {
            player.sendMessage(ChatColor.GREEN + perkType.displayName() + ChatColor.GRAY + " purchased for " + ChatColor.GOLD + cost + ChatColor.GRAY + " coins.");
        } else {
            gamePlayer.addCoins(cost); // refund if application failed for any reason
            player.sendMessage(ChatColor.RED + "Unable to equip perk. Your coins were refunded.");
        }
        player.closeInventory();
    }

    private void handleHotbarSwap(InventoryClickEvent event) {
        int hotbarSlot = event.getHotbarButton();
        if (hotbarSlot == SELECTOR_SLOT) {
            event.setCancelled(true);
        }
    }

//    private boolean isProtectedSlot(int slot) {
//        return slot == SELECTOR_SLOT || Perks.isPerkSlot(slot);
//    }
//
//    private boolean isProtectedItem(ItemStack stack) {
//        if (stack == null) {
//            return false;
//        }
//        if (isSelectorItem(stack)) {
//            return true;
//        }
//        return isPerkUiItem(stack);
//    }
//
//    private boolean isPerkUiItem(ItemStack stack) {
//        if (stack == null) {
//            return false;
//        }
//        if (stack.isSimilar(General.Perk_EmptySlotItem)) {
//            return true;
//        }
//        for (PerkType perkType : PerkType.values()) {
//            if (stack.isSimilar(perkType.buildIndicatorIcon())) {
//                return true;
//            }
//        }
//        return false;
//    }

    private ItemStack withCostLore(ItemStack original, int cost) {
        ItemStack stack = original.clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
        lore.add(" ");
        lore.add(ChatColor.GOLD + "Cost: " + ChatColor.YELLOW + cost + " coins");
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }
}
