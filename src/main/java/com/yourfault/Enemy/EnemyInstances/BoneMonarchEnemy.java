package com.yourfault.Enemy.EnemyInstances;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.BoneMonarch_Type;
import com.yourfault.Enemy.EnemyTypes.BoneArcher_Type;
import com.yourfault.Enemy.EnemyTypes.ShieldSkeleton_Type;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BoneMonarchEnemy extends Enemy implements Listener {

    private static final String PRISON_TAG = "bone_monarch_prison";
    private static final String BONE_PROJECTILE_META = "bone_monarch_projectile";
    private static final float PHASE_TWO_THRESHOLD = 0.70f;
    private static final float PHASE_THREE_THRESHOLD = 0.40f;

    private final Random random = new Random();
    private int phase = 1;
    private int summonCooldown = 0;
    private int barrageCooldown = 40;
    private int graveCallCooldown = 160;
    private int decreeCooldown = 140;
    private int prisonCooldown = 200;
    private boolean fusionConsumed = false;

    private final List<GraveMark> graveMarks = new ArrayList<>();
    private final List<PrisonCage> prisons = new ArrayList<>();

    public BoneMonarchEnemy(Mob entity, WaveContext context, BoneMonarch_Type type) {
        super(entity, context, 5L, type);
        Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
        entity.getEquipment().setItemInMainHand(new ItemStack(Material.BONE));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) {
            return;
        }
        float ratio = HEALTH / MAX_HEALTH;
        if (phase == 1 && ratio <= PHASE_TWO_THRESHOLD) {
            startPhaseTwo();
        } else if (phase == 2 && ratio <= PHASE_THREE_THRESHOLD) {
            startPhaseThree();
        }

        tickGraveMarks();

        if (summonCooldown > 0) summonCooldown -= 5;
        if (barrageCooldown > 0) barrageCooldown -= 5;
        if (graveCallCooldown > 0) graveCallCooldown -= 5;
        if (decreeCooldown > 0) decreeCooldown -= 5;
        if (prisonCooldown > 0) prisonCooldown -= 5;

        switch (phase) {
            case 1 -> runPhaseOneAbilities();
            case 2 -> runPhaseTwoAbilities();
            case 3 -> runPhaseThreeAbilities();
        }
    }

    private void runPhaseOneAbilities() {
        if (summonCooldown <= 0) {
            summonSkeletalDominion();
            summonCooldown = 400;
        }
        if (barrageCooldown <= 0) {
            performBoneBarrage(false);
            barrageCooldown = 120;
        }
        if (graveCallCooldown <= 0) {
            castGraveCall();
            graveCallCooldown = 240;
        }
    }

    private void runPhaseTwoAbilities() {
        if (summonCooldown <= 0) {
            summonSkeletalDominion();
            summonCooldown = 360;
        }
        if (barrageCooldown <= 0) {
            performBoneBarrage(true);
            barrageCooldown = 110;
        }
        if (graveCallCooldown <= 0) {
            castGraveCall();
            graveCallCooldown = 220;
        }
        if (decreeCooldown <= 0) {
            royalDecree();
            decreeCooldown = 160;
        }
        if (prisonCooldown <= 0) {
            bonePrison();
            prisonCooldown = 260;
        }
    }

    private void runPhaseThreeAbilities() {
        runPhaseTwoAbilities();
        applyBoneStorm();
    }

    private void summonSkeletalDominion() {
        for (int i = 0; i < 3; i++) {
            spawnMinion(new BoneArcher_Type(), 6 + random.nextInt(4));
        }
        for (int i = 0; i < 2; i++) {
            spawnMinion(new ShieldSkeleton_Type(), 6 + random.nextInt(4));
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.6f);
        entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 40, 1.5, 0.4, 1.5, 0.02);
    }

    private void spawnMinion(com.yourfault.Enemy.EnemyTypes.AbstractEnemyType type, double distance) {
        Vector offset = Vector.getRandom().multiply(distance);
        offset.setY(0);
        Location spawn = entity.getLocation().clone().add(offset);
        spawn.setY(entity.getLocation().getY());
        if (Main.game.getWaveManager() != null) {
            Main.game.getWaveManager().spawnEnemyAt(spawn, type, context, false);
        } else {
            Mob mob = type.SpawnEntity(spawn);
            type.CreateEnemyInstance(mob, context);
        }
    }

    private void performBoneBarrage(boolean upgraded) {
        entity.swingMainHand();
        Location origin = entity.getEyeLocation();
        List<Vector> directions = fanDirections(origin.getDirection(), 8, 30);
        for (Vector dir : directions) {
            spawnBoneProjectile(origin, dir, upgraded);
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.9f);
    }

    private List<Vector> fanDirections(Vector forward, int count, double spreadDegrees) {
        List<Vector> list = new ArrayList<>();
        Vector base = forward.clone().normalize();
        for (int i = 0; i < count; i++) {
            double angle = spreadDegrees * ((i / (double) (count - 1)) - 0.5);
            double radians = Math.toRadians(angle);
            Vector rotated = rotateAroundY(base, radians);
            list.add(rotated);
        }
        return list;
    }

    private Vector rotateAroundY(Vector vec, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = vec.getX() * cos - vec.getZ() * sin;
        double z = vec.getX() * sin + vec.getZ() * cos;
        return new Vector(x, vec.getY(), z).normalize();
    }

    private void spawnBoneProjectile(Location origin, Vector direction, boolean upgraded) {
        Snowball projectile = entity.getWorld().spawn(origin, Snowball.class, ball -> {
            ball.setItem(new ItemStack(Material.BONE));
            ball.setVelocity(direction.multiply(1.2));
            ball.setShooter(entity);
            ball.setMetadata(BONE_PROJECTILE_META, new FixedMetadataValue(Main.plugin, upgraded ? 2 : 1));
        });
        if (upgraded) {
            projectile.setGravity(false);
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (projectile.isDead() || projectile.isOnGround()) {
                        cancel();
                        return;
                    }
                    ticks++;
                    projectile.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, projectile.getLocation(), 3, 0.05, 0.05, 0.05, Bukkit.createBlockData(Material.BONE_BLOCK));
                    if (ticks % 6 == 0) {
                        Player nearest = findNearestPlayer(projectile.getLocation());
                        if (nearest != null) {
                            Vector pull = nearest.getEyeLocation().toVector().subtract(projectile.getLocation().toVector()).normalize().multiply(0.2);
                            projectile.setVelocity(projectile.getVelocity().add(pull).normalize().multiply(1.2));
                        }
                    }
                }
            }.runTaskTimer(Main.plugin, 1L, 1L);
        }
    }

    private Player findNearestPlayer(Location loc) {
        double best = Double.MAX_VALUE;
        Player result = null;
        for (GamePlayer gp : Main.game.PLAYER_LIST.values()) {
            Player p = gp.getMinecraftPlayer();
            if (p == null || !p.isOnline() || p.isDead()) continue;
            double dist = p.getLocation().distanceSquared(loc);
            if (dist < best) {
                best = dist;
                result = p;
            }
        }
        return result;
    }

    private void castGraveCall() {
        List<Player> candidates = new ArrayList<>();
        for (GamePlayer gp : Main.game.PLAYER_LIST.values()) {
            Player p = gp.getMinecraftPlayer();
            if (p != null && p.isOnline() && !p.isDead()) {
                candidates.add(p);
            }
        }
        Collections.shuffle(candidates);
        int targets = Math.min(3, candidates.size());
        for (int i = 0; i < targets; i++) {
            Player player = candidates.get(i);
            Location base = player.getLocation().getBlock().getLocation();
            Block block = base.getBlock();
            BlockData original = block.getBlockData().clone();
            block.setType(Material.BONE_BLOCK, false);
            ArmorStand marker = spawnMarkerAbove(player.getLocation());
            graveMarks.add(new GraveMark(player.getUniqueId(), marker, block, original));
            player.sendMessage("§7Your grave has been chosen!");
        }
    }

    private ArmorStand spawnMarkerAbove(Location location) {
        return location.getWorld().spawn(location.clone().add(0, 2.0, 0), ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setMarker(true);
            as.setSmall(true);
            as.setInvulnerable(true);
            as.setGravity(false);
            as.getEquipment().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));
        });
    }

    private void tickGraveMarks() {
        Iterator<GraveMark> iterator = graveMarks.iterator();
        while (iterator.hasNext()) {
            GraveMark mark = iterator.next();
            mark.ticks -= 5;
            Player player = Bukkit.getPlayer(mark.playerId);
            if (player != null) {
                player.spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 2, 0.2, 0.3, 0.2, 0.01);
            }
            if (mark.ticks <= 0) {
                explodeGrave(mark, player);
                iterator.remove();
            }
        }
    }

    private void explodeGrave(GraveMark mark, Player player) {
        if (player != null && player.getWorld().equals(entity.getWorld())) {
            Location checkLoc = player.getLocation();
            boolean safe = checkLoc.getBlock().equals(mark.boneBlock);
            double damage = safe ? 4.0 : 14.0;
            GamePlayer gp = Main.game.GetPlayer(player);
            if (gp != null) {
                gp.applyDamage((float) damage, this, false);
            }
            player.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, player.getLocation(), 20, 0.6, 0.2, 0.6, Bukkit.createBlockData(Material.BONE_BLOCK));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BONE_BLOCK_BREAK, 1.0f, safe ? 1.2f : 0.6f);
        }
        mark.restore();
    }

    private void startPhaseTwo() {
        phase = 2;
        entity.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, entity.getLocation(), 1);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.4f, 0.7f);
        decreeCooldown = 40;
        prisonCooldown = 100;
        performFusionIfNeeded();
    }

    private void startPhaseThree() {
        phase = 3;
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.2f, 0.8f);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
    }

    private void royalDecree() {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.6f);
        for (Player player : entity.getWorld().getPlayers()) {
            if (player == null || player.isDead()) continue;
            double distSq = player.getLocation().distanceSquared(entity.getLocation());
            if (distSq > 100) continue;
            Vector knock = player.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(1.4);
            player.setVelocity(knock.setY(0.4));
            GamePlayer gp = Main.game.GetPlayer(player);
            if (gp != null) gp.applyDamage(6.0f, this, false);
        }
    }

    private void bonePrison() {
        List<Player> candidates = new ArrayList<>();
        for (GamePlayer gp : Main.game.PLAYER_LIST.values()) {
            Player p = gp.getMinecraftPlayer();
            if (p != null && p.isOnline() && !p.isDead()) candidates.add(p);
        }
        Collections.shuffle(candidates);
        int count = Math.min(4, candidates.size());
        for (int i = 0; i < count; i++) {
            createBonePrison(candidates.get(i));
        }
    }

    private void createBonePrison(Player target) {
        Location base = target.getLocation().getBlock().getLocation();
        PrisonCage cage = new PrisonCage(target.getUniqueId(), base);
        prisons.add(cage);

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 6));
        target.sendMessage("§cYou are trapped in a Bone Prison! Left-click the glass to escape!");
    }

    private void performFusionIfNeeded() {
        if (fusionConsumed) return;
        List<UUID> absorbed = new ArrayList<>();
        for (UUID id : new ArrayList<>(Main.game.ENEMY_LIST.keySet())) {
            com.yourfault.Enemy.Enemy enemy = Main.game.ENEMY_LIST.get(id);
            if (enemy == null || enemy == this) continue;
            String name = enemy.enemyType.displayName.toLowerCase(Locale.ROOT);
            if (name.contains("bone") || name.contains("skeleton")) {
                absorbed.add(id);
                enemy.Destroy();
            }
        }
        if (!absorbed.isEmpty()) {
            fusionConsumed = true;
            damageMultiplier += 0.3f;
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
            entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 60, 1.8, 0.6, 1.8, 0.03);
        }
    }

    private void applyBoneStorm() {
        performFusionIfNeeded();
        for (Player player : entity.getWorld().getPlayers()) {
            if (player == null || player.isDead()) continue;
            if (player.getLocation().distanceSquared(entity.getLocation()) > 64) continue;
            if (!entity.hasLineOfSight(player)) continue;
            GamePlayer gp = Main.game.GetPlayer(player);
            if (gp != null) {
                gp.applyDamage(2.5f, this, false);
            }
            player.spawnParticle(Particle.BLOCK_CRUMBLE, player.getLocation(), 6, 0.4, 0.5, 0.4, Bukkit.createBlockData(Material.BONE_BLOCK));
        }
    }

    @EventHandler
    public void onBoneProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (!snowball.hasMetadata(BONE_PROJECTILE_META)) return;
        int value = snowball.getMetadata(BONE_PROJECTILE_META).get(0).asInt();
        Entity hit = event.getHitEntity();
        if (hit instanceof Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            GamePlayer gp = Main.game.GetPlayer(player);
            if (gp != null) gp.applyDamage(6.0f * damageMultiplier, this, false);
            snowball.remove();
            return;
        }
        if (event.getHitBlock() != null) {
            if (value > 0) {
                Vector dir = snowball.getVelocity().multiply(-1).normalize();
                snowball.setVelocity(dir.multiply(1.05));
                snowball.setMetadata(BONE_PROJECTILE_META, new FixedMetadataValue(Main.plugin, value - 1));
                event.setCancelled(true);
            } else {
                snowball.remove();
            }
        }
    }

    @EventHandler
    public void onPrisonInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.WHITE_STAINED_GLASS) return;

        Iterator<PrisonCage> it = prisons.iterator();
        while (it.hasNext()) {
            PrisonCage cage = it.next();
            if (cage.contains(clicked)) {
                cage.damage(event.getPlayer());
                if (cage.isBroken()) {
                    cage.restore();
                    it.remove();
                }
                event.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public void Destroy() {
        HandlerList.unregisterAll(this);
        for (GraveMark mark : graveMarks) {
            mark.restore();
        }
        graveMarks.clear();
        for (PrisonCage cage : prisons) {
            cage.restore();
        }
        prisons.clear();
        super.Destroy();
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}

    private static class GraveMark {
        private final UUID playerId;
        private final ArmorStand marker;
        private final Block boneBlock;
        private final BlockData original;
        private int ticks = 100;

        GraveMark(UUID playerId, ArmorStand marker, Block block, BlockData original) {
            this.playerId = playerId;
            this.marker = marker;
            this.boneBlock = block;
            this.original = original;
        }

        void restore() {
            boneBlock.setBlockData(original, false);
            if (marker != null && !marker.isDead()) {
                marker.remove();
            }
        }
    }

    private class PrisonCage {
        UUID victimId;
        List<Block> blocks = new ArrayList<>();
        List<BlockData> originalData = new ArrayList<>();
        int health = 10;

        PrisonCage(UUID victimId, Location base) {
            this.victimId = victimId;
            createBlock(base);
            createBlock(base.clone().add(0, 1, 0));
        }

        void createBlock(Location loc) {
            Block b = loc.getBlock();
            blocks.add(b);
            originalData.add(b.getBlockData().clone());
            b.setType(Material.WHITE_STAINED_GLASS);
        }

        boolean contains(Block b) {
            return blocks.contains(b);
        }

        void damage(Player attacker) {
            health--;
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_GLASS_HIT, 1.0f, 1.0f);
            if (health <= 0) {
                attacker.playSound(attacker.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
            }
        }

        boolean isBroken() {
            return health <= 0;
        }

        void restore() {
            for (int i = 0; i < blocks.size(); i++) {
                blocks.get(i).setBlockData(originalData.get(i));
            }
        }
    }
}
