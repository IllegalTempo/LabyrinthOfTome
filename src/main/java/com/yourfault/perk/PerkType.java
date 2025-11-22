package com.yourfault.perk;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import com.yourfault.perk.quickdraw.QuickdrawPerk;
import com.yourfault.perk.sharpshooter.SharpshooterPerk;

public enum PerkType {
    QUICKDRAW(
        QuickdrawPerk.DISPLAY_NAME,
        QuickdrawPerk.DESCRIPTION,
        QuickdrawPerk.MENU_SLOT,
        QuickdrawPerk::buildMenuIcon,
        QuickdrawPerk::buildIndicatorIcon
    ),
    SHARPSHOOTER(
        SharpshooterPerk.DISPLAY_NAME,
        SharpshooterPerk.DESCRIPTION,
        SharpshooterPerk.MENU_SLOT,
        SharpshooterPerk::buildMenuIcon,
        SharpshooterPerk::buildIndicatorIcon
    );

    private final String displayName;
    private final String description;
    private final int menuSlot;
    private final BiFunction<NamespacedKey, String, ItemStack> menuIconFactory;
    private final Supplier<ItemStack> indicatorSupplier;

    PerkType(String displayName,
         String description,
         int menuSlot,
         BiFunction<NamespacedKey, String, ItemStack> menuIconFactory,
         Supplier<ItemStack> indicatorSupplier) {
        this.displayName = displayName;
        this.description = description;
        this.menuSlot = menuSlot;
    this.menuIconFactory = menuIconFactory;
    this.indicatorSupplier = indicatorSupplier;
    }

    public int menuSlot() {
        return menuSlot;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public ItemStack buildMenuIcon(NamespacedKey key) {
        return menuIconFactory.apply(key, name());
    }

    public ItemStack buildIndicatorIcon() {
        return indicatorSupplier.get();
    }
}
