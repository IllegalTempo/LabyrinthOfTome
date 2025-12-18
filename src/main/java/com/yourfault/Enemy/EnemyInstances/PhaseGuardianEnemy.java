package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.projectiles.PhaseGuardianBolt;
import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;

import java.util.Random;

public class PhaseGuardianEnemy extends Enemy {
    private static final int[] PHASE_THRESHOLDS = {100, 250, 550};
    private static final double BLAST_RADIUS = 3.0;
    private static final int CHARGE_TIME = 40;
    private static final int BASE_COOLDOWN = 80;
    private int phaseIndex = 0;
    private int chargeTicks = -1;
    private int cooldown = 40;
    private int boltCooldown = 60;
    private final Random random = new Random();
    private final Shulker shulker;

    public PhaseGuardianEnemy(Mob entity, WaveContext context, AbstractEnemyType type) {
        super(entity, context, 5L, type);
        this.shulker = entity instanceof Shulker s ? s : null;
        if (shulker != null) {
            shulker.setAI(false);
            shulker.setSilent(true);
            shulker.setPeek(0.0f);
        }
        entity.setGravity(false);
    }

    @Override
    public void update() {
        if (entity.isDead()) {
            return;
        }
        handlePhaseTransitions();
        handleBlastAttack();
        handleLevitationBolts();
    }

    private void handlePhaseTransitions() {
        float lost = MAX_HEALTH - HEALTH;
        if (phaseIndex < PHASE_THRESHOLDS.length && lost >= PHASE_THRESHOLDS[phaseIndex]) {
            phaseIndex++;
            teleportWithinBounds();
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f + (phaseIndex * 0.1f));
        }
    }

    private void handleBlastAttack() {
        if (chargeTicks >= 0) {
            chargeTicks += 5;
            if (shulker != null) {
                shulker.setPeek(Math.min(1.0f, shulker.getPeek() + 0.1f));
            }
            spawnChargeParticles();
            if (chargeTicks >= CHARGE_TIME) {
                detonate();
                chargeTicks = -1;
                cooldown = Math.max(40, BASE_COOLDOWN - (phaseIndex * 10));
                if (shulker != null) {
                    shulker.setPeek(0.0f);
                }
            }
            return;
        }
        if (cooldown > 0) {
            cooldown = Math.max(0, cooldown - 5);
            return;
        }
        chargeTicks = 0;
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 1.2f);
    }

    private void detonate() {
        Location center = entity.getLocation().add(0, 0.5, 0);
        entity.getWorld().spawnParticle(Particle.FLAME, center, 120, BLAST_RADIUS, 0.8, BLAST_RADIUS, 0.02);
        entity.getWorld().spawnParticle(Particle.EXPLOSION, center, 8, 0.2, 0.2, 0.2, 0.0);
        entity.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
        double radiusSq = BLAST_RADIUS * BLAST_RADIUS;
        for (Player player : center.getWorld().getPlayers()) {
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) > radiusSq) {
                continue;
            }
            GamePlayer gamePlayer = Main.game.GetPlayer(player);
            if (gamePlayer != null) {
                gamePlayer.applyDamage(40f,this,false); //todo check im not sure the bypass chain
            }
        }
    }

    private void spawnChargeParticles() {
        Location base = entity.getLocation().add(0, 0.5, 0);
        double radius = 2.6;
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 12) {
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            base.getWorld().spawnParticle(Particle.FLAME, base.getX() + x, base.getY(), base.getZ() + z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private void handleLevitationBolts() {
        if (phaseIndex < 3) {
            return;
        }
        if (boltCooldown > 0) {
            boltCooldown = Math.max(0, boltCooldown - 5);
            return;
        }
        GamePlayer target = getNearestPlayer();
        if (target == null || target.getMinecraftPlayer() == null) {
            return;
        }
        Location eye = entity.getEyeLocation();
        eye.setDirection(target.getMinecraftPlayer().getEyeLocation().toVector().subtract(eye.toVector()));
        new PhaseGuardianBolt(eye, this);
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.1f);
        boltCooldown = 60;
    }

    private void teleportWithinBounds() {
        Location origin = entity.getLocation();
        for (int i = 0; i < 10; i++) {
            double offsetX = random.nextInt(11) - 5;
            double offsetZ = random.nextInt(11) - 5;
            Location candidate = origin.clone().add(offsetX, 0, offsetZ);
            if (candidate.getWorld() != origin.getWorld()) {
                continue;
            }
            int highestY = candidate.getWorld().getHighestBlockYAt(candidate.getBlockX(), candidate.getBlockZ());
            candidate.setY(highestY + 1);
            entity.teleport(candidate);
            return;
        }
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
