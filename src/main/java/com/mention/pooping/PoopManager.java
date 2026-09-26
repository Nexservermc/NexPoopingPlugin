package com.mention.pooping;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 拉屎玩法核心逻辑：触发判定、进度条、屎实体生成与生命周期管理。
 * 所有可调参数均从 config.yml 读取，支持 /poop reload 热重载。
 */
public class PoopManager {

    private final JavaPlugin plugin;
    private final NamespacedKey poopKey;

    // ---- 配置项 ----
    private long tapWindowMs;
    private long holdWindowMs;
    private Set<GameMode> allowedGameModes;
    private String title;
    private int barLength;
    private int progressTicks;
    private NamedTextColor startColor;
    private NamedTextColor fillColor;
    private boolean cancelOnDamage;
    private Material poopMaterial;
    private String nameFormat;
    private double nameViewRadius;
    private long poopLifetimeMs;
    private boolean canPickup;
    private boolean limitEnabled;
    private int maxPerPlayer;
    private String limitMessage;
    private long limitMessageCooldownMs;

    // ---- 运行时状态 ----
    private final Map<UUID, SneakState> sneakStates = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();
    private final Map<Item, PoopData> poopItems = new ConcurrentHashMap<>();
    private final Map<UUID, Long> limitMessageTimes = new ConcurrentHashMap<>();

    private BukkitTask maintenanceTask;

