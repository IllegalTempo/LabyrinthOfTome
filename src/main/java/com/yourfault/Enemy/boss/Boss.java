package com.yourfault.Enemy.boss;

import com.yourfault.system.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class Boss extends Enemy implements Listener{
    public Boss(LivingEntity entity, float health, float MaxHealth, float defense, String displayName) {
        super(entity, health, MaxHealth, defense, displayName);
    }

    @EventHandler
    public void onBossDamaged(EntityDamageByEntityEvent)
    {

    }

    @Override
    public void tick() {

    }

    @Override
    public void OnAttack() {

    }

    @Override
    public void OnDealDamage() {

    }
}
