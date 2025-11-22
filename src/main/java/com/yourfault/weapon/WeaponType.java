package com.yourfault.weapon;

import com.yourfault.Items.weapons;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.function.Supplier;

public enum WeaponType {
        Excalibur(weapons.EXCALIBUR::clone, 100.0f, 50.0f, 20.0f),;

        private final Supplier<ItemStack> templateSupplier;
        public float Health;
        public float Mana;
        public float Defense;

        WeaponType(Supplier<ItemStack> templateSupplier,float Health,float Mana,float Defense) {
            this.templateSupplier = templateSupplier;
            this.Health = Health;
            this.Mana = Mana;
            this.Defense = Defense;
        }
    }