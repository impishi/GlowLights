package impishi.imsag;

import impishi.imsag.command.GlowCommand;
import impishi.imsag.config.GlowConfig;
import impishi.imsag.light.LightManager;
import impishi.imsag.listener.PlayerListener;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class GlowLights extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private GlowConfig cfg;
    private LightManager lights;
    private NamespacedKey offKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        offKey = new NamespacedKey(this, "disabled");
        cfg = new GlowConfig(this);
        lights = new LightManager(this, cfg);
        getServer().getPluginManager().registerEvents(new PlayerListener(lights), this);

        PluginCommand cmd = getCommand("glowlights");
        if (cmd != null) {
            GlowCommand handler = new GlowCommand(this, lights, cfg);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        lights.start();

        getServer().getConsoleSender().sendMessage(
                LEGACY.deserialize("&6ɢʟᴏᴡ&eʟɪɢʜᴛꜱ &f» &aᴇɴᴀʙʟᴇᴅ"));
    }

    @Override
    public void onDisable() {
        if (lights != null) lights.stop();
        getServer().getConsoleSender().sendMessage(
                LEGACY.deserialize("&6ɢʟᴏᴡ&eʟɪɢʜᴛꜱ &f» &7ᴅɪꜱᴀʙʟᴇᴅ"));
    }

    public void reloadAll() {
        cfg.reload();
        lights.reload();
    }

    public NamespacedKey offKey() {
        return offKey;
    }
}
