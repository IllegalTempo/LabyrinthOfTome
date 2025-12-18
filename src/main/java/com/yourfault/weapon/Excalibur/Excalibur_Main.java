package com.yourfault.weapon.Excalibur;

import com.yourfault.projectiles.HolySword;
import com.yourfault.projectiles.Sword_Aura;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.utils.AnimationInfo;
import com.yourfault.weapon.WeaponListener;
import com.yourfault.weapon.WeaponType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;

import static com.yourfault.Main.plugin;

public class Excalibur_Main extends WeaponListener {
    private static final AnimationInfo ANIMATION_LC = new AnimationInfo("animation_lc",15L);
    private static final AnimationInfo ANIMATION_LC2 = new AnimationInfo("animation_lc2",20L);

    private static final AnimationInfo ANIMATION_RC = new AnimationInfo("animation_rc",10L);
    private static final AnimationInfo ANIMATION_FC = new AnimationInfo("animation_fc",40L);

    public Excalibur_Main() {
        super(WeaponType.Excalibur, 0, 5, 20);
    }


    private void onMeleeAttack(GamePlayer player)
    {
        if(player.weapondata[0] == 0)
        {
            player.playAnimation(ANIMATION_LC);
            Location locz = player.getMinecraftPlayer().getLocation();
            locz.getWorld().playSound(Sound.sound(Key.key("minecraft:entity.player.attack.sweep"),Sound.Source.PLAYER,1.0f,0.5f),locz.getX(),locz.getY(),locz.getZ());

        } else {
            player.playAnimation(ANIMATION_LC2);
            Location locz = player.getMinecraftPlayer().getLocation();
            Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                // Particle effect
                locz.getWorld().playSound(Sound.sound(Key.key("minecraft:entity.player.attack.sweep"),Sound.Source.PLAYER,1.0f,0.3f),locz.getX(),locz.getY(),locz.getZ());

            }, 5L);
        }
        player.weapondata[0] = (player.weapondata[0] + 1) % 2;


    }


    @Override
    public void LC(GamePlayer player) {
        onMeleeAttack(player);
    }

    @Override
    public void RC(GamePlayer player) {
        player.playAnimation(ANIMATION_RC);
        player.ChangeMana(-rc_mana);
        Location locz = player.getMinecraftPlayer().getLocation();

        player.MINECRAFT_PLAYER.getWorld().playSound(Sound.sound(Key.key("minecraft:entity.player.attack.sweep"),Sound.Source.PLAYER,1.0f,0.5f),locz.getX(),locz.getY(),locz.getZ());


        int baseCount = 6;
        int projectileCount = Math.max(1, (int)(baseCount * player.projectileMultiplier));
        float totalSpreadDegrees = 100f; // total angular span across all projectiles
        float angleStep = projectileCount > 1 ? totalSpreadDegrees / (projectileCount - 1) : 0f;
        double midpoint = (projectileCount - 1) / 2.0;
        int startDelay = 5;

        for (int i = 0; i < projectileCount; i++) {
            int finalI = i;
            float angleOffset = (float) ((finalI - midpoint) * angleStep);
            long delay = (startDelay + finalI);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location loc = player.GetForward(1);
                loc.setYaw(loc.getYaw() + angleOffset);
                new Sword_Aura(loc, 10f, player);
            }, delay);
        }
    }

    @Override
    public void FC(GamePlayer player) {
        player.playAnimation(ANIMATION_FC);
        player.ChangeMana(-fc_mana);
        Location locz = player.MINECRAFT_PLAYER.getLocation();
        double grav = player.MINECRAFT_PLAYER.getAttribute(Attribute.GRAVITY).getValue();
        player.MINECRAFT_PLAYER.getAttribute(Attribute.GRAVITY).setBaseValue(-0.01);
        Bukkit.getScheduler().runTaskLater(plugin, ()-> {
            player.MINECRAFT_PLAYER.getAttribute(Attribute.GRAVITY).setBaseValue(grav);

        }, ANIMATION_FC.durationTicks());
        Bukkit.getScheduler().runTaskLater(plugin, ()-> {
            player.MINECRAFT_PLAYER.getAttribute(Attribute.GRAVITY).setBaseValue(1);
            locz.getWorld().playSound(Sound.sound(Key.key("minecraft:entity.ender_dragon.ambient"),Sound.Source.PLAYER,1.0f,2.0f),locz.getX(),locz.getY(),locz.getZ());


        }, 20);
        // capture the effect center and owner now so the scheduled task can use them
        Bukkit.getScheduler().runTaskLater(plugin, ()-> {
            Location effectCenter = player.MINECRAFT_PLAYER.getLocation().clone().add(0, 1.0, 0);

            F_MainEffect(effectCenter, player);

        }, 30L);

    }

    private void F_MainEffect(Location center, GamePlayer owner)
    {

        Player bukkitOwner = owner != null ? owner.getMinecraftPlayer() : null;
        if (bukkitOwner == null) {
            return;
        }
        final int count = (int)(8 * owner.projectileMultiplier);
        final double radius = 2.0;
        final float damage = 6f;

        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            // projectile spawn location slightly above the center
            Location spawnLoc = center.clone();
            // direction outward from the center
            Vector dir = new Vector(x, 0, z).normalize();
            if (dir.lengthSquared() == 0) dir = bukkitOwner.getLocation().getDirection().clone();
            spawnLoc.setDirection(dir);
            // create the projectile (Sword_Aura will handle registration)
            new HolySword(spawnLoc, damage, owner);
        }
    }
}
