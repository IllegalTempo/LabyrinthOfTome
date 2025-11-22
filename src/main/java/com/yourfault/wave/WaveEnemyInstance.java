package com.yourfault.wave;

import org.bukkit.entity.LivingEntity;

import com.yourfault.system.Enemy;

public class WaveEnemyInstance extends Enemy {
    private final WaveEnemyType type;
    private final double scaledDamage;
    private final double scaledDefense;

    public WaveEnemyInstance(LivingEntity entity, float health, float maxHealth, float defense, WaveEnemyType type, double scaledDamage) {
        super(entity, health, maxHealth, defense, type.name());
        this.type = type;
        this.scaledDamage = scaledDamage;
        this.scaledDefense = defense;
    }

    public WaveEnemyType getType() {
        return type;
    }

    public double getScaledDamage() {
        return scaledDamage;
    }

    public double getScaledDefense() {
        return scaledDefense;
    }

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}
}
