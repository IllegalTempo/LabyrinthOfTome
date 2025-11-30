package com.yourfault.system;

import com.yourfault.Main;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;

import java.util.Locale;

public abstract class Enemy {
    public LivingEntity entity;
    public float HEALTH;
    public float DEFENSE;
    public float MaxHealth;
    public String DisplayName;

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


    }
    private void updateDisplay() {

        String label = ChatColor.RED + String.format(Locale.US, "%.0f/%.0f HP ", HEALTH, MaxHealth)
                + ChatColor.GRAY + DisplayName;
        entity.setCustomName(label);

    }
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
