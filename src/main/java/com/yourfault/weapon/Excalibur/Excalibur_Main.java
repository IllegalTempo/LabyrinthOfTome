package com.yourfault.weapon.Excalibur;

import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.utils.AnimationInfo;
import com.yourfault.utils.ItemUtil;
import com.yourfault.weapon.WeaponType;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static com.yourfault.Main.plugin;

public class Excalibur_Main implements Listener {
    private static final AnimationInfo ANIMATION_LC = new AnimationInfo("animation_lc",15L);
    private static final AnimationInfo ANIMATION_RC = new AnimationInfo("animation_rc",10L);
    private static final AnimationInfo ANIMATION_FC = new AnimationInfo("animation_fc",40L);

    @EventHandler
    public void OnRightClick(PlayerInteractEvent e)
    {
        GamePlayer gamePlayer = Main.game.GetPlayer(e.getPlayer().getUniqueId());
        if(!gamePlayer.ActionReady(WeaponType.Excalibur,5f)) return;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            gamePlayer.playAnimation(ANIMATION_RC);
            gamePlayer.ChangeMana(-5f);
            Location locz = e.getPlayer().getLocation();

            e.getPlayer().getWorld().playSound(Sound.sound(Key.key("minecraft:entity.player.attack.sweep"),Sound.Source.PLAYER,1.0f,0.5f),locz.getX(),locz.getY(),locz.getZ());


            // sweep the spawn directions from +22.5° to -22.5° over i = 5..10
            for(int i = 5 ; i <= 10; i++)
            {
                int finalI = i;

                Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                    Location loc = gamePlayer.GetForward(1);
                    //Location loc = gamePlayer.getLocationRelativeToPlayer(new Vector(finalI -7,0,0));
                    loc.setYaw(loc.getYaw() + (finalI-7.5f) * 20f);

                    new Sword_Aura(loc, 10f,gamePlayer);
                }, i);
            }

        }
        if(e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)
        {
            onMeleeAttack(gamePlayer);

        }


    }
    private void onMeleeAttack(GamePlayer player)
    {
        player.playAnimation(ANIMATION_LC);
        Location locz = player.getMinecraftPlayer().getLocation();
        locz.getWorld().playSound(Sound.sound(Key.key("minecraft:entity.player.attack.sweep"),Sound.Source.PLAYER,1.0f,0.5f),locz.getX(),locz.getY(),locz.getZ());

    }
    @EventHandler
    public void OnMelee(PrePlayerAttackEntityEvent e)
    {
        GamePlayer gamePlayer = Main.game.GetPlayer(e.getPlayer().getUniqueId());
        if(!gamePlayer.ActionReady(WeaponType.Excalibur,0f)) return;

        Location locz = e.getPlayer().getLocation();
        onMeleeAttack(gamePlayer);




    }
    @EventHandler
    public void on_F_clicked(PlayerSwapHandItemsEvent e)
    {
        e.setCancelled(true);
        GamePlayer gamePlayer = Main.game.GetPlayer(e.getPlayer().getUniqueId());
        if(!gamePlayer.ActionReady(WeaponType.Excalibur,20f)) return;

        gamePlayer.playAnimation(ANIMATION_FC);
        gamePlayer.ChangeMana(-20f);
        Location locz = e.getPlayer().getLocation();
        double grav = e.getPlayer().getAttribute(Attribute.GRAVITY).getValue();
        e.getPlayer().getAttribute(Attribute.GRAVITY).setBaseValue(-0.01);
        Bukkit.getScheduler().runTaskLater(plugin, ()-> {
            e.getPlayer().getAttribute(Attribute.GRAVITY).setBaseValue(grav);

        }, ANIMATION_FC.durationTicks());
        Bukkit.getScheduler().runTaskLater(plugin, ()-> {
            e.getPlayer().getAttribute(Attribute.GRAVITY).setBaseValue(1);
            locz.getWorld().playSound(Sound.sound(Key.key("minecraft:entity.ender_dragon.ambient"),Sound.Source.PLAYER,1.0f,2.0f),locz.getX(),locz.getY(),locz.getZ());


        }, 20);
        // capture the effect center and owner now so the scheduled task can use them
        Bukkit.getScheduler().runTaskLater(plugin, ()-> {
            Location effectCenter = e.getPlayer().getLocation().clone().add(0, 1.0, 0);

            F_MainEffect(effectCenter, gamePlayer);

        }, 30L);

    }
    private void F_MainEffect(Location center, GamePlayer owner)
    {

        Player bukkitOwner = owner != null ? owner.getMinecraftPlayer() : null;
        if (bukkitOwner == null) {
            return;
        }
        // spawn 16 projectiles arranged in a circle around `center`, each facing outward
        final int count = 32;
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
