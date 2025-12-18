package com.yourfault.projectiles;

import com.yourfault.Main;
import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Objects;

public abstract class Projectile {
    protected float speed;
    protected float damage;
    protected boolean UseGravity;
    protected ItemStack projectileItem;
    protected float age;
    protected float LastFor;
    protected ArmorStand entity;
    protected float radius;
    protected BukkitRunnable UpdateTask;
    protected LabyrinthCreature owner;
    protected boolean pierce = false;

    public Projectile(Location eyeLocation,float speed, float damage, float radius, boolean UseGravity, float LastFor, LabyrinthCreature owner)
    {
        this(eyeLocation,speed,damage,radius,UseGravity,new ItemStack(Material.AIR),LastFor,owner);
    }
    public Projectile(Location eyeLocation,float speed, float damage, float radius, boolean UseGravity, ItemStack projectileItem, float LastFor, LabyrinthCreature owner,boolean pierce)
    {
        this(eyeLocation,speed,damage,radius,UseGravity,projectileItem,LastFor,owner);
        this.pierce = pierce;
    }

    public Projectile(Location eyeLocation,float speed, float damage, float radius, boolean UseGravity, ItemStack projectileItem, float LastFor, LabyrinthCreature owner)
    {
        //speed(b/t), age(ticks)
        //boundingbox is relative to 0 0 0

        this.speed = speed;
        this.UseGravity = UseGravity;
        this.projectileItem = projectileItem;
        this.owner = owner;
        this.damage = damage * owner.damageMultiplier;
        this.LastFor = LastFor + owner.projectileSizeMultiplier * 5f;
        this.radius = radius * owner.projectileSizeMultiplier;

        age = 0;







        entity = eyeLocation.getWorld().spawn(eyeLocation.subtract(0,1,0), ArmorStand.class, e ->{
            e.setVisible(false);
            e.setInvulnerable(true);
            e.setBasePlate(false);
            e.setMarker(true);
            e.setSmall(true);
            if (e.getAttribute(Attribute.SCALE) != null) {
                e.getAttribute(Attribute.SCALE).setBaseValue(owner.projectileSizeMultiplier);
            }

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

    protected Location getDisplayedLocation()
    {
        return entity.getLocation().add(0,1,0);
    }

    public void setDirection(Vector dir) {
        Location loc = entity.getLocation();
        loc.setDirection(dir);
        entity.teleport(loc);
    }
    protected void ChildUpdate(){}
    public void onObstacle(Location loc)
    {
        if(!pierce)
        {
            Destroy();
        }
    }
    public void Update()
    {
        UpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                ChildUpdate();
                if(age == LastFor) {Destroy();return;}
                Vector travel = entity.getLocation().getDirection().multiply(speed);
                age += 1;
                if(UseGravity) travel.subtract(Main.game.Gravity);
                Location newloc = entity.getLocation().add(travel);
                entity.teleport(newloc);
                if (entity.getLocation().getBlock().getType().isSolid()) {
                    onObstacle(newloc.clone());
                }
                Collection<Entity> nearby = entity.getLocation().getWorld().getNearbyEntities(getDisplayedLocation(), radius, radius, radius);
                boolean hit = false;
                for (Entity e : nearby) {
                    LabyrinthCreature creature = Main.game.CREATURE_LIST.get(e.getUniqueId());
                    if(creature == null) continue;
                    if(creature.team == owner.team) return;
                    creature.applyDamage(damage,owner,false);
                    onHit(creature);
                    hit = true;

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
    public void onHit(LabyrinthCreature c)
    {


    }
    public void Destroy()
    {
        UpdateTask.cancel();
        entity.remove();
        Main.game.PROJECTILE_LIST.remove(entity.getUniqueId());
    }

}
