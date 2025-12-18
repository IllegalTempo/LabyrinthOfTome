package com.yourfault.weapon;

import com.yourfault.Items.weapons;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public enum WeaponType {
        Excalibur(weapons.EXCALIBUR()::clone, 100.0f, 50.0f, 20.0f,"\ue101","excalibur"),
        ThouserHand(weapons.THOUSER()::clone, 80.0f, 70.0f, 15.0f,"\ue102","dragonslayer"),;

        private static Function<String, Supplier<ItemStack>> resolver;

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