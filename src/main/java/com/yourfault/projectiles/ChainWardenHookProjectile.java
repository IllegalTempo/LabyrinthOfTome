package com.yourfault.projectiles;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyInstances.ChainWardenEnemy;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ChainWardenHookProjectile extends Projectile {

    public ChainWardenHookProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.2f, 5.0f, 1.5f, false, new ItemStack(Material.IRON_CHAIN), 60, owner);
    }

    @Override
    public void ChildUpdate()
    {
        // Spawn chain particle effect along the projectile's path
        Location loc = this.entity.getLocation();
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 5, 0.1, 0.1, 0.1, 0.05);
        loc.getWorld().playSound(loc, Sound.BLOCK_CHAIN_HIT, 0.2f, 1.0f);

    }


    @Override
    public void onHit(LabyrinthCreature p) {
        if (owner instanceof ChainWardenEnemy && p instanceof GamePlayer) {
            Player pl = ((GamePlayer) p).MINECRAFT_PLAYER;
            ((ChainWardenEnemy) owner).onHookHit(pl); //only players can be hooked
            pl.playSound(pl.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);

        }
        //pl.playSound(p.MINECRAFT_PLAYER.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);

    }
}
