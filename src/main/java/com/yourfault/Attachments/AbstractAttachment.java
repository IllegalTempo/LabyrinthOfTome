package com.yourfault.Attachments;

import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.Objects;

import static com.yourfault.Main.plugin;

public abstract class AbstractAttachment{
    private Vector relativeOffset;
    private LabyrinthCreature parentCreature;
    private BukkitTask updateTask;
    protected Entity minecraftEntity;

    public AbstractAttachment(LabyrinthCreature parentCreature, Vector relativeOffset, Entity minecraftEntity)
    {
        this.minecraftEntity = minecraftEntity;
        this.parentCreature = parentCreature;
        this.relativeOffset = relativeOffset;
        startUpdate();
    }
    public AbstractAttachment(LabyrinthCreature parentCreature, Vector relativeOffset, ItemStack item)
    {
        this(
                parentCreature,
                relativeOffset,
                parentCreature.minecraftEntity.getWorld().spawn(parentCreature.minecraftEntity.getLocation(), ArmorStand.class, e ->{
            e.setVisible(false);
            e.setInvulnerable(true);
            e.setBasePlate(false);
            e.setMarker(true);
            e.setSmall(true);

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                e.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING);
                e.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
            }
            Objects.requireNonNull(e.getEquipment()).setHelmet(item);
            Vector dir = parentCreature.minecraftEntity.getEyeLocation().getDirection().clone().normalize();
            double pitch = Math.asin(-dir.getY());
            e.setHeadPose(new EulerAngle(pitch, 0, 0));

        }));
    }
    private void startUpdate()
    {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::update,0L,1L);
    }
    protected void update()
    {
        minecraftEntity.teleport(parentCreature.getRelativeLocation(relativeOffset));

    }
    public void destroy()
    {
        if(updateTask != null)
        {
            updateTask.cancel();
        }
        minecraftEntity.remove();
    }
}
