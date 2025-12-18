package com.yourfault.pet;

import com.yourfault.system.LabyrinthCreature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class pet extends LabyrinthCreature {
    public boolean damagable;
    public LabyrinthCreature owner;
    public pet(LivingEntity entity, float maxHealth, float maxMana, float defense, float speed, boolean damagable, LabyrinthCreature owner, int team) {
        super(entity, maxHealth, maxMana, defense, speed,team);
        this.damagable = damagable;
        this.owner = owner;

    }

    @Override
    public void applyDamage(float damage, LabyrinthCreature source, boolean bypassChain) {

    }
}
