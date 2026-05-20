package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.util.TaskManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BukkitTaskManager extends TaskManager {

    private final Plugin plugin;
    private final AtomicInteger foliaTaskIds = new AtomicInteger();
    private final Map<Integer, Object> foliaTasks = new ConcurrentHashMap<>();

    public BukkitTaskManager(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int repeat(@Nonnull final Runnable runnable, final int interval) {
        return repeat(runnable, interval, interval);
    }

    @Override
    public int repeat(@Nonnull final Runnable runnable, final int interval, final int delay) {
        if (FoliaSupport.isFolia()) {
            return registerFoliaTask(FoliaSupport.runGlobalRepeating(this.plugin, runnable, delay, interval));
        }
        return this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, runnable, delay, interval);
    }

    @Override
    public int repeatAsync(@Nonnull final Runnable runnable, final int interval) {
        if (FoliaSupport.isFolia()) {
            return registerFoliaTask(FoliaSupport.runAsyncRepeating(this.plugin, runnable, interval, interval));
        }
        return this.plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(this.plugin, runnable, interval, interval);
    }

    @Override
    public void async(@Nonnull final Runnable runnable) {
        if (FoliaSupport.isFolia()) {
            FoliaSupport.runAsync(this.plugin, runnable);
            return;
        }
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, runnable).getTaskId();
    }

    @Override
    public void task(@Nonnull final Runnable runnable) {
        if (FoliaSupport.isFolia()) {
            FoliaSupport.runGlobal(this.plugin, runnable);
            return;
        }
        this.plugin.getServer().getScheduler().runTask(this.plugin, runnable).getTaskId();
    }

    @Override
    public void later(@Nonnull final Runnable runnable, final int delay) {
        if (FoliaSupport.isFolia()) {
            FoliaSupport.runGlobalDelayed(this.plugin, runnable, delay);
            return;
        }
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, runnable, delay).getTaskId();
    }

    @Override
    public void laterAsync(@Nonnull final Runnable runnable, final int delay) {
        if (FoliaSupport.isFolia()) {
            FoliaSupport.runAsyncDelayed(this.plugin, runnable, delay);
            return;
        }
        this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, runnable, delay);
    }

    @Override
    public void cancel(final int task) {
        if (task != -1) {
            if (FoliaSupport.isFolia()) {
                Object foliaTask = foliaTasks.remove(task);
                if (foliaTask != null) {
                    FoliaSupport.cancelTask(foliaTask);
                }
                return;
            }
            Bukkit.getScheduler().cancelTask(task);
        }
    }

    private int registerFoliaTask(Object task) {
        int id = foliaTaskIds.incrementAndGet();
        foliaTasks.put(id, task);
        return id;
    }

}
