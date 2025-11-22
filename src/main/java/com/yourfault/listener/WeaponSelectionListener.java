package com.yourfault.listener;

import com.yourfault.Items.weapons;
import com.yourfault.handler.WeaponSelectionHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class WeaponSelectionListener implements Listener {

    private final WeaponSelectionHandler weaponSelectionHandler;
    private static final String GUI_TITLE = "Select your default weapon";
    private final JavaPlugin plugin;
    private final NamespacedKey weaponKey;
    private final Set<UUID> pendingSelection = new HashSet<>();

    public WeaponSelectionListener(JavaPlugin plugin, WeaponSelectionHandler weaponSelectionHandler) {
        this.plugin = plugin;
        this.weaponSelectionHandler = weaponSelectionHandler;
        this.weaponKey = new NamespacedKey(plugin, "weapon_option");
    }

    public void openSelection(Player player) {
        if (player == null || !player.isOnline()) return;
        pendingSelection.add(player.getUniqueId());
        player.openInventory(buildInventory());
    }

    private Inventory buildInventory() {
        Inventory inventory = Bukkit.createInventory(null, 9, GUI_TITLE);
        for (WeaponType type : WeaponType.values()) {
            inventory.setItem(type.menuSlot(), type.buildMenuIcon(weaponKey));
        }
        fillEmpty(inventory);
        return inventory;
    }

    private void fillEmpty(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler.clone());
            }
        }
    }

    private boolean shouldForceSelection(Player player) {
        return !weaponSelectionHandler.hasSelectedWeapon(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isSelectionInventory(event.getView())) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        WeaponType weaponType = resolveWeaponType(clicked);
        if (weaponType == null) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        if (!pendingSelection.remove(uuid)) return;
        player.closeInventory();
        weaponSelectionHandler.handleWeaponSelection(player, weaponType);
        if (shouldForceSelection(player)) {
            openSelection(player);
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
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (weaponSelectionHandler.hasSelectedWeapon(uuid)) {
            pendingSelection.remove(uuid);
            return;
        }
        pendingSelection.add(uuid);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player refreshed = Bukkit.getPlayer(uuid);
            if (refreshed == null || !refreshed.isOnline()) {
                pendingSelection.remove(uuid);
                return;
            }
            if (weaponSelectionHandler.hasSelectedWeapon(uuid)) {
                pendingSelection.remove(uuid);
                return;
            }
            openSelection(refreshed);
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingSelection.remove(event.getPlayer().getUniqueId());
    }

    private boolean isSelectionInventory(InventoryView view) {
        return GUI_TITLE.equals(view.getTitle());
    }

    private WeaponType resolveWeaponType(ItemStack stack) {
        var meta = stack.getItemMeta();
        if (meta == null) return null;
        var container = meta.getPersistentDataContainer();
        String stored = container.get(weaponKey, PersistentDataType.STRING);
        if (stored == null) return null;
        try {
            return WeaponType.valueOf(stored);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public enum WeaponType {
        Excalibur(() -> weapons.EXCALIBUR.clone(), 3);

        private final Supplier<ItemStack> templateSupplier;
        private final int menuSlot;

        WeaponType(Supplier<ItemStack> templateSupplier, int menuSlot) {
            this.templateSupplier = templateSupplier;
            this.menuSlot = menuSlot;
        }

        public int menuSlot() {
            return menuSlot;
        }

        public String displayName() {
            ItemStack stack = templateSupplier.get();
            var meta = stack.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
            return name();
        }

        public ItemStack buildMenuIcon(NamespacedKey key) {
            ItemStack stack = templateSupplier.get();
            var meta = stack.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, name());
                stack.setItemMeta(meta);
            }
            return stack;
        }

        public ItemStack createCombatItem() {
            return templateSupplier.get();
        }
    }
}
