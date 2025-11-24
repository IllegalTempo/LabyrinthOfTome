package com.yourfault.perks;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import com.yourfault.perks.quickdraw.QuickdrawPerk;
import com.yourfault.perks.sharpshooter.SharpshooterPerk;

public enum PerkType {
    QUICKDRAW(
        QuickdrawPerk.DISPLAY_NAME,
        QuickdrawPerk.DESCRIPTION,
        QuickdrawPerk.MENU_SLOT,
        QuickdrawPerk::buildMenuIcon,
        QuickdrawPerk::buildIndicatorIcon,
        1500
    ),
    SHARPSHOOTER(
        SharpshooterPerk.DISPLAY_NAME,
        SharpshooterPerk.DESCRIPTION,
        SharpshooterPerk.MENU_SLOT,
        SharpshooterPerk::buildMenuIcon,
        SharpshooterPerk::buildIndicatorIcon,
        400
    );

    private final String displayName;
    private final String description;
    private final int menuSlot;
    private final BiFunction<NamespacedKey, String, ItemStack> menuIconFactory;
    private final Supplier<ItemStack> indicatorSupplier;
    private final int cost;

    PerkType(String displayName,
         String description,
         int menuSlot,
         BiFunction<NamespacedKey, String, ItemStack> menuIconFactory,
         Supplier<ItemStack> indicatorSupplier,
         int cost) {
        this.displayName = displayName;
        this.description = description;
        this.menuSlot = menuSlot;
        this.menuIconFactory = menuIconFactory;
        this.indicatorSupplier = indicatorSupplier;
        this.cost = cost;
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

    public int coinCost() {
        return cost;
    }
}
