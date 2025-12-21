package com.yourfault.weapon;

import com.yourfault.Items.weapons;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.weapon.Excalibur.Excalibur_Main;
import com.yourfault.weapon.ThouserHand.Thouser_Main;
import net.minecraft.world.item.component.Weapon;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public enum WeaponType {
        Excalibur(weapons.EXCALIBUR()::clone, Excalibur_Main.class,0,5,20,100.0f, 50.0f, 20.0f,"\ue101","excalibur"),
        ThouserHand(weapons.THOUSER()::clone, Thouser_Main.class,5,10,20,80.0f, 70.0f, 15.0f,"\ue102","dragonslayer"),;

        private static Function<String, Supplier<ItemStack>> resolver;

        private final Supplier<ItemStack> templateSupplier;
        public final float Health;
        public final float Mana;
        public final float Defense;
        public final String weaponNBT;
        public final String weaponIcon;
        public final float lc_mana;
        public final float rc_mana;
        public final float fc_mana;
        private final Class<? extends WeaponAttachment> weaponclass;


        WeaponType(Supplier<ItemStack> templateSupplier, Class<? extends WeaponAttachment> object, float lc_mana, float rc_mana, float fc_mana, float Health, float Mana, float Defense, String weaponicon, String weaponNBT) {

            this.lc_mana = lc_mana;
            this.rc_mana = rc_mana;
            this.fc_mana = fc_mana;
            this.templateSupplier = templateSupplier;
            this.Health = Health;
            this.Mana = Mana;
            this.Defense = Defense;
            this.weaponIcon = weaponicon;
            this.weaponNBT = weaponNBT;
            this.weaponclass = object;
        }
        public WeaponAttachment createAttachmentInstance(GamePlayer player)
        {
            try {
                return weaponclass.getDeclaredConstructor(GamePlayer.class).newInstance(player);
            } catch (Exception e) {
                throw new IllegalStateException("Fail to create weapon object");
            }
        }

        public ItemStack GetItem() {
            return templateSupplier.get().clone();

        }

}