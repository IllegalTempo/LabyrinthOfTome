package com.yourfault.Enemy.EnemyInstances;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.yourfault.Enemy.Enemy;
import com.yourfault.Enemy.EnemyTypes.CrystalSentinel_Type;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.system.LabyrinthCreature;
import com.yourfault.wave.WaveContext;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CrystalSentinelEnemy extends Enemy implements Listener {

    private static final String CRYSTAL_NODE_TAG = "lot_crystal_sentinel_node";
    private static final String PRISM_TAG = "lot_crystal_prism";
    private static final String SHARD_META = "lot_crystal_shard";
    private static final int PRISM_HP = 2;
    private static final double PHASE_TWO_THRESHOLD = 0.75;
    private static final double PHASE_THREE_THRESHOLD = 0.45;

    private final List<CrystalNode> shieldNodes = new ArrayList<>();
    private final List<CrystalGrowth> growthBlocks = new ArrayList<>();
    private final Map<UUID, PrismShard> prismShards = new HashMap<>();
    private EnderCrystal displayCrystal;
    private int phase = 1;
    private int laserCooldown = 60;
    private int shardCooldown = 80;
    private int shatterwaveCooldown = 160;
    private int growthCooldown = 180;
    private int prismCooldown = 120;
    private boolean shieldActive = true;
    private long shieldBrokenTicks = 0;
    private Frequency currentFrequency = Frequency.AQUA;
    private int frequencyTimer = 120;
    private Vector rainbowDirection = new Vector(1, 0, 0);

    public CrystalSentinelEnemy(Mob entity, WaveContext context, CrystalSentinel_Type type) {
        super(entity, context, 5L, type);
        entity.setInvisible(true);
        entity.setAI(false);
        spawnDisplayCrystal();
        createShieldNodes();
        Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
    }

    private void spawnDisplayCrystal() {
        displayCrystal = entity.getWorld().spawn(entity.getLocation(), EnderCrystal.class, crystal -> {
            crystal.setShowingBottom(false);
            crystal.setInvulnerable(true);
        });
    }

    private void createShieldNodes() {
        shieldNodes.clear();
        Location origin = entity.getLocation();
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 / 8) * i;
            shieldNodes.add(new CrystalNode(origin, angle));
        }
        shieldActive = true;
        shieldBrokenTicks = 0;
    }

    @Override
    public void update() {
        if (entity == null || !entity.isValid()) {
            return;
        }
        if (displayCrystal != null && !displayCrystal.isDead()) {
            displayCrystal.teleport(entity.getLocation());
        }
        double ratio = HEALTH / MAX_HEALTH;
        if (phase == 1 && ratio <= PHASE_TWO_THRESHOLD) {
            startPhaseTwo();
        } else if (phase == 2 && ratio <= PHASE_THREE_THRESHOLD) {
            startPhaseThree();
        }

        for (CrystalNode node : shieldNodes) {
            node.tick(entity.getLocation());
        }
        tickGrowthBlocks();
        if (phase == 3) rotateRainbowDirection();

        if (laserCooldown > 0) laserCooldown -= 5;
        if (shardCooldown > 0) shardCooldown -= 5;
        if (shatterwaveCooldown > 0) shatterwaveCooldown -= 5;
        if (growthCooldown > 0) growthCooldown -= 5;
        if (prismCooldown > 0) prismCooldown -= 5;
        frequencyTimer -= 5;

        if (phase == 1) {
            if (laserCooldown <= 0) {
                firePatternedLasers();
                laserCooldown = 80;
            }
            if (shardCooldown <= 0) {
                launchCrystalShards();
                shardCooldown = 100;
            }
            if (shieldActive) {
                boolean anyAlive = shieldNodes.stream().anyMatch(CrystalNode::isAlive);
                if (!anyAlive) {
                    shieldActive = false;
                    shieldBrokenTicks = 20 * 15;
                    entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.6f);
                }
            } else {
                shieldBrokenTicks -= 5;
                if (shieldBrokenTicks <= 0) {
                    createShieldNodes();
                }
            }
        } else if (phase == 2) {
            if (laserCooldown <= 0) {
                firePatternedLasers();
                laserCooldown = 70;
            }
            if (shardCooldown <= 0) {
                launchCrystalShards();
                shardCooldown = 90;
            }
            if (shatterwaveCooldown <= 0) {
                castShatterwave();
                shatterwaveCooldown = 160;
            }
            if (growthCooldown <= 0) {
                spawnCrystalGrowth();
                growthCooldown = 200;
            }
            if (frequencyTimer <= 0) {
                cycleFrequency();
                frequencyTimer = 140;
            }
            handleFrequencyAura();
        } else if (phase == 3) {
            if (laserCooldown <= 0) {
                fireRainbowLaser();
                laserCooldown = 60;
            }
            if (prismCooldown <= 0 && prismShards.isEmpty()) {
                spawnPrismShards();
                prismCooldown = 200;
            }
        }
    }

    private void startPhaseTwo() {
        phase = 2;
        for (CrystalNode node : shieldNodes) {
            node.destroy();
        }
        shieldNodes.clear();
        shieldActive = false;
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.4f);
    }

    private void startPhaseThree() {
        phase = 3;
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.6f);
        spawnPrismShards();
    }

    private void firePatternedLasers() {
        LaserPattern pattern = LaserPattern.random();
        switch (pattern) {
            case CROSS -> fireLaserPattern(0);
            case CIRCLE -> fireLaserPattern(45);
            case SPIRAL -> fireSpiralPattern();
        }
    }

    private void fireLaserPattern(double baseAngle) {
        Location origin = entity.getLocation().add(0, 1.5, 0);
        for (int i = 0; i < 4; i++) {
            double angle = Math.toRadians(baseAngle + 90 * i);
            Vector dir = new Vector(Math.cos(angle), 0, Math.sin(angle));
            drawLaser(origin, dir, 20);
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
    }

    private void fireSpiralPattern() {
        Location origin = entity.getLocation().add(0, 1.5, 0);
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians((System.currentTimeMillis() / 100) % 360 + (60 * i));
            Vector dir = new Vector(Math.cos(angle), 0, Math.sin(angle));
            drawLaser(origin, dir, 18);
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.7f);
    }

    private void drawLaser(Location origin, Vector dir, int length) {
        dir = dir.normalize();
        for (int i = 1; i <= length; i++) {
            Location point = origin.clone().add(dir.clone().multiply(i));
            origin.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.02, 0.02, 0.02, 0.0);
            for (Player player : origin.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(point) < 1.2) {
                    GamePlayer gp = Main.game.GetPlayer(player);
                    if (gp != null) gp.applyDamage(5.0f, this, false);
                }
            }
        }
    }

    private void launchCrystalShards() {
        for (int i = 0; i < 6; i++) {
            Snowball shard = entity.getWorld().spawn(entity.getLocation().add(0, 1.5, 0), Snowball.class, ball -> {
                ball.setItem(new ItemStack(Material.AMETHYST_SHARD));
                Vector dir = new Vector((ThreadLocalRandom.current().nextDouble() * 2) - 1,
                        (ThreadLocalRandom.current().nextDouble() * 0.7) + 0.1,
                        (ThreadLocalRandom.current().nextDouble() * 2) - 1).normalize().multiply(0.9);
                ball.setVelocity(dir);
                ball.setMetadata(SHARD_META, new FixedMetadataValue(Main.plugin, 2));
            });
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_HIT, 1.0f, 1.4f);
    }

    private void castShatterwave() {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ALLAY_DEATH, 1.2f, 0.3f);
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.isDead()) continue;
            GamePlayer gp = Main.game.GetPlayer(player);
            if (gp != null) {
                gp.applyDamage(10.0f, this, true);
            }
        }
    }

    private void spawnCrystalGrowth() {
        for (int i = 0; i < 3; i++) {
            Vector offset = new Vector((ThreadLocalRandom.current().nextDouble() * 2) - 1, 0, (ThreadLocalRandom.current().nextDouble() * 2) - 1).normalize().multiply(ThreadLocalRandom.current().nextDouble() * 6);
            Location loc = entity.getLocation().clone().add(offset);
            loc.setY(loc.getWorld().getHighestBlockYAt(loc));
            Block block = loc.getBlock();
            BlockData original = block.getBlockData().clone();
            block.setType(Material.AMETHYST_CLUSTER, false);
            growthBlocks.add(new CrystalGrowth(block, original, 200));
        }
    }

    private void tickGrowthBlocks() {
        Iterator<CrystalGrowth> iterator = growthBlocks.iterator();
        while (iterator.hasNext()) {
            CrystalGrowth growth = iterator.next();
            growth.tick(5);
            if (growth.expired()) {
                growth.restore();
                iterator.remove();
                continue;
            }
            for (Player player : entity.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(growth.center()) < 2.0) {
                    GamePlayer gp = Main.game.GetPlayer(player);
                    if (gp != null) gp.applyDamage(4.0f, this, false);
                }
            }
        }
    }

    private void handleFrequencyAura() {
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getWorld() != entity.getWorld()) {
                continue;
            }
            if (player.getLocation().distanceSquared(entity.getLocation()) > 36) {
                continue;
            }
            if (!player.isSneaking()) {
                continue;
            }
            PotionEffectType effect = switch (currentFrequency) {
                case AQUA -> PotionEffectType.DOLPHINS_GRACE;
                case GOLD -> PotionEffectType.HERO_OF_THE_VILLAGE;
                case CRIMSON -> PotionEffectType.STRENGTH;
            };
            player.addPotionEffect(new PotionEffect(effect, 80, 0, true, true, true));
        }
    }

    private void cycleFrequency() {
        currentFrequency = currentFrequency.next();
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, currentFrequency.pitch);
        for (GamePlayer gp : Main.game.PLAYER_LIST.values()) {
            Player p = gp.getMinecraftPlayer();
            if (p != null) {
                p.sendMessage(Component.text("Crystal resonance shifts to " + currentFrequency.display, NamedTextColor.AQUA));
            }
        }
    }

    private void spawnPrismShards() {
        for (PrismShard shard : prismShards.values()) {
            shard.stand.remove();
        }
        prismShards.clear();
        Location origin = entity.getLocation();
        WeaponRequirement[] requirements = WeaponRequirement.values();
        for (int i = 0; i < requirements.length; i++) {
            WeaponRequirement req = requirements[i];
            double angle = (Math.PI * 2 / requirements.length) * i;
            Vector offset = new Vector(Math.cos(angle), 0.2, Math.sin(angle)).multiply(5);
            ArmorStand stand = origin.getWorld().spawn(origin.clone().add(offset), ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setMarker(false); // allow hitting
                as.setGravity(false);
                if (as.getEquipment() != null) as.getEquipment().setHelmet(new ItemStack(req.helmet));
                as.setMetadata(PRISM_TAG, new FixedMetadataValue(Main.plugin, true));
            });
            prismShards.put(stand.getUniqueId(), new PrismShard(stand, req, PRISM_HP));
        }
    }

    private void rotateRainbowDirection() {
        double angle = Math.toRadians(6);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = rainbowDirection.getX() * cos - rainbowDirection.getZ() * sin;
        double z = rainbowDirection.getX() * sin + rainbowDirection.getZ() * cos;
        rainbowDirection = new Vector(x, 0, z).normalize();
    }

    private void fireRainbowLaser() {
        Location origin = entity.getLocation().add(0, 1.5, 0);
        drawLaser(origin, rainbowDirection.clone(), 22);
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.2f, 0.4f);
    }

    @Override
    public void applyDamage(float damage, LabyrinthCreature damageDealer, boolean bypassChain) {
        if (shieldActive && phase == 1) {
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_STEP, 0.8f, 1.6f);
            return;
        }
        if (phase == 2 && damageDealer instanceof GamePlayer gp) {
            if (!currentFrequency.matches(gp)) {
                gp.getMinecraftPlayer().sendMessage(Component.text("Your frequency mismatches!", NamedTextColor.GRAY));
                damage *= 0.15f;
            }
        }
        if (phase == 3 && !prismShards.isEmpty() && damageDealer instanceof GamePlayer gp) {
            WeaponRequirement matched = null;
            // iterate entries so we can remove the completed shard from the map immediately
            Iterator<Map.Entry<UUID, PrismShard>> it = prismShards.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, PrismShard> entry = it.next();
                PrismShard shard = entry.getValue();
                if (!shard.completed && shard.requirement.matches(gp)) {
                    matched = shard.requirement;
                    shard.completed = true;
                    if (shard.stand != null) shard.stand.remove();
                    it.remove();
                    break;
                }
            }
            if (matched == null) {
                gp.getMinecraftPlayer().sendMessage(Component.text("Your weapon fails to fracture the prism.", NamedTextColor.GRAY));
                damage *= 0.1f;
            } else if (prismShards.isEmpty()) {
                // all shards cleared
                entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.4f, 0.9f);
            }
        }
        super.applyDamage(damage, damageDealer, bypassChain);
    }

    @EventHandler
    public void onNodeHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;
        if (!stand.hasMetadata(CRYSTAL_NODE_TAG)) return;
        for (CrystalNode node : shieldNodes) {
            if (node.matches(stand)) {
                node.hit();
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onPrismHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;
        if (!stand.hasMetadata(PRISM_TAG)) return;
        PrismShard shard = prismShards.get(stand.getUniqueId());
        if (shard == null) return;
        // decrement shard HP and play feedback
        try {
            shard.hp--;
            stand.getWorld().spawnParticle(Particle.CRIT, stand.getLocation().add(0, 0.5, 0), 8, 0.2, 0.2, 0.2, 0.02);
            stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.8f, 1.2f);
            if (shard.hp <= 0) {
                shard.completed = true;
                if (shard.stand != null) shard.stand.remove();
                prismShards.remove(stand.getUniqueId());
                if (prismShards.isEmpty()) {
                    entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.4f, 0.9f);
                }
            }
        } catch (Exception ignored) {}
        event.setCancelled(true);
    }

    @EventHandler
    public void onShardHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (!snowball.hasMetadata(SHARD_META)) return;
        int bounces = snowball.getMetadata(SHARD_META).get(0).asInt();
        if (event.getHitEntity() instanceof Player player) {
            GamePlayer gp = Main.game.GetPlayer(player);
            if (gp != null) gp.applyDamage(5.0f, this, false);
            snowball.remove();
        } else if (event.getHitBlock() != null) {
            if (bounces > 0) {
                Vector rebound = snowball.getVelocity().multiply(-1);
                snowball.setVelocity(rebound);
                snowball.setMetadata(SHARD_META, new FixedMetadataValue(Main.plugin, bounces - 1));
                event.setCancelled(true);
            } else {
                snowball.remove();
            }
        }
    }

    @EventHandler
    public void onGrowthBroken(BlockBreakEvent event) {
        if (growthBlocks.isEmpty()) {
            return;
        }
        Iterator<CrystalGrowth> iterator = growthBlocks.iterator();
        while (iterator.hasNext()) {
            CrystalGrowth growth = iterator.next();
            if (growth.isBlock(event.getBlock())) {
                event.setDropItems(false);
                event.setCancelled(true);
                growth.restore();
                iterator.remove();
                event.getPlayer().playSound(event.getBlock().getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.6f, 1.8f);
                break;
            }
        }
    }

    @Override
    public void Destroy() {
        HandlerList.unregisterAll(this);
        if (displayCrystal != null) {
            displayCrystal.remove();
        }
        for (CrystalNode node : shieldNodes) {
            node.destroy();
        }
        for (CrystalGrowth growth : growthBlocks) {
            growth.restore();
        }
        for (PrismShard shard : prismShards.values()) {
            shard.stand.remove();
        }
        prismShards.clear();
        super.Destroy();
    }

    @Override
    public void tick() {}

    @Override
    public void OnAttack() {}

    @Override
    public void OnDealDamage() {}

    private static class CrystalNode {
        private ArmorStand stand;
        private double angle;
        private int hits = 12;
        private double radius = 5.5;

        CrystalNode(Location origin, double angle) {
            this.angle = angle;
            spawn(origin);
        }

        void spawn(Location origin) {
            stand = origin.getWorld().spawn(origin.clone().add(Math.cos(angle) * radius, 1.2, Math.sin(angle) * radius), ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setMarker(false);
                as.setInvulnerable(false);
                as.setCustomNameVisible(true);
                as.setCustomName("§bCrystal Node");
                as.getEquipment().setHelmet(new ItemStack(Material.PRISMARINE_CRYSTALS));
                as.setMetadata(CRYSTAL_NODE_TAG, new FixedMetadataValue(Main.plugin, true));
            });
        }

        void tick(Location origin) {
            if (!isAlive()) return;
            angle += Math.toRadians(1.8);
            Location target = origin.clone().add(Math.cos(angle) * radius, 1.2, Math.sin(angle) * radius);
            stand.teleport(target);
        }

        boolean isAlive() {
            return stand != null && !stand.isDead() && hits > 0;
        }

        boolean matches(ArmorStand other) {
            return stand != null && stand.getUniqueId().equals(other.getUniqueId());
        }

        void hit() {
            if (!isAlive()) return;
            hits--;
            stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_STEP, 1.0f, 1.4f);
            if (hits <= 0) {
                stand.setCustomName("§7Inactive");
                stand.getWorld().spawnParticle(Particle.ASH, stand.getLocation(), 8, 0.3, 0.3, 0.3, 0.01);
            }
        }

        void destroy() {
            if (stand != null) {
                stand.remove();
            }
        }
    }

    private static class CrystalGrowth {
        private final Block block;
        private final BlockData original;
        private final int lifespan;
        private int age;

        private CrystalGrowth(Block block, BlockData original, int lifespan) {
            this.block = block;
            this.original = original;
            this.lifespan = lifespan;
            this.age = 0;
        }

        void tick(int delta) {
            age += delta;
        }

        boolean expired() {
            return age >= lifespan;
        }

        void restore() {
            block.setBlockData(original, false);
        }

        Location center() {
            return block.getLocation().add(0.5, 0.5, 0.5);
        }

        boolean isBlock(Block other) {
            return block.equals(other);
        }
    }

    private enum LaserPattern {
        CROSS,
        CIRCLE,
        SPIRAL;

        private static final LaserPattern[] VALUES = values();

        static LaserPattern random() {
            return VALUES[new Random().nextInt(VALUES.length)];
        }
    }

    private enum Frequency {
        AQUA("Azure") {
            @Override
            boolean matches(GamePlayer gp) {
                return gp.getMinecraftPlayer().hasPotionEffect(PotionEffectType.DOLPHINS_GRACE);
            }
        },
        GOLD("Golden") {
            @Override
            boolean matches(GamePlayer gp) {
                return gp.getMinecraftPlayer().hasPotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
            }
        },
        CRIMSON("Crimson") {
            @Override
            boolean matches(GamePlayer gp) {
                return gp.getMinecraftPlayer().hasPotionEffect(PotionEffectType.STRENGTH);
            }
        };

        final String display;
        final float pitch;

        Frequency(String display) {
            this.display = display;
            this.pitch = switch (this) {
                case AQUA -> 1.0f;
                case GOLD -> 1.3f;
                case CRIMSON -> 0.7f;
            };
        }

        Frequency next() {
            return switch (this) {
                case AQUA -> GOLD;
                case GOLD -> CRIMSON;
                case CRIMSON -> AQUA;
            };
        }

        abstract boolean matches(GamePlayer gp);
    }

    private enum WeaponRequirement {
        MELEE(Material.DIAMOND_SWORD) {
            @Override
            boolean matches(GamePlayer gp) {
                return matchesByPlayer(gp, "sword", EnumSet.of(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD), "excalibur", "blade");
            }
        },
        RANGED(Material.BOW) {
            @Override
            boolean matches(GamePlayer gp) {
                return matchesByPlayer(gp, "bow", EnumSet.of(Material.BOW, Material.CROSSBOW));
            }
        },
        MAGIC(Material.ENCHANTED_BOOK) {
            @Override
            boolean matches(GamePlayer gp) {
                return matchesByPlayer(gp, "staff", EnumSet.of(Material.ENCHANTED_BOOK));
            }
        },
        ARC(Material.BLAZE_ROD) {
            @Override
            boolean matches(GamePlayer gp) {
                return matchesByPlayer(gp, "arc", EnumSet.of(Material.BLAZE_ROD, Material.STICK));
            }
        },
        LANCE(Material.TRIDENT) {
            @Override
            boolean matches(GamePlayer gp) {
                return matchesByPlayer(gp, "lance", EnumSet.of(Material.TRIDENT));
            }
        },
        HAMMER(Material.NETHERITE_AXE) {
            @Override
            boolean matches(GamePlayer gp) {
                return matchesByPlayer(gp, "hammer", EnumSet.of(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE));
            }
        },
        DAGGER(Material.IRON_SWORD) {
            @Override
            boolean matches(GamePlayer gp) {
                return matchesByPlayer(gp, "dagger", EnumSet.of(Material.IRON_SWORD));
            }
        };

        final Material helmet;

        WeaponRequirement(Material helmet) {
            this.helmet = helmet;
        }

        abstract boolean matches(GamePlayer gp);

        private static boolean matchesByPlayer(GamePlayer gp, String keyword, Set<Material> materials, String... aliases) {
            if (gp == null) return false;
            try {
                // first try the selected weapon name (if present)
                if (gp.SELECTED_WEAPON != null && gp.SELECTED_WEAPON.name().toLowerCase(Locale.ROOT).contains(keyword)) {
                    return true;
                }
                Player p = gp.getMinecraftPlayer();
                if (p == null) return false;
                // check main hand item
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand != null && hand.getType() != Material.AIR) {
                    Material t = hand.getType();
                    String tname = t.name().toLowerCase(Locale.ROOT);
                    if (materials.contains(t)) return true;
                    // match by material name (covers renamed/custom items that keep vanilla material)
                    if (tname.contains(keyword)) return true;
                    // special-case: lance maps to trident
                    if ("lance".equals(keyword) && tname.contains("trident")) return true;
                    // check display name for keywords or aliases (covers custom-named items like "Excalibur")
                    if (hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()) {
                        String name = hand.getItemMeta().getDisplayName().toLowerCase(Locale.ROOT);
                        if (name.contains(keyword)) return true;
                        for (String a : aliases) {
                            if (a != null && !a.isEmpty() && name.contains(a.toLowerCase(Locale.ROOT))) return true;
                        }
                    }
                }
                // check offhand as secondary fallback
                ItemStack off = p.getInventory().getItemInOffHand();
                if (off != null && off.getType() != Material.AIR && materials.contains(off.getType())) return true;
            } catch (Exception ignored) {}
            return false;
        }
    }

    private static class PrismShard {
        private final ArmorStand stand;
        private final WeaponRequirement requirement;
        private boolean completed = false;
        private int hp;

        PrismShard(ArmorStand stand, WeaponRequirement requirement, int hp) {
            this.stand = stand;
            this.requirement = requirement;
            this.hp = hp;
        }
    }
}
