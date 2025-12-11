package com.yourfault.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockVector;

import com.yourfault.map.util.RadialTaskRunner;

/**
 * Handles spawning and clearing the boss arena structure progressively from the center outward.
 */
public class BossStructureSpawner {
    private static final String[] STRUCTURE_RESOURCES = {
            "structures/bossRoomModel/BossRoom2.nbt",
            "structures/bossRoomModel/BossRoom3.nbt"
    };
    private static final int PLACEMENTS_PER_TICK = 500;
    private static final int CLEAR_PLACEMENTS_TICK = 1500;
    private static final Set<Material> BLOCKED_TEMPLATE_MATERTIALS = EnumSet.of(Material.BARRIER);
    private static final int PROGRESS_STEP_PERCENT = 10;

    private final JavaPlugin plugin;
    private final StructurePlacementHelper structureHelper;
    private final Random random = new Random();

    private RadialTaskRunner activeGeneration;
    private RadialTaskRunner activeClear;
    private final Map<BlockPosition, BlockData> originalBlocks = new HashMap<>();
    private Location activeCenter;
    private String activeTemplate;
    private TemplateInfo reservedTemplate;
    private int nextBossRoomProgressBroadcast = PROGRESS_STEP_PERCENT;

    public BossStructureSpawner(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.structureHelper = new StructurePlacementHelper(plugin);
    }

    public synchronized boolean isGenerationRunning() {
        return activeGeneration != null;
    }

    public synchronized boolean isClearRunning() {
        return activeClear != null;
    }

    public synchronized boolean hasActiveBossRoom() {
        return !originalBlocks.isEmpty();
    }

    public synchronized Optional<TemplateInfo> previewNextTemplate() {
        if (isGenerationRunning() || hasActiveBossRoom()) {
            return Optional.empty();
        }
        TemplateInfo info = ensureReservedTemplate();
        if (info == null) {
            return Optional.empty();
        }
        return Optional.of(info.copy());
    }

    /**
     * Safe accessor for callers that only need the template size without accessing internal types.
     */
    public synchronized Optional<BlockVector> previewNextTemplateSize() {
        return previewNextTemplate().map(TemplateInfo::size);
    }

    public synchronized void generateBossRoom(Location center,
                                              Consumer<String> onSuccess,
                                              Consumer<String> onError) {
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onError, "onError");
        if (center == null || center.getWorld() == null) {
            onError.accept("Center location with a valid world is required.");
            return;
        }
        if (isGenerationRunning()) {
            onError.accept("Boss room generation already running.");
            return;
        }
        if (isClearRunning()) {
            onError.accept("Boss room clearing in progress. Wait until it finishes.");
            return;
        }
        if (hasActiveBossRoom()) {
            onError.accept("A boss room already exists. Use /clearbossroom first.");
            return;
        }

        TemplateInfo template = ensureReservedTemplate();
        if (template == null) {
            onError.accept("Boss room template could not be loaded.");
            return;
        }

        resetBossProgressGate();

        World world = center.getWorld();
        BlockVector size = template.size();
        if (size == null) {
            onError.accept("Boss room template size information missing.");
            return;
        }

        Bounds bounds = new Bounds(center, size);
        if (bounds.minY < world.getMinHeight() || bounds.maxY > world.getMaxHeight()) {
            onError.accept("Y coordinate is out of bounds for the boss structure.");
            return;
        }

        snapshotOriginalBlocks(world, bounds);

        boolean placed = structureHelper.placeStructure(
                template.resourcePath(),
                world,
                center.getBlockX(),
                bounds.minY,
                center.getBlockZ(),
                random,
                true,
                MapTheme.StructureTemplate.Rotation.NONE
        );
        if (!placed) {
            restoreOriginalSnapshot(world);
            originalBlocks.clear();
            onError.accept("Failed to place boss structure; see server log for details.");
            return;
        }

