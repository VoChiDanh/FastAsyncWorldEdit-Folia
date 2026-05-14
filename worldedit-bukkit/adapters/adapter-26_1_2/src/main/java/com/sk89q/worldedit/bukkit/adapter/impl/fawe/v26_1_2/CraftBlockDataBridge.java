package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v26_1_2;

import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class CraftBlockDataBridge {

    private static final Method BLOCK_DATA_FACTORY = resolveBlockDataFactory();

    private CraftBlockDataBridge() {
    }

    public static CraftBlockData fromData(BlockState blockState) {
        try {
            return (CraftBlockData) BLOCK_DATA_FACTORY.invoke(null, blockState);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to create CraftBlockData from native block state", e);
        }
    }

    private static Method resolveBlockDataFactory() {
        for (String methodName : new String[]{"createData", "fromData"}) {
            try {
                Method method = CraftBlockData.class.getDeclaredMethod(methodName, BlockState.class);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new IllegalStateException("No CraftBlockData factory method is available");
    }

}
