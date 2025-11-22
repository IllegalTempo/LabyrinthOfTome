package com.yourfault.weapon.Excalibur;

import com.yourfault.Main;
import com.yourfault.weapon.General.Projectile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

public class Sword_Aura extends Projectile {
    public Sword_Aura(Location StartLocation, float damage) {
        super(StartLocation, 1, damage, 1, false, new ItemStack(Material.LIGHT_BLUE_WOOL), 20);

    }
    @Override
    public void Update()
    {
        super.Update();
    }
    @Override
    public void Projectile_OnHit()
    {
        super.Projectile_OnHit();

    }
    @Override
    public void Monster_OnHit(Entity monster)
    {

    }
    @Override
    public void Destroy()
    {
        super.Destroy();
    }
}
