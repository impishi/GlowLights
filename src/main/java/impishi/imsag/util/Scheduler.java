package impishi.imsag.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public final class Scheduler {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    private Scheduler() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void runTimer(Plugin plugin, Runnable task, long delay, long period) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), Math.max(1, delay), Math.max(1, period));
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }

    public static void runForEntity(Plugin plugin, Entity entity, Runnable task) {
        if (FOLIA) {
            entity.getScheduler().run(plugin, t -> task.run(), null);
        } else {
            task.run();
        }
    }

    public static void cancel(Plugin plugin) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        } else {
            Bukkit.getScheduler().cancelTasks(plugin);
        }
    }
}
