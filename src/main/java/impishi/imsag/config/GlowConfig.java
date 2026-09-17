package impishi.imsag.config;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class GlowConfig {

    private final JavaPlugin plugin;

    private boolean enabled;
    private int viewRadius;
    private int updateInterval;
    private int scanInterval;
    private int nearRadius;
    private int farMultiplier;
    private boolean trackMobs;
    private boolean trackItems;
    private boolean noVanished;
    private boolean noSpectators;
    private int clusterRadius;
    private int clusterHeight;
    private int falloff;

    private Set<String> disabledWorlds;
    private Set<EntityType> mobWhitelist;
    private Set<EquipmentSlot> slots;
    private EnumMap<Material, Integer> brightness;
    private Set<Material> replaceable;

    public GlowConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        enabled = cfg.getBoolean("enabled", true);
        viewRadius = Math.max(4, cfg.getInt("view-radius", 32));
        updateInterval = Math.max(1, cfg.getInt("update-interval-ticks", 1));
        scanInterval = Math.max(1, cfg.getInt("emitter-scan-interval-ticks", 4));
        nearRadius = Math.max(4, cfg.getInt("near-radius", 16));
        farMultiplier = Math.max(1, cfg.getInt("far-interval-multiplier", 4));
        trackMobs = cfg.getBoolean("track-mobs", true);
        trackItems = cfg.getBoolean("track-items", true);
        noVanished = cfg.getBoolean("suppress-vanished", true);
        noSpectators = cfg.getBoolean("suppress-spectator", true);
        clusterRadius = clamp(cfg.getInt("cluster-radius", 1), 0, 2);
        clusterHeight = clamp(cfg.getInt("cluster-height", 2), 1, 3);
        falloff = Math.max(0, cfg.getInt("cluster-falloff", 2));

        disabledWorlds = new HashSet<>(cfg.getStringList("disabled-worlds"));

        mobWhitelist = EnumSet.noneOf(EntityType.class);
        for (String name : cfg.getStringList("mob-whitelist")) {
            try {
                mobWhitelist.add(EntityType.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        slots = EnumSet.noneOf(EquipmentSlot.class);
        for (String name : cfg.getStringList("equipment-slots")) {
            try {
                slots.add(EquipmentSlot.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (slots.isEmpty()) {
            slots.add(EquipmentSlot.HAND);
            slots.add(EquipmentSlot.OFF_HAND);
            slots.add(EquipmentSlot.HEAD);
        }

        brightness = new EnumMap<>(Material.class);
        ConfigurationSection section = cfg.getConfigurationSection("item-brightness");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Material mat = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
                if (mat != null) {
                    brightness.put(mat, clamp(section.getInt(key), 1, 15));
                }
            }
        }

        replaceable = EnumSet.noneOf(Material.class);
        for (String name : cfg.getStringList("replaceable-blocks")) {
            Material mat = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
            if (mat != null) replaceable.add(mat);
        }
        if (replaceable.isEmpty()) {
            replaceable.add(Material.AIR);
            replaceable.add(Material.CAVE_AIR);
        }
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : Math.min(v, max);
    }

    public boolean enabled() { return enabled; }
    public int viewRadius() { return viewRadius; }
    public int updateInterval() { return updateInterval; }
    public int scanInterval() { return scanInterval; }
    public int nearRadius() { return nearRadius; }
    public int farMultiplier() { return farMultiplier; }
    public boolean trackMobs() { return trackMobs; }
    public boolean trackItems() { return trackItems; }
    public boolean noVanished() { return noVanished; }
    public boolean noSpectators() { return noSpectators; }
    public int clusterRadius() { return clusterRadius; }
    public int clusterHeight() { return clusterHeight; }
    public int falloff() { return falloff; }

    public Set<EntityType> mobWhitelist() { return mobWhitelist; }
    public Set<EquipmentSlot> slots() { return slots; }
    public Set<Material> replaceable() { return replaceable; }

    public int brightness(Material mat) {
        Integer v = brightness.get(mat);
        return v == null ? -1 : v;
    }

    public boolean isDisabledWorld(String world) {
        return disabledWorlds.contains(world);
    }
}
