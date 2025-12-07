// Java
package com.yourfault.Enemy;

import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

public abstract class ME_Enemy extends Enemy {
    public record EntityComponent(Mob entity, Vector RelativeOffset, Vector Pivot, Vector direction) {}
    public final EntityComponent[] parts;

    public ME_Enemy(EntityComponent[] entity, WaveContext context, AbstractEnemyType type) {
        super(entity[0].entity, context, type);
        this.parts = entity;
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

    public void move(Vector translation) {
        if (translation == null || translation.lengthSquared() == 0) {
            return;
        }
        LivingEntity base = primaryPart();
        Location newBaseLocation = base.getLocation().clone().add(translation);
        base.teleport(newBaseLocation);

        for (EntityComponent component : parts) {
            if (component.entity().equals(base)) {
                continue;
            }
            Location target = newBaseLocation.clone().add(component.RelativeOffset());
            if (component.direction() != null && component.direction().lengthSquared() > 0) {
                target.setDirection(component.direction());
            }
            component.entity().teleport(target);
        }
    }

    public void rotate(int partIndex, Vector axis, double radians) {
        if (partIndex < 0 || partIndex >= parts.length) {
            throw new IndexOutOfBoundsException("Invalid part index: " + partIndex);
        }
        if (axis == null || axis.lengthSquared() == 0) {
            throw new IllegalArgumentException("Axis vector must be non-zero");
        }

        EntityComponent original = parts[partIndex];
        Vector pivot = cloneOrZero(original.Pivot());
        Vector relative = cloneOrZero(original.RelativeOffset());
        Vector direction = cloneOrZero(original.direction());

        Vector rotatedOffset = rotateAroundAxis(relative.clone().subtract(pivot), axis, radians).add(pivot);
        Vector rotatedDirection = rotateAroundAxis(direction, axis, radians);

        EntityComponent updated = new EntityComponent(original.entity(), rotatedOffset, pivot, rotatedDirection);
        parts[partIndex] = updated;
        applyRelativeTransform(updated);
    }

    private void applyRelativeTransform(EntityComponent component) {
        LivingEntity base = primaryPart();
        if (component.entity().equals(base)) {
            return;
        }
        Location target = base.getLocation().clone().add(component.RelativeOffset());
        if (component.direction() != null && component.direction().lengthSquared() > 0) {
            target.setDirection(component.direction());
        }
        component.entity().teleport(target);
    }

    private LivingEntity primaryPart() {
        return parts[0].entity();
    }

    private static Vector cloneOrZero(Vector source) {
        return source == null ? new Vector(0, 0, 0) : source.clone();
    }

    private static Vector rotateAroundAxis(Vector vector, Vector axis, double radians) {
        Vector k = axis.clone().normalize();
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        Vector term1 = vector.clone().multiply(cos);
        Vector term2 = k.clone().getCrossProduct(vector).multiply(sin);
        Vector term3 = k.clone().multiply(k.dot(vector) * (1 - cos));

        return term1.add(term2).add(term3);
    }
}