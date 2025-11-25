package com.yourfault.weapon.General;

import com.yourfault.Main;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import java.util.Collection;
import java.util.Objects;

public abstract class Projectile {
    float speed;
    float damage;
    boolean UseGravity;
    ItemStack projectileItem;
    float age;
    ArmorStand entity;
    float radius;
    private BukkitRunnable UpdateTask;


    public Projectile(Location eyeLocation,float speed, float damage, float radius, boolean UseGravity, ItemStack projectileItem, float LastFor)
    {
        //speed(b/t), age(ticks)
        //boundingbox is relative to 0 0 0
        this.speed = speed;
        this.damage = damage;
        this.UseGravity = UseGravity;
        this.projectileItem = projectileItem;

        age = LastFor;
        this.radius = radius;

        entity = eyeLocation.getWorld().spawn(eyeLocation.subtract(0,1,0), ArmorStand.class, e ->{
            e.setVisible(false);
            e.setInvulnerable(true);
            e.setBasePlate(false);
            e.setMarker(true);
            e.setSmall(true);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                e.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING);
                e.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
            }
            Objects.requireNonNull(e.getEquipment()).setHelmet(projectileItem);
            Vector dir = eyeLocation.getDirection().clone().normalize();
            double pitch = Math.asin(-dir.getY());
            double yaw = Math.atan2(dir.getX(), dir.getZ());
            e.setHeadPose(new EulerAngle(pitch, 0, 0));
            Main.game.PROJECTILE_LIST.put(e.getUniqueId(),this);

        });


        Update();

    }
    public void Update()
    {
        UpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if(age == 0) Destroy();
                Vector travel = entity.getLocation().getDirection().multiply(speed);
                age -= 1;
                if(UseGravity) travel.subtract(Main.game.Gravity);
                entity.teleport(entity.getLocation().add(travel));
                Collection<Entity> nearby = entity.getLocation().getWorld().getNearbyEntities(entity.getLocation(), radius, radius, radius);
                boolean hit = false;
                for (Entity e : nearby) {
                    if (e.getScoreboardTags().contains("enemy")) {
                        Monster_OnHit(e);
                        hit = true;
                    }
                }
                if(hit) Projectile_OnHit();



            }
        };
        UpdateTask.runTaskTimer(Main.plugin, 0L, 1L);
    }
    public void Projectile_OnHit()
    {
        Destroy();
    }
    public void Monster_OnHit(Entity e)
    {

    }
    public void Destroy()
    {
        UpdateTask.cancel();
        entity.remove();
        Main.game.PROJECTILE_LIST.remove(entity.getUniqueId());
    }

}
