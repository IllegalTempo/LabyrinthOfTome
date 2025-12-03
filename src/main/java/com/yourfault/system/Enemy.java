package com.yourfault.system;

import com.yourfault.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockType;
import org.bukkit.entity.LivingEntity;

import java.util.Locale;

import static com.yourfault.Main.plugin;

public abstract class Enemy {
    public LivingEntity entity;
    public float HEALTH;
    public float DEFENSE;
    public float MaxHealth;
    public String DisplayName;
    public float scale = 1;

    public Enemy(LivingEntity entity, float health, float MaxHealth, float defense, String displayName)
    {
        this.HEALTH = health;
        this.MaxHealth = MaxHealth;
        this.DEFENSE = defense;
        this.DisplayName = displayName;
        this.entity = entity;
        entity.setCustomNameVisible(true);
        updateDisplay();

        Main.game.ENEMY_LIST.put(entity.getUniqueId(),this);
        startUpdate();
        Main.game.EnemyTeam.addEntity(entity);
        entity.setGlowing(true);
    }
    private void updateDisplay() {

        String label = ChatColor.RED + String.format(Locale.US, "%.0f/%.0f HP ", HEALTH, MaxHealth)
                + ChatColor.GRAY + DisplayName;
        entity.setCustomName(label);

    }
    public void startUpdate()
    {
        Bukkit.getScheduler().runTaskTimer(plugin,()->{
            tick();
            if(scale > 1)
            {
                entity.getAttribute(Attribute.SCALE).setBaseValue(1);
            }
        },0L,1L);
    }
    public abstract void tick();
    public abstract void OnAttack();
    public abstract void OnDealDamage();
    public void OnBeingDamage(float damage)
    {
        HEALTH -= damage;
        entity.damage(0);
        updateDisplay();
        if(HEALTH <= 0)
        {
            Destroy();
        }
        entity.getAttribute(Attribute.SCALE).setBaseValue(1.1);
        scale = 1.1f;
        entity.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation().add(0,1,0),50,0.3,0.6,0.3,0, BlockType.REDSTONE_BLOCK.createBlockData() );

    }
    public void Destroy()
    {
        entity.remove();
        Main.game.ENEMY_LIST.remove(entity.getUniqueId());
        if (Main.game.getWaveManager() != null) {
            Main.game.getWaveManager().handleEnemyDeath(entity.getUniqueId(), null);
        }
    }
}
