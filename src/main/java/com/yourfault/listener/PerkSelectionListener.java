package com.yourfault.listener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import com.yourfault.perk.PerkType;

public class PerkSelectionListener implements Listener {
    private static final int SELECTOR_SLOT = 8;
    private static final String GUI_TITLE = "Select your perk";

    private final JavaPlugin plugin;
    private final NamespacedKey perkKey;
    private final NamespacedKey selectorKey;
    private final ItemStack selectorItem;
    private final Set<UUID> pendingSelection = new HashSet<>();

    public PerkSelectionListener(JavaPlugin plugin) {
        this.plugin = plugin;
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
        pendingSelection.add(player.getUniqueId());
        player.openInventory(buildInventory());
    }

    private Inventory buildInventory() {
        Inventory inventory = Bukkit.createInventory(null, 27, GUI_TITLE);
        for (PerkType perkType : PerkType.values()) {
            inventory.setItem(perkType.menuSlot(), perkType.buildMenuIcon(perkKey));
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
        if (!isSelectionInventory(event.getView())) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        PerkType perkType = resolvePerkType(clicked);
        if (perkType == null) return;
        Player player = (Player) event.getWhoClicked();
        GamePlayer gamePlayer = Main.game.GetPlayer(player);
        boolean applied = gamePlayer.PLAYER_PERKS.applyPerkSelection(perkType);
        if (applied) {
            player.sendMessage(ChatColor.GREEN + perkType.displayName() + ChatColor.GRAY + " perk equipped!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "You already have " + perkType.displayName() + ChatColor.YELLOW + ".");
        }
        pendingSelection.remove(player.getUniqueId());
        player.closeInventory();
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
        if (!isSelectionInventory(event.getView())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!isSelectionInventory(event.getView())) return;
        pendingSelection.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingSelection.remove(event.getPlayer().getUniqueId());
    }
}
