package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.Fawe;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class FoliaSupport {

    private static final boolean FOLIA;
    private static final Method GET_GLOBAL_REGION_SCHEDULER;
    private static final Method GET_REGION_SCHEDULER;
    private static final Method GET_ASYNC_SCHEDULER;
    private static final Method GLOBAL_RUN;
    private static final Method GLOBAL_RUN_DELAYED;
    private static final Method GLOBAL_RUN_AT_FIXED_RATE;
    private static final Method GLOBAL_CANCEL_TASKS;
    private static final Method REGION_RUN_LOCATION;
    private static final Method REGION_RUN_DELAYED_LOCATION;
    private static final Method ASYNC_RUN_NOW;
    private static final Method ASYNC_RUN_DELAYED;
    private static final Method ASYNC_RUN_AT_FIXED_RATE;
    private static final Method ASYNC_CANCEL_TASKS;
    private static final Method TASK_CANCEL;
    private static final Method IS_GLOBAL_TICK_THREAD;
    private static final Method IS_OWNED_ENTITY;
    private static final Method IS_OWNED_BLOCK;
    private static final Method IS_OWNED_LOCATION;

    static {
        boolean folia = false;
        Method getGlobalRegionScheduler = null;
        Method getRegionScheduler = null;
        Method getAsyncScheduler = null;
        Method globalRun = null;
        Method globalRunDelayed = null;
        Method globalRunAtFixedRate = null;
        Method globalCancelTasks = null;
        Method regionRunLocation = null;
        Method regionRunDelayedLocation = null;
        Method asyncRunNow = null;
        Method asyncRunDelayed = null;
        Method asyncRunAtFixedRate = null;
        Method asyncCancelTasks = null;
        Method taskCancel = null;
        Method isGlobalTickThread = null;
        Method isOwnedEntity = null;
        Method isOwnedBlock = null;
        Method isOwnedLocation = null;

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            Class<?> scheduledTask = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            Class<?> globalRegionScheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            Class<?> regionScheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            Class<?> asyncScheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");

            getGlobalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
            getRegionScheduler = Bukkit.class.getMethod("getRegionScheduler");
            getAsyncScheduler = Bukkit.class.getMethod("getAsyncScheduler");

            globalRun = globalRegionScheduler.getMethod("run", Plugin.class, Consumer.class);
            globalRunDelayed = globalRegionScheduler.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
            globalRunAtFixedRate = globalRegionScheduler.getMethod(
                    "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class
            );
            globalCancelTasks = globalRegionScheduler.getMethod("cancelTasks", Plugin.class);

            regionRunLocation = regionScheduler.getMethod("run", Plugin.class, Location.class, Consumer.class);
            regionRunDelayedLocation = regionScheduler.getMethod(
                    "runDelayed", Plugin.class, Location.class, Consumer.class, long.class
            );

            asyncRunNow = asyncScheduler.getMethod("runNow", Plugin.class, Consumer.class);
            asyncRunDelayed = asyncScheduler.getMethod(
                    "runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class
            );
            asyncRunAtFixedRate = asyncScheduler.getMethod(
                    "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class
            );
            asyncCancelTasks = asyncScheduler.getMethod("cancelTasks", Plugin.class);
            taskCancel = scheduledTask.getMethod("cancel");

            isGlobalTickThread = Bukkit.class.getMethod("isGlobalTickThread");
            isOwnedEntity = Bukkit.class.getMethod("isOwnedByCurrentRegion", Entity.class);
            isOwnedBlock = Bukkit.class.getMethod("isOwnedByCurrentRegion", Block.class);
            isOwnedLocation = Bukkit.class.getMethod("isOwnedByCurrentRegion", Location.class);
            folia = true;
        } catch (ReflectiveOperationException ignored) {
        }

        FOLIA = folia;
        GET_GLOBAL_REGION_SCHEDULER = getGlobalRegionScheduler;
        GET_REGION_SCHEDULER = getRegionScheduler;
        GET_ASYNC_SCHEDULER = getAsyncScheduler;
        GLOBAL_RUN = globalRun;
        GLOBAL_RUN_DELAYED = globalRunDelayed;
        GLOBAL_RUN_AT_FIXED_RATE = globalRunAtFixedRate;
        GLOBAL_CANCEL_TASKS = globalCancelTasks;
        REGION_RUN_LOCATION = regionRunLocation;
        REGION_RUN_DELAYED_LOCATION = regionRunDelayedLocation;
        ASYNC_RUN_NOW = asyncRunNow;
        ASYNC_RUN_DELAYED = asyncRunDelayed;
        ASYNC_RUN_AT_FIXED_RATE = asyncRunAtFixedRate;
        ASYNC_CANCEL_TASKS = asyncCancelTasks;
        TASK_CANCEL = taskCancel;
        IS_GLOBAL_TICK_THREAD = isGlobalTickThread;
        IS_OWNED_ENTITY = isOwnedEntity;
        IS_OWNED_BLOCK = isOwnedBlock;
        IS_OWNED_LOCATION = isOwnedLocation;
    }

    private FoliaSupport() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static boolean isGlobalTickThread() {
        if (!FOLIA) {
            return Bukkit.isPrimaryThread();
        }
        return invokeBoolean(null, IS_GLOBAL_TICK_THREAD);
    }

    public static boolean isOwnedByCurrentRegion(@Nonnull Entity entity) {
        return !FOLIA || invokeBoolean(null, IS_OWNED_ENTITY, entity);
    }

    public static boolean isOwnedByCurrentRegion(@Nonnull Block block) {
        return !FOLIA || invokeBoolean(null, IS_OWNED_BLOCK, block);
    }

    public static boolean isOwnedByCurrentRegion(@Nonnull Location location) {
        return !FOLIA || invokeBoolean(null, IS_OWNED_LOCATION, location);
    }

    public static Object runGlobal(@Nonnull Plugin plugin, @Nonnull Runnable runnable) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(runnable);
        return invokeTask(globalScheduler(), GLOBAL_RUN, plugin, wrap(runnable));
    }

    public static Object runGlobalDelayed(@Nonnull Plugin plugin, @Nonnull Runnable runnable, long delayTicks) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(runnable);
        return invokeTask(globalScheduler(), GLOBAL_RUN_DELAYED, plugin, wrap(runnable), normalizeTickDelay(delayTicks));
    }

    public static Object runGlobalRepeating(
            @Nonnull Plugin plugin,
            @Nonnull Runnable runnable,
            long initialDelayTicks,
            long periodTicks
    ) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(runnable);
        return invokeTask(
                globalScheduler(),
                GLOBAL_RUN_AT_FIXED_RATE,
                plugin,
                wrap(runnable),
                normalizeTickDelay(initialDelayTicks),
                normalizeTickDelay(periodTicks)
        );
    }

    public static Object runAsync(@Nonnull Plugin plugin, @Nonnull Runnable runnable) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(runnable);
        return invokeTask(asyncScheduler(), ASYNC_RUN_NOW, plugin, wrap(runnable));
    }

    public static Object runAsyncDelayed(@Nonnull Plugin plugin, @Nonnull Runnable runnable, long delayTicks) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(runnable);
        return invokeTask(
                asyncScheduler(),
                ASYNC_RUN_DELAYED,
                plugin,
                wrap(runnable),
                ticksToMillis(delayTicks),
                TimeUnit.MILLISECONDS
        );
    }

    public static Object runAsyncRepeating(
            @Nonnull Plugin plugin,
            @Nonnull Runnable runnable,
            long initialDelayTicks,
            long periodTicks
    ) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(runnable);
        return invokeTask(
                asyncScheduler(),
                ASYNC_RUN_AT_FIXED_RATE,
                plugin,
                wrap(runnable),
                ticksToMillis(initialDelayTicks),
                ticksToMillis(periodTicks),
                TimeUnit.MILLISECONDS
        );
    }

    public static void runAtLocation(@Nonnull Plugin plugin, @Nonnull Location location, @Nonnull Runnable runnable) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(location);
        Objects.requireNonNull(runnable);
        if (!FOLIA || isOwnedByCurrentRegion(location)) {
            runnable.run();
            return;
        }
        invokeTask(regionScheduler(), REGION_RUN_LOCATION, plugin, location, wrap(runnable));
    }

    public static void runAtLocationDelayed(
            @Nonnull Plugin plugin,
            @Nonnull Location location,
            @Nonnull Runnable runnable,
            long delayTicks
    ) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(location);
        Objects.requireNonNull(runnable);
        if (!FOLIA) {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
            return;
        }
        invokeTask(regionScheduler(), REGION_RUN_DELAYED_LOCATION, plugin, location, wrap(runnable), normalizeTickDelay(delayTicks));
    }

    public static <T> T callAtEntity(@Nonnull Plugin plugin, @Nonnull Entity entity, @Nonnull Supplier<T> supplier) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(entity);
        Objects.requireNonNull(supplier);
        if (!FOLIA || isOwnedByCurrentRegion(entity)) {
            return supplier.get();
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        Object scheduler = invokeObject(entity, "getScheduler");
        Object scheduled = invokeTask(
                scheduler,
                "run",
                new Class<?>[]{Plugin.class, Consumer.class, Runnable.class},
                plugin,
                wrap(() -> complete(future, supplier)),
                (Runnable) () -> future.completeExceptionally(new IllegalStateException("Entity scheduler retired"))
        );
        if (scheduled == null) {
            throw new IllegalStateException("Entity scheduler retired");
        }
        return join(future);
    }

    public static boolean teleport(@Nonnull Plugin plugin, @Nonnull Entity entity, @Nonnull Location location) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(entity);
        Objects.requireNonNull(location);
        if (!FOLIA) {
            return entity.teleport(location);
        }
        try {
            Object future = entity.getClass().getMethod("teleportAsync", Location.class).invoke(entity, location);
            if (future instanceof CompletableFuture) {
                @SuppressWarnings("unchecked") CompletableFuture<Boolean> teleportFuture = (CompletableFuture<Boolean>) future;
                return join(teleportFuture);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to invoke teleportAsync", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Failed to invoke teleportAsync", cause);
        }
        return callAtEntity(plugin, entity, () -> entity.teleport(location));
    }

    public static void runAtBlock(@Nonnull Plugin plugin, @Nonnull Block block, @Nonnull Runnable runnable) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(block);
        runAtLocation(plugin, block.getLocation(), runnable);
    }

    public static void cancelTask(@Nonnull Object task) {
        Objects.requireNonNull(task);
        invokeTask(task, TASK_CANCEL);
    }

    public static void cancelPluginTasks(@Nonnull Plugin plugin) {
        Objects.requireNonNull(plugin);
        if (!FOLIA) {
            Bukkit.getScheduler().cancelTasks(plugin);
            return;
        }
        invokeTask(globalScheduler(), GLOBAL_CANCEL_TASKS, plugin);
        invokeTask(asyncScheduler(), ASYNC_CANCEL_TASKS, plugin);
    }

    private static Consumer<Object> wrap(Runnable runnable) {
        return ignored -> {
            Thread previous = null;
            if (Fawe.instance() != null && isGlobalTickThread()) {
                previous = Fawe.instance().getMainThread();
                Fawe.instance().setMainThread();
            }
            try {
                runnable.run();
            } finally {
                if (previous != null && Fawe.instance() != null) {
                    Fawe.instance().setMainThread(previous);
                }
            }
        };
    }

    private static <T> void complete(CompletableFuture<T> future, Supplier<T> supplier) {
        try {
            future.complete(supplier.get());
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private static Object globalScheduler() {
        return invokeTask(null, GET_GLOBAL_REGION_SCHEDULER);
    }

    private static Object regionScheduler() {
        return invokeTask(null, GET_REGION_SCHEDULER);
    }

    private static Object asyncScheduler() {
        return invokeTask(null, GET_ASYNC_SCHEDULER);
    }

    private static long normalizeTickDelay(long ticks) {
        return Math.max(1L, ticks);
    }

    private static long ticksToMillis(long ticks) {
        return normalizeTickDelay(ticks) * 50L;
    }

    private static Object invokeObject(Object target, String method) {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke Folia method " + method, e);
        }
    }

    private static Object invokeTask(Object target, String method, Class<?>[] parameterTypes, Object... args) {
        try {
            return target.getClass().getMethod(method, parameterTypes).invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke Folia method " + method, e);
        }
    }

    private static Object invokeTask(Object target, Method method, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to invoke Folia scheduler", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Failed to invoke Folia scheduler", cause);
        }
    }

    private static boolean invokeBoolean(Object target, Method method, Object... args) {
        return (Boolean) invokeTask(target, method, args);
    }

}
