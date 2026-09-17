package impishi.imsag.listener;

import impishi.imsag.light.LightManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {

    private final LightManager lights;

    public PlayerListener(LightManager lights) {
        this.lights = lights;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        lights.register(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lights.unregister(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        lights.onBlockChange();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        lights.onBlockChange();
    }
}
