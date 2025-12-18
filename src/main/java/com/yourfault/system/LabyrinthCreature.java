package com.yourfault.system;

import com.yourfault.Main;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public abstract class LabyrinthCreature {
    public float MAX_HEALTH;
    public float MAX_MANA;
    public float DEFENSE;
    public float HEALTH;
    public float MANA;
    public float Speed;
    public float flatDamageBonus = 0.0f;
    public float damageMultiplier = 1.0f;
    //Perk Stats
    public float projectileMultiplier = 1;
    public float projectileSizeMultiplier = 1.0f;
    public float manaRegenRate = 0.2f;
    public int attackSpeed = 0;



    private float temporarySpeedBonus = 0;
    private int speedBoostTicks = 0;


    public LivingEntity minecraftEntity;
    public int team;
    public LabyrinthCreature(LivingEntity entity,float maxHealth, float maxMana, float defense, float speed,int team) {
        this.MAX_HEALTH = maxHealth;
        this.HEALTH = maxHealth;
        this.MAX_MANA = maxMana;
        this.MANA = maxMana;
        this.DEFENSE = defense;
        this.Speed = speed;
        this.minecraftEntity = entity;
        this.team = team;
        Main.game.CREATURE_LIST.put(entity.getUniqueId(),this);
    }
    public void applySpeedBoost(float amount, int ticks) {
        this.temporarySpeedBonus = amount;
        this.speedBoostTicks = ticks;
        updateSpeed();
    }
    protected void Update()
    {
        if (speedBoostTicks > 0) {
            speedBoostTicks--;
            if (speedBoostTicks <= 0) {
                temporarySpeedBonus = 0;
                updateSpeed();
            }
        }
    }
    protected void updateSpeed() {
        if (minecraftEntity == null) return;
        float totalSpeed = Speed + temporarySpeedBonus;

        // Use Attribute.MOVEMENT_SPEED to ensure it applies to both walking and sprinting
        // Base attribute value for players is 0.1.
        // 100 Speed => 0.1 attribute value (default)
        double attributeValue = (totalSpeed / 100.0) * 0.1;
        if(minecraftEntity instanceof LivingEntity le)
        {
            var attribute = le.getAttribute(Attribute.MOVEMENT_SPEED);
            if (attribute != null) {
                le.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(attributeValue);
            }
        }


    }

    public abstract void applyDamage(float damage, LabyrinthCreature source, boolean bypassChain);
}
