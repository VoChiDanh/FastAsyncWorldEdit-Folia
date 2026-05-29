package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.util.FoliaUtil;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.TreeGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.BlockState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A base class for version-specific implementations of the BukkitImplAdapter
 *
 * @param <TAG>          the version-specific NBT tag type
 * @param <SERVER_LEVEL> the version-specific ServerLevel type
 */
public abstract class FaweAdapter<TAG, SERVER_LEVEL> extends CachedBukkitAdapter implements IDelegateBukkitImplAdapter<TAG> {

    protected final BukkitImplAdapter<TAG> parent;
    protected int[] ibdToOrdinal = null;
    protected int[] ordinalToIbdID = null;
    protected boolean initialised = false;
    protected Map<String, List<Property<?>>> allBlockProperties = null;

    protected FaweAdapter(final BukkitImplAdapter<TAG> parent) {
        this.parent = parent;
    }

    @Override
    public void initializeRegistries() {
        parent.initializeRegistries();
    }

    @Override
    public boolean generateTree(
            final TreeGenerator.TreeType treeType,
            final EditSession editSession,
            BlockVector3 blockVector3,
            final World world
    ) {
        TreeType bukkitType = BukkitWorld.toBukkitTreeType(treeType);
        if (bukkitType == TreeType.CHORUS_PLANT) {
            // bukkit skips the feature gen which does this offset normally, so we have to add it back
            blockVector3 = blockVector3.add(BlockVector3.UNIT_Y);
        }
        BlockVector3 target = blockVector3;
        SERVER_LEVEL serverLevel = getServerLevel(world);
        if (FoliaUtil.isFoliaServer()) {
            return generateTreeFolia(bukkitType, editSession, target, world);
        }
        List<BlockState> placed = TaskManager.taskManager().sync(() -> {
            preCaptureStates(serverLevel);
            try {
                if (!world.generateTree(BukkitAdapter.adapt(world, target), bukkitType)) {
                    return null;
                }
                return getCapturedBlockStatesCopy(serverLevel);
            } finally {
                postCaptureBlockStates(serverLevel);
            }
        });

        if (placed == null || placed.isEmpty()) {
            return false;
        }
        for (BlockState blockState : placed) {
            if (blockState == null || blockState.getType() == Material.AIR) {
                continue;
            }
            editSession.setBlock(blockState.getX(), blockState.getY(), blockState.getZ(),
                    BukkitAdapter.adapt(blockState.getBlockData())
            );
        }
        return true;
    }

    private boolean generateTreeFolia(TreeType treeType, EditSession editSession, BlockVector3 target, World world) {
        if (Bukkit.isOwnedByCurrentRegion(world, target.x() >> 4, target.z() >> 4)) {
            return generateTreeFoliaInternal(treeType, editSession, target, world);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Bukkit.getServer().getRegionScheduler().run(
                WorldEditPlugin.getInstance(),
                world,
                target.x() >> 4,
                target.z() >> 4,
                task -> {
                    try {
                        future.complete(generateTreeFoliaInternal(treeType, editSession, target, world));
                    } catch (Throwable throwable) {
                        future.completeExceptionally(throwable);
                    }
                }
        );
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate tree on Folia", e);
        }
    }

    private boolean generateTreeFoliaInternal(TreeType treeType, EditSession editSession, BlockVector3 target, World world) {
        SERVER_LEVEL serverLevel = getServerLevel(world);
        preCaptureStates(serverLevel);
        List<BlockState> placed;
        try {
            if (!world.generateTree(BukkitAdapter.adapt(world, target), treeType)) {
                return false;
            }
            placed = getCapturedBlockStatesCopy(serverLevel);
        } finally {
            postCaptureBlockStates(serverLevel);
        }

        if (placed == null || placed.isEmpty()) {
            return false;
        }
        for (BlockState blockState : placed) {
            if (blockState == null) {
                continue;
            }
            editSession.setBlock(blockState.getX(), blockState.getY(), blockState.getZ(),
                    BukkitAdapter.adapt(blockState.getBlockData())
            );
        }
        return true;
    }

    protected <T> T syncRegion(World world, BlockVector3 point, java.util.function.Supplier<T> supplier) {
        if (!FoliaUtil.isFoliaServer()) {
            return TaskManager.taskManager().sync(supplier);
        }
        if (Bukkit.isOwnedByCurrentRegion(world, point.x() >> 4, point.z() >> 4)) {
            return supplier.get();
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getServer().getRegionScheduler().run(
                WorldEditPlugin.getInstance(),
                world,
                point.x() >> 4,
                point.z() >> 4,
                task -> {
                    try {
                        future.complete(supplier.get());
                    } catch (Throwable throwable) {
                        future.completeExceptionally(throwable);
                    }
                }
        );
        return future.join();
    }

    public void mapFromGlobalPalette(char[] data) {
        assert data.length == 4096;
        ensureInit();
        for (int i = 0; i < 4096; i++) {
            data[i] = (char) this.ibdToOrdinal[data[i]];
        }
    }

    public void mapWithPalette(char[] data, char[] paletteToOrdinal) {
        for (int i = 0; i < 4096; i++) {
            char paletteVal = data[i];
            char val = paletteToOrdinal[paletteVal];
            assert val != Character.MAX_VALUE; // paletteToOrdinal should prevent that
            data[i] = val;
        }
    }

    protected abstract void ensureInit();

    protected abstract void preCaptureStates(SERVER_LEVEL serverLevel);

    protected abstract List<BlockState> getCapturedBlockStatesCopy(SERVER_LEVEL serverLevel);

    protected abstract void postCaptureBlockStates(SERVER_LEVEL serverLevel);

    protected abstract SERVER_LEVEL getServerLevel(World world);

}
