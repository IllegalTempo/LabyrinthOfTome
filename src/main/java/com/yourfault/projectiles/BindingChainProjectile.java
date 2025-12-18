package com.yourfault.projectiles;
import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyInstances.ChainBinderEnemy;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.LabyrinthCreature;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;

public class BindingChainProjectile extends Projectile {

    public BindingChainProjectile(Location eyeLocation, Enemy owner) {
        super(eyeLocation, 1.35f, 6.0f, 1.2f, false, new ItemStack(Material.IRON_CHAIN), 60, owner);
    }

    @Override
    protected void ChildUpdate() {
        entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation(), 3, 0.05, 0.05, 0.05, 0.0);
    }

    @Override
    public void onHit(LabyrinthCreature player) {
        if (owner instanceof ChainBinderEnemy binder && player instanceof GamePlayer) {
            binder.onChainProjectileHit((GamePlayer)player); //todo check
        }
    }
}
