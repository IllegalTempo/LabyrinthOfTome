package com.yourfault.weapon;

import com.yourfault.Items.weapons;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public enum WeaponType {
        Excalibur(weapons.ITEM_MAP.get("excalibur")::clone, 100.0f, 50.0f, 20.0f,"\ue101","excalibur"),;

        private final Supplier<ItemStack> templateSupplier;
        public float Health;
        public float Mana;
        public float Defense;
        public String weaponNBT;
        public String weaponIcon;

        WeaponType(Supplier<ItemStack> templateSupplier,float Health,float Mana,float Defense,String weaponicon,String weaponNBT) {
            this.templateSupplier = templateSupplier;
            this.Health = Health;
            this.Mana = Mana;
            this.Defense = Defense;
            this.weaponIcon = weaponicon;
            this.weaponNBT = weaponNBT;
        }
        public ItemStack GetItem() {
            return templateSupplier.get().clone();

        }
    }