    public PoopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.poopKey = new NamespacedKey(plugin, "poop_id");
        loadConfig();
    }

    /** 从配置文件读取全部参数。 */
    public void loadConfig() {
        FileConfiguration cfg = plugin.getConfig();

        this.tapWindowMs = cfg.getLong("trigger.tap-window-ms", 500L);
        this.holdWindowMs = cfg.getLong("trigger.hold-window-ms", 500L);

        Set<GameMode> modes = EnumSet.noneOf(GameMode.class);
        for (String name : cfg.getStringList("trigger.game-modes")) {
            try {
                modes.add(GameMode.valueOf(name.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("未知的游戏模式: " + name + "（已忽略）");
            }
        }
        this.allowedGameModes = modes.isEmpty()
                ? EnumSet.of(GameMode.SURVIVAL, GameMode.ADVENTURE, GameMode.CREATIVE)
                : modes;

        this.title = cfg.getString("progress.title", "正在拉屎");
        this.barLength = Math.max(1, cfg.getInt("progress.bar-length", 20));
        long durationMs = Math.max(50L, cfg.getLong("progress.duration-ms", 3000L));
        this.progressTicks = Math.max(1, (int) (durationMs / 50L));
        this.startColor = parseColor(cfg.getString("progress.start-color", "GREEN"));
        this.fillColor = parseColor(cfg.getString("progress.fill-color", "RED"));
        this.cancelOnDamage = cfg.getBoolean("progress.cancel-on-damage", true);

        this.poopMaterial = Material.matchMaterial(cfg.getString("poop.material", "GLOWSTONE_DUST"));
        if (this.poopMaterial == null) {
            plugin.getLogger().warning("未知的物品类型，已回退为 GLOWSTONE_DUST");
            this.poopMaterial = Material.GLOWSTONE_DUST;
        }
        this.nameFormat = cfg.getString("poop.name-format", "%player%的屎");
        this.nameViewRadius = cfg.getDouble("poop.name-view-radius", 8.0);
        this.poopLifetimeMs = Math.max(1L, cfg.getLong("poop.lifetime-seconds", 60L)) * 1000L;
        this.canPickup = cfg.getBoolean("poop.can-pickup", false);
        this.limitEnabled = cfg.getBoolean("poop.limit.enabled", false);
        this.maxPerPlayer = Math.max(0, cfg.getInt("poop.limit.max-per-player", 1));
        this.limitMessage = cfg.getString("poop.limit.message", "你拉不出来了！地上的屎太多了");
        this.limitMessageCooldownMs = Math.max(0L, cfg.getLong("poop.limit.message-cooldown-ms", 2000L));
    }

    private NamedTextColor parseColor(String name) {
        if (name == null) return NamedTextColor.WHITE;
        NamedTextColor color = NamedTextColor.NAMES.value(name.trim().toLowerCase());
        if (color == null) {
            plugin.getLogger().warning("未知的颜色: " + name + "（已回退为 WHITE）");
            return NamedTextColor.WHITE;
        }
        return color;
    }

    public void start() {
        maintenanceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::maintain, 20L, 10L);
    }

    public void shutdown() {
        if (maintenanceTask != null) maintenanceTask.cancel();
        for (BukkitTask task : activeTasks.values()) task.cancel();
        activeTasks.clear();
        for (Item item : poopItems.keySet()) item.remove();
        poopItems.clear();
        sneakStates.clear();
    }

    // ==================== 触发判定 ====================

    /** 处理一次"按下蹲下"（PlayerToggleSneakEvent 且 isSneaking=true）。 */
    public void handleSneakDown(Player player) {
        if (!allowedGameModes.contains(player.getGameMode())) return;
        if (activeTasks.containsKey(player.getUniqueId())) return;
        if (limitEnabled && countPoops(player.getUniqueId()) >= maxPerPlayer) {
            sendLimitMessage(player);
            return;
        }

        long now = System.currentTimeMillis();
        SneakState state = sneakStates.computeIfAbsent(player.getUniqueId(), k -> new SneakState());

        if (state.count == 1 && now - state.lastTime <= tapWindowMs) {
            state.count = 2;
            state.lastTime = now;
        } else if (state.count == 2 && now - state.lastTime <= holdWindowMs) {
            state.count = 0;
            startPoop(player);
        } else {
            state.count = 1;
            state.lastTime = now;
        }
    }

    public void handleQuit(Player player) {
        sneakStates.remove(player.getUniqueId());
        limitMessageTimes.remove(player.getUniqueId());
    }

    /** 达到上限时向玩家发送提示（带冷却，防止刷屏）。 */
    private void sendLimitMessage(Player player) {
        long now = System.currentTimeMillis();
        Long last = limitMessageTimes.get(player.getUniqueId());
        if (last != null && now - last < limitMessageCooldownMs) return;
        limitMessageTimes.put(player.getUniqueId(), now);
        player.sendActionBar(Component.text(limitMessage).color(NamedTextColor.RED));
    }

    /** 受伤打断：取消进行中的拉屎。 */
    public void handleDamage(Player player) {
        if (!cancelOnDamage) return;
        if (activeTasks.containsKey(player.getUniqueId())) {
            cancelPoop(player.getUniqueId());
        }
    }

    // ==================== 进度条 ====================

    private void startPoop(Player player) {
        UUID id = player.getUniqueId();
        player.sendActionBar(renderBar(0.0));

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int elapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !player.isSneaking()) {
                    cancelPoop(id);
                    return;
                }
                elapsed++;
                double progress = (double) elapsed / progressTicks;
                if (progress >= 1.0) {
                    cancelPoop(id);
                    spawnPoop(player);
                    return;
                }
                player.sendActionBar(renderBar(progress));
            }
        }, 1L, 1L);

        activeTasks.put(id, task);
    }

    private void cancelPoop(UUID id) {
        BukkitTask task = activeTasks.remove(id);
        if (task != null) task.cancel();
        Player player = Bukkit.getPlayer(id);
        if (player != null && player.isOnline()) {
            player.sendActionBar(Component.empty());
        }
    }

    private Component renderBar(double progress) {
        int filled = (int) Math.round(progress * barLength);
        if (filled < 0) filled = 0;
        if (filled > barLength) filled = barLength;
        int remaining = barLength - filled;
        return Component.text(title + " ")
                .color(NamedTextColor.WHITE)
                .append(Component.text("|".repeat(filled)).color(fillColor))
                .append(Component.text("|".repeat(remaining)).color(startColor));
    }

    // ==================== 生成屎 ====================

    private void spawnPoop(Player player) {
        Location loc = player.getLocation();
        Component poopName = Component.text(nameFormat.replace("%player%", player.getName()))
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false);

        ItemStack stack = new ItemStack(poopMaterial);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(poopName);
        meta.getPersistentDataContainer().set(poopKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        stack.setItemMeta(meta);

        Item item = loc.getWorld().dropItem(loc, stack);
        // 关键：把悬浮名设置到掉落物实体本身，否则只显示物品默认名
        item.customName(poopName);
        item.setInvulnerable(true);
        item.setCustomNameVisible(false);
        if (canPickup) {
            item.setCanMobPickup(true);
            item.setPickupDelay(10);
        } else {
            item.setCanMobPickup(false);
            item.setPickupDelay(32767);
        }

        poopItems.put(item, new PoopData(player.getUniqueId(),
                System.currentTimeMillis() + poopLifetimeMs));
    }

    /** 统计某玩家当前在服务器内的屎数量。 */
    private int countPoops(UUID ownerId) {
        int count = 0;
        for (PoopData data : poopItems.values()) {
            if (data.ownerId.equals(ownerId)) count++;
        }
        return count;
    }

    // ==================== 维护任务 ====================

    private void maintain() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Item, PoopData> entry : poopItems.entrySet()) {
            Item item = entry.getKey();
            if (!item.isValid()) {
                poopItems.remove(item);
                continue;
            }
            if (now >= entry.getValue().expireAt) {
                item.remove();
                poopItems.remove(item);
                continue;
            }
            boolean near = isPlayerNear(item.getLocation(), nameViewRadius);
            if (item.isCustomNameVisible() != near) {
                item.setCustomNameVisible(near);
            }
        }
    }

    private boolean isPlayerNear(Location loc, double radius) {
        World world = loc.getWorld();
        if (world == null) return false;
        double radiusSq = radius * radius;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(loc) <= radiusSq) return true;
        }
        return false;
    }

    public boolean isPoopItem(Item item) {
        return poopItems.containsKey(item);
    }

    private static final class SneakState {
        int count;
        long lastTime;
    }

    /** 每坨屎的运行时数据：拥有者与过期时间。 */
    private static final class PoopData {
        final UUID ownerId;
        final long expireAt;

        PoopData(UUID ownerId, long expireAt) {
            this.ownerId = ownerId;
            this.expireAt = expireAt;
        }
    }
}
