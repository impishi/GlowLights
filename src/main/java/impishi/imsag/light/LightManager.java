package impishi.imsag.light;

import impishi.imsag.GlowLights;
import impishi.imsag.config.GlowConfig;
import impishi.imsag.util.LongIntMap;
import impishi.imsag.util.Scheduler;
import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class LightManager {

    private final GlowLights plugin;
    private final GlowConfig cfg;

    private final Map<UUID, Emitter> emitters = new ConcurrentHashMap<>();
    private final Map<UUID, Viewer> viewers = new ConcurrentHashMap<>();
    private final BlockData[][] lightData = new BlockData[16][2];
    private final AtomicLong version = new AtomicLong();

    private long[] clusterKeys = new long[32];
    private int[] clusterLevels = new int[32];

    private volatile boolean running;
    private long tick;

    public LightManager(GlowLights plugin, GlowConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        for (int level = 1; level <= 15; level++) {
            Light dry = (Light) Material.LIGHT.createBlockData();
            dry.setLevel(level);
            lightData[level][0] = dry;

            Light wet = (Light) Material.LIGHT.createBlockData();
            wet.setLevel(level);
            wet.setWaterlogged(true);
            lightData[level][1] = wet;
        }
    }

    public void start() {
        running = true;
        Scheduler.runTimer(plugin, this::tickAll, 1L, cfg.updateInterval());
    }

    public void stop() {
        running = false;
        Scheduler.cancel(plugin);
        for (Viewer v : viewers.values()) {
            Player p = Bukkit.getPlayer(v.id);
            if (p != null && p.isOnline()) clear(p, v);
        }
        viewers.clear();
        emitters.clear();
    }

    public void reload() {
        stop();
        start();
    }

    public void register(Player player) {
        viewers.computeIfAbsent(player.getUniqueId(), Viewer::new);
    }

    public void unregister(Player player) {
        Viewer v = viewers.remove(player.getUniqueId());
        if (v != null) clear(player, v);
    }

    public void onBlockChange() {
        version.incrementAndGet();
    }

    private void tickAll() {
        if (!running || !cfg.enabled()) return;
        tick++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scheduler.runForEntity(plugin, p, () -> tickPlayer(p));
        }
    }

    private void tickPlayer(Player p) {
        if (!p.isOnline()) return;

        if (cfg.isDisabledWorld(p.getWorld().getName())) {
            Viewer v = viewers.get(p.getUniqueId());
            if (v != null) clear(p, v);
            return;
        }

        Viewer v = viewers.computeIfAbsent(p.getUniqueId(), Viewer::new);

        if (isOff(p)) {
            clear(p, v);
            return;
        }

        Location eye = p.getEyeLocation();
        long ver = version.get();

        int interval = cfg.scanInterval();
        if (!v.nearby) interval *= cfg.farMultiplier();

        boolean due = tick - v.lastScan >= interval;
        boolean bumped = ver != v.lastVersion && tick - v.lastScan >= 2;

        v.lastX = eye.getBlockX();
        v.lastY = eye.getBlockY();
        v.lastZ = eye.getBlockZ();
        v.lastVersion = ver;

        if (due || bumped) {
            v.lastScan = tick;
            scan(p, eye, v);
        }

        buildDesired(p, v, eye);
        sendDiff(p, v);
    }

    private void scan(Player p, Location eye, Viewer v) {
        double r = cfg.viewRadius() + 8;
        double nearSq = (double) cfg.nearRadius() * cfg.nearRadius();
        boolean near = false;

        for (Entity e : p.getWorld().getNearbyEntities(eye, r, r, r)) {
            if (e.isDead() || !e.isValid()) continue;
            if (!shouldGlow(e)) continue;
            if (levelOf(e) <= 0) continue;

            emitters.computeIfAbsent(e.getUniqueId(), id -> new Emitter(e));

            Location loc = e.getLocation();
            double dx = loc.getX() - eye.getX();
            double dy = loc.getY() - eye.getY();
            double dz = loc.getZ() - eye.getZ();
            if (dx * dx + dy * dy + dz * dz <= nearSq) near = true;
        }

        List<UUID> gone = null;
        for (Emitter em : emitters.values()) {
            Entity e = em.entity;
            if (e.isDead() || !e.isValid() || !shouldGlow(e) || levelOf(e) <= 0) {
                if (gone == null) gone = new ArrayList<>();
                gone.add(e.getUniqueId());
            }
        }
        if (gone != null) {
            emitters.keySet().removeAll(gone);
            version.incrementAndGet();
        }

        v.nearby = near;
    }

    private void buildDesired(Player p, Viewer v, Location eye) {
        World world = p.getWorld();
        double maxDistSq = (double) cfg.viewRadius() * cfg.viewRadius();
        LongIntMap desired = v.desired;
        desired.clear();

        for (Emitter em : emitters.values()) {
            Entity e = em.entity;
            if (e.getWorld() != world) continue;

            Location loc = e.getLocation();
            double dx = loc.getX() - eye.getX();
            double dy = loc.getY() - eye.getY();
            double dz = loc.getZ() - eye.getZ();
            if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;

            int level = levelOf(e);
            if (level <= 0) continue;

            int bx = loc.getBlockX();
            int by = loc.getBlockY() + 1;
            int bz = loc.getBlockZ();
            if (em.needsUpdate(bx, by, bz, level)) {
                em.cluster = buildCluster(world, bx, by, bz, level);
                em.lastX = bx;
                em.lastY = by;
                em.lastZ = bz;
                em.lastLevel = level;
            }
            if (em.cluster == null) continue;

            long[] keys = em.cluster.keys;
            int[] levels = em.cluster.levels;
            for (int i = 0; i < em.cluster.size; i++) {
                desired.mergeMax(keys[i], levels[i]);
            }
        }
    }

    private Emitter.Cluster buildCluster(World world, int ox, int oy, int oz, int base) {
        int radius = cfg.clusterRadius();
        int height = cfg.clusterHeight();
        int fall = cfg.falloff();
        int cap = (radius * 2 + 1) * (radius * 2 + 1) * height;

        if (clusterKeys.length < cap) {
            clusterKeys = new long[cap];
            clusterLevels = new int[cap];
        }

        int n = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int level = base - (Math.abs(x) + Math.abs(z)) * fall;
                if (level < 1) continue;

                for (int y = 0; y < height; y++) {
                    int ly = level - y * fall;
                    if (ly < 1) continue;

                    int bx = ox + x;
                    int by = oy + y;
                    int bz = oz + z;
                    if (!world.isChunkLoaded(bx >> 4, bz >> 4)) continue;

                    Block b = world.getBlockAt(bx, by, bz);
                    Material type = b.getType();
                    if (!cfg.replaceable().contains(type)) continue;

                    clusterKeys[n] = Pos.pack(bx, by, bz);
                    clusterLevels[n] = Math.min(ly, 15) | (type == Material.WATER ? 0x10 : 0);
                    n++;
                }
            }
        }

        if (n == 0) return null;
        long[] keys = new long[n];
        int[] levels = new int[n];
        System.arraycopy(clusterKeys, 0, keys, 0, n);
        System.arraycopy(clusterLevels, 0, levels, 0, n);
        return new Emitter.Cluster(keys, levels, n);
    }

    private void sendDiff(Player p, Viewer v) {
        LongIntMap current = v.current;
        LongIntMap desired = v.desired;
        LongIntMap toSend = v.toSend;
        LongIntMap toRevert = v.toRevert;

        toSend.clear();
        toRevert.clear();

        for (int i = 0; i < desired.capacity(); i++) {
            if (!desired.isPresent(i)) continue;
            long key = desired.keyAt(i);
            int value = desired.valueAt(i);
            if (current.get(key) != value) toSend.put(key, value);
        }
        for (int i = 0; i < current.capacity(); i++) {
            if (!current.isPresent(i)) continue;
            long key = current.keyAt(i);
            if (!desired.contains(key)) toRevert.put(key, 1);
        }

        if (toSend.size() > 0 || toRevert.size() > 0) {
            send(p, toRevert, toSend, p.getWorld());
            current.clear();
            for (int i = 0; i < desired.capacity(); i++) {
                if (desired.isPresent(i)) current.put(desired.keyAt(i), desired.valueAt(i));
            }
        }
    }

    private void send(Player p, LongIntMap revert, LongIntMap send, World world) {
        Map<Position, BlockData> batch = new HashMap<>();

        for (int i = 0; i < revert.capacity(); i++) {
            if (!revert.isPresent(i)) continue;
            long key = revert.keyAt(i);
            int x = Pos.x(key);
            int y = Pos.y(key);
            int z = Pos.z(key);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            batch.put(Position.block(x, y, z), world.getBlockAt(x, y, z).getBlockData());
        }

        for (int i = 0; i < send.capacity(); i++) {
            if (!send.isPresent(i)) continue;
            long key = send.keyAt(i);
            int value = send.valueAt(i);
            int level = value & 0xF;
            boolean wet = (value & 0x10) != 0;
            int x = Pos.x(key);
            int y = Pos.y(key);
            int z = Pos.z(key);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            batch.put(Position.block(x, y, z), lightData[level][wet ? 1 : 0]);
        }

        if (!batch.isEmpty()) p.sendMultiBlockChange(batch);
    }

    private void clear(Player p, Viewer v) {
        if (v.current.isEmpty()) return;
        v.toRevert.clear();
        for (int i = 0; i < v.current.capacity(); i++) {
            if (v.current.isPresent(i)) v.toRevert.put(v.current.keyAt(i), 1);
        }
        v.toSend.clear();
        send(p, v.toRevert, v.toSend, p.getWorld());
        v.current.clear();
        v.desired.clear();
    }

    private boolean shouldGlow(Entity e) {
        if (e instanceof Player other) {
            if (cfg.noSpectators() && other.getGameMode() == GameMode.SPECTATOR) return false;
            if (cfg.noVanished() && other.hasMetadata("vanished")) return false;
            return true;
        }
        if (e instanceof Item) return cfg.trackItems();
        if (e instanceof LivingEntity living) {
            return cfg.trackMobs() && cfg.mobWhitelist().contains(living.getType());
        }
        return false;
    }

    private int levelOf(Entity e) {
        if (e instanceof Item item) {
            return cfg.brightness(item.getItemStack().getType());
        }
        if (e instanceof LivingEntity living) {
            int best = -1;
            for (EquipmentSlot slot : cfg.slots()) {
                ItemStack stack = living.getEquipment().getItem(slot);
                if (stack.getType().isAir()) continue;
                int lvl = cfg.brightness(stack.getType());
                if (lvl > best) best = lvl;
            }
            return best;
        }
        return -1;
    }

    public boolean isOff(Player p) {
        Byte flag = p.getPersistentDataContainer().get(plugin.offKey(), PersistentDataType.BYTE);
        return flag != null && flag == 1;
    }

    public void toggle(Player p) {
        boolean off = !isOff(p);
        p.getPersistentDataContainer().set(plugin.offKey(), PersistentDataType.BYTE, (byte) (off ? 1 : 0));
        if (off) {
            Viewer v = viewers.get(p.getUniqueId());
            if (v != null) clear(p, v);
        }
        version.incrementAndGet();
    }

    public int viewerCount() {
        return viewers.size();
    }

    public int lightCount() {
        int total = 0;
        for (Viewer v : viewers.values()) total += v.current.size();
        return total;
    }

    public int emitterCount() {
        return emitters.size();
    }

    private static final class Viewer {
        final UUID id;
        final LongIntMap current = new LongIntMap(256);
        final LongIntMap desired = new LongIntMap(256);
        final LongIntMap toSend = new LongIntMap(64);
        final LongIntMap toRevert = new LongIntMap(64);
        int lastX = Integer.MIN_VALUE;
        int lastY;
        int lastZ;
        long lastVersion = -1;
        long lastScan;
        boolean nearby = true;

        Viewer(UUID id) {
            this.id = id;
        }
    }

    private static final class Emitter {
        final Entity entity;
        Cluster cluster;
        int lastX = Integer.MIN_VALUE;
        int lastY;
        int lastZ;
        int lastLevel = -1;

        Emitter(Entity entity) {
            this.entity = entity;
        }

        boolean needsUpdate(int x, int y, int z, int level) {
            return cluster == null || x != lastX || y != lastY || z != lastZ || level != lastLevel;
        }

        record Cluster(long[] keys, int[] levels, int size) {
        }
    }
}