        List<BlockPlacement> plan = capturePlacementPlan(world, center, bounds);
        List<RadialTaskRunner.Step> steps = buildPlacementSteps(plan);
        if (steps.isEmpty()) {
            restoreOriginalSnapshot(world);
            originalBlocks.clear();
            onError.accept("Boss room template has no block data to place.");
            return;
        }

        this.activeCenter = center.clone();
        this.activeTemplate = template.resourcePath();
        RadialTaskRunner task = new RadialTaskRunner(
                steps,
                PLACEMENTS_PER_TICK,
                () -> onGenerationFinished(onSuccess),
                ex -> onGenerationFailed(ex, onError),
                this::handleBossGenerationProgress
        );
        this.activeGeneration = task;
        task.runTaskTimer(plugin, 1L, 1L);
    }

    private void handleBossGenerationProgress(int completedSteps, int totalSteps) {
        if (totalSteps <= 0 || nextBossRoomProgressBroadcast > 100) {
            return;
        }
        double fraction = (double) completedSteps / (double) totalSteps;
        int percent = (int) Math.floor(fraction * 100.0);
        while (percent >= nextBossRoomProgressBroadcast && nextBossRoomProgressBroadcast < 100) {
            int displayPercent = nextBossRoomProgressBroadcast;
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[Boss] Arena prep " + displayPercent + "% complete...");
            nextBossRoomProgressBroadcast += PROGRESS_STEP_PERCENT;
        }
    }

    private void resetBossProgressGate() {
        nextBossRoomProgressBroadcast = PROGRESS_STEP_PERCENT;
    }

    public synchronized void clearBossRoom(Consumer<String> onSuccess,
                                           Consumer<String> onError) {
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onError, "onError");
        if (!hasActiveBossRoom()) {
            onError.accept("No active boss room to clear.");
            return;
        }
        if (isGenerationRunning()) {
            onError.accept("Cannot clear boss room while generation is running.");
            return;
        }
        if (isClearRunning()) {
            onError.accept("Boss room clearing already in progress.");
            return;
        }
        if (activeCenter == null || activeCenter.getWorld() == null) {
            onError.accept("Boss room world reference is missing; cannot clear safely.");
            return;
        }

        List<BlockRestore> restorePlan = buildRestorePlan();
        if (restorePlan.isEmpty()) {
            onSuccess.accept("Boss room already cleared.");
            resetState();
            return;
        }
        List<RadialTaskRunner.Step> steps = buildRestoreSteps(restorePlan);
        RadialTaskRunner task = new RadialTaskRunner(
                steps,
                CLEAR_PLACEMENTS_TICK,
                () -> onClearFinished(onSuccess),
                ex -> onClearFailed(ex, onError)
        );
        this.activeClear = task;
        task.runTaskTimer(plugin, 1L, 1L);
    }

    private Optional<String> resolveTemplateResource() {
        List<String> available = new ArrayList<>();
        for (String resource : STRUCTURE_RESOURCES) {
            if (structureHelper.hasStructure(resource)) {
                available.add(resource);
            }
        }
        if (available.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(available.get(random.nextInt(available.size())));
    }

    private void snapshotOriginalBlocks(World world, Bounds bounds) {
        originalBlocks.clear();
        for (int x = 0; x < bounds.sizeX; x++) {
            int worldX = bounds.minX + x;
            for (int y = 0; y < bounds.sizeY; y++) {
                int worldY = bounds.minY + y;
                for (int z = 0; z < bounds.sizeZ; z++) {
                    int worldZ = bounds.minZ + z;
                    BlockPosition position = new BlockPosition(worldX, worldY, worldZ);
                    Block block = world.getBlockAt(worldX, worldY, worldZ);
                    originalBlocks.put(position, block.getBlockData().clone());
                }
            }
        }
    }

    private List<BlockPlacement> capturePlacementPlan(World world, Location center, Bounds bounds) {
        List<BlockPlacement> plan = new ArrayList<>();
        double centerX = center.getX();
        double centerZ = center.getZ();
        for (int x = 0; x < bounds.sizeX; x++) {
            int worldX = bounds.minX + x;
            for (int y = 0; y < bounds.sizeY; y++) {
                int worldY = bounds.minY + y;
                for (int z = 0; z < bounds.sizeZ; z++) {
                    int worldZ = bounds.minZ + z;
                    Block block = world.getBlockAt(worldX, worldY, worldZ);
                    BlockData data = block.getBlockData().clone();
                    Material material = data.getMaterial();
                    if (material != Material.AIR && !BLOCKED_TEMPLATE_MATERTIALS.contains(material)) {
                        double dx = (worldX + 0.5) - centerX;
                        double dz = (worldZ + 0.5) - centerZ;
                        double radial = Math.sqrt(dx * dx + dz * dz);
                        plan.add(new BlockPlacement(worldX, worldY, worldZ, data, radial));
                    }
                    block.setType(Material.AIR, false);
                }
            }
        }
        plan.sort(Comparator.comparingDouble(BlockPlacement::radialDistance));
        return plan;
    }

    private List<RadialTaskRunner.Step> buildPlacementSteps(List<BlockPlacement> plan) {
        List<RadialTaskRunner.Step> steps = new ArrayList<>(plan.size());
        for (BlockPlacement placement : plan) {
            steps.add(new RadialTaskRunner.Step(
                    placement.radialDistance(),
                    () -> {
                        World world = requireActiveWorld();
                        Block block = world.getBlockAt(placement.getX(), placement.getY(), placement.getZ());
                        block.setBlockData(placement.getData().clone(), false);
                    }
            ));
        }
        return steps;
    }

    private List<BlockRestore> buildRestorePlan() {
        if (activeCenter == null) {
            return Collections.emptyList();
        }
        double centerX = activeCenter.getX();
        double centerZ = activeCenter.getZ();
        List<BlockRestore> plan = new ArrayList<>(originalBlocks.size());
        for (Map.Entry<BlockPosition, BlockData> entry : originalBlocks.entrySet()) {
            BlockPosition position = entry.getKey();
            double dx = (position.getX() + 0.5) - centerX;
            double dz = (position.getZ() + 0.5) - centerZ;
            double radial = Math.sqrt(dx * dx + dz * dz);
            plan.add(new BlockRestore(position, entry.getValue(), radial));
        }
        plan.sort(Comparator.comparingDouble(BlockRestore::radialDistance).reversed());
        return plan;
    }

    private List<RadialTaskRunner.Step> buildRestoreSteps(List<BlockRestore> plan) {
        List<RadialTaskRunner.Step> steps = new ArrayList<>(plan.size());
        for (BlockRestore restore : plan) {
            steps.add(new RadialTaskRunner.Step(
                    restore.radialDistance(),
                    () -> {
                        World world = requireActiveWorld();
                        Block block = world.getBlockAt(restore.position().getX(), restore.position().getY(), restore.position().getZ());
                        block.setBlockData(restore.data().clone(), false);
                        originalBlocks.remove(restore.position());
                    }
            ));
        }
        return steps;
    }

    private synchronized void onGenerationFinished(Consumer<String> onSuccess) {
        activeGeneration = null;
        reservedTemplate = null;
        onSuccess.accept("Boss room generated using " + (activeTemplate != null ? activeTemplate : "template") + ".");
    }

    private void onGenerationFailed(Exception ex, Consumer<String> onError) {
        cleanupAfterFailure();
        onError.accept("Failed to generate boss room: " + ex.getMessage());
        plugin.getLogger().severe("Boss room generation failed: " + ex.getMessage());
    }

    private synchronized void onClearFinished(Consumer<String> onSuccess) {
        activeClear = null;
        resetState();
        onSuccess.accept("Boss room cleared and world state restored.");
    }

    private void onClearFailed(Exception ex, Consumer<String> onError) {
        cleanupAfterFailure();
        onError.accept("Failed to clear boss room: " + ex.getMessage());
        plugin.getLogger().severe("Boss room clearing failed: " + ex.getMessage());
    }

    private World requireActiveWorld() {
        if (activeCenter == null || activeCenter.getWorld() == null) {
            throw new IllegalStateException("Boss room world reference lost.");
        }
        return activeCenter.getWorld();
    }

    private void cleanupAfterFailure() {
        synchronized (this) {
            if (activeCenter != null && activeCenter.getWorld() != null && !originalBlocks.isEmpty()) {
                restoreOriginalSnapshot(activeCenter.getWorld());
            }
            resetState();
        }
    }

    private void restoreOriginalSnapshot(World world) {
        originalBlocks.forEach((pos, data) -> world.getBlockAt(pos.getX(), pos.getY(), pos.getZ()).setBlockData(data.clone(), false));
    }

    private void resetState() {
        activeGeneration = null;
        activeClear = null;
        originalBlocks.clear();
        activeCenter = null;
        activeTemplate = null;
        resetBossProgressGate();
    }

    private TemplateInfo ensureReservedTemplate() {
        if (reservedTemplate != null) {
            return reservedTemplate;
        }
        Optional<String> resource = resolveTemplateResource();
        if (resource.isEmpty()) {
            return null;
        }
        BlockVector size = structureHelper.getStructureSize(resource.get());
        if (size == null) {
            return null;
        }
        reservedTemplate = new TemplateInfo(resource.get(), size.clone());
        return reservedTemplate;
    }

    private static final class Bounds {
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final int maxY;

        private Bounds(Location center, BlockVector size) {
            this.sizeX = Math.max(1, size.getBlockX());
            this.sizeY = Math.max(1, size.getBlockY());
            this.sizeZ = Math.max(1, size.getBlockZ());
            int centerX = center.getBlockX();
            int centerZ = center.getBlockZ();
            this.minX = centerX - sizeX / 2;
            this.minZ = centerZ - sizeZ / 2;
            this.minY = center.getBlockY() - sizeY / 2;
            this.maxY = minY + sizeY - 1;
        }
    }

    private static final class BlockPlacement {
        private final int x;
        private final int y;
        private final int z;
        private final BlockData data;
        private final double radialDistance;

        private BlockPlacement(int x, int y, int z, BlockData data, double radialDistance) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.data = data;
            this.radialDistance = radialDistance;
        }

        private int getX() {
            return x;
        }

        private int getY() {
            return y;
        }

        private int getZ() {
            return z;
        }

        private BlockData getData() {
            return data;
        }

        private double radialDistance() {
            return radialDistance;
        }
    }

    private static final class BlockRestore {
        private final BlockPosition position;
        private final BlockData data;
        private final double radialDistance;

        private BlockRestore(BlockPosition position, BlockData data, double radialDistance) {
            this.position = position;
            this.data = data;
            this.radialDistance = radialDistance;
        }

        private BlockPosition position() {
            return position;
        }

        private BlockData data() {
            return data;
        }

        private double radialDistance() {
            return radialDistance;
        }
    }

    private static final class BlockPosition {
        private final int x;
        private final int y;
        private final int z;
        private final int hash;

        private BlockPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.hash = ((x * 31 + y) * 31) + z;
        }

        private int getX() {
            return x;
        }

        private int getY() {
            return y;
        }

        private int getZ() {
            return z;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BlockPosition other)) {
                return false;
            }
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static final class TemplateInfo {
        private final String resourcePath;
        private final BlockVector size;

        private TemplateInfo(String resourcePath, BlockVector size) {
            this.resourcePath = resourcePath;
            this.size = size;
        }

        public String resourcePath() {
            return resourcePath;
        }

        public BlockVector size() {
            return size.clone();
        }

        private TemplateInfo copy() {
            return new TemplateInfo(resourcePath, size.clone());
        }
    }
}
