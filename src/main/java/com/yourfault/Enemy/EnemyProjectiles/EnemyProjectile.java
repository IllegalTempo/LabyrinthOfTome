package com.yourfault.Enemy.EnemyProjectiles;

import com.yourfault.Main;
import com.yourfault.Enemy.Enemy;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.Projectile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;

public abstract class EnemyProjectile extends Projectile {
    public Enemy owner;


    public EnemyProjectile(Location eyeLocation, float speed, float damage, float radius, boolean UseGravity, ItemStack projectileItem, float LastFor, Enemy owner)
    {
        super(eyeLocation, speed, damage, radius, UseGravity, projectileItem,LastFor,null);
        this.owner = owner;

    }
    public EnemyProjectile(Location eyeLocation, float speed, float damage, float radius, boolean UseGravity, float LastFor, Enemy owner)
    {
        this(eyeLocation, speed, damage, radius, UseGravity, new ItemStack(org.bukkit.Material.AIR), LastFor,owner);
    }
    @Override
    public void Update()
    {
        UpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                ChildUpdate();
                if(age == LastFor) Destroy();
                Vector travel = entity.getLocation().getDirection().multiply(speed);
                age += 1;
                if(UseGravity) travel.subtract(Main.game.Gravity);
                entity.teleport(entity.getLocation().add(travel));
                Collection<? extends Player> online = Bukkit.getOnlinePlayers();
                boolean hit = false;
                for(Player p : online)
                {
                    if(p.getWorld() != entity.getWorld()) continue;
                    if(p.getLocation().distanceSquared(getDisplayedLocation()) <= radius * radius)
                    {
                        hit = true;
                        GamePlayer pl = Main.game.GetPlayer(p);
                        pl.damage(damage);
                        Player_OnHit(pl);
                    }
                }

                if(hit) Projectile_OnHit();




            }
        };
        UpdateTask.runTaskTimer(Main.plugin, 0L, 1L);
    }
    public void Player_OnHit(GamePlayer p)
    {

    }

}
