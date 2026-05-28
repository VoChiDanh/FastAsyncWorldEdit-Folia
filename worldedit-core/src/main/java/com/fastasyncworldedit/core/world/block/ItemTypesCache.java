package com.fastasyncworldedit.core.world.block;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.NoCapablePlatformException;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.registry.ItemRegistry;
import com.sk89q.worldedit.world.registry.Registries;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

public final class ItemTypesCache {

    public static void init() {
        registerItemTypeConstants();
        registerPlatformItems();
    }

    private static void registerItemTypeConstants() {
        for (Field field : ItemTypes.class.getDeclaredFields()) {
            if (field.getType() != ItemType.class || !Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            register("minecraft:" + field.getName().toLowerCase(Locale.ROOT));
        }
    }

    private static void registerPlatformItems() {
        try {
            Platform platform = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS);
            Registries registries = platform.getRegistries();
            ItemRegistry itemReg = registries.getItemRegistry();
            for (String key : itemReg.values()) {
                register(key);
            }
        } catch (NoCapablePlatformException ignored) {
        }
    }

    private static void register(String key) {
        if (!ItemType.REGISTRY.getMap().containsKey(key)) {
            ItemType.REGISTRY.register(key, new ItemType(key));
        }
    }
}
