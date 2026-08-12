package com.github.shop.ShopTeleport;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

public class ShopTeleport extends JavaPlugin implements CommandExecutor, TabCompleter {

    private final Map<String, ShopLocation> shops = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadShops();

        // 注册命令执行器
        getCommand("shop").setExecutor(this);
        getCommand("shop").setTabCompleter(this);

        getLogger().info("ShopTeleport 插件已启用! 作者: 25db");
        getLogger().info("支持 Folia 1.21.11 服务端");
    }

    @Override
    public void onDisable() {
        saveShopsToConfigAsync();
        getLogger().info("ShopTeleport 插件已禁用!");
    }

    private void loadShops() {
        FileConfiguration config = getConfig();
        ConfigurationSection shopsSection = config.getConfigurationSection("shops");
        if (shopsSection == null) return;

        for (String shopName : shopsSection.getKeys(false)) {
            String worldName = shopsSection.getString(shopName + ".world");
            double x = shopsSection.getDouble(shopName + ".x");
            double y = shopsSection.getDouble(shopName + ".y");
            double z = shopsSection.getDouble(shopName + ".z");
            float yaw = (float) shopsSection.getDouble(shopName + ".yaw", 0);
            float pitch = (float) shopsSection.getDouble(shopName + ".pitch", 0);

            World world = getServer().getWorld(worldName);
            if (world != null) {
                Location loc = new Location(world, x, y, z, yaw, pitch);
                shops.put(shopName.toLowerCase(), new ShopLocation(shopName, loc));
            }
        }
        getLogger().info("已加载 " + shops.size() + " 个商店");
    }

    /** Folia 兼容：保存到主线程 */
    private void saveShopsToConfigAsync() {
        Runnable saveTask = () -> {
            FileConfiguration config = getConfig();
            config.set("shops", null); // 清除旧数据

            for (Map.Entry<String, ShopLocation> entry : shops.entrySet()) {
                ShopLocation shop = entry.getValue();
                String path = "shops." + shop.getName();
                Location loc = shop.getLocation();

                config.set(path + ".world", loc.getWorld().getName());
                config.set(path + ".x", loc.getX());
                config.set(path + ".y", loc.getY());
                config.set(path + ".z", loc.getZ());
                config.set(path + ".yaw", loc.getYaw());
                config.set(path + ".pitch", loc.getPitch());
            }
            saveConfig();
        };

        if (isFolia()) {
            Bukkit.getMainScheduler().execute(this, saveTask, 0L);
        } else {
            saveTask.run();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set":
                return handleSetShop(sender, args);
            case "delete":
            case "del":
                return handleDeleteShop(sender, args);
            case "list":
                return handleListShops(sender);
            case "tp":
            case "teleport":
            case "go":
                return handleTeleport(sender, args);
            case "help":
                sendHelp(sender);
                return true;
            case "reload":
                return handleReload(sender);
            default:
                sender.sendMessage(ChatColor.RED + "未知命令! 使用 /shop help 查看帮助");
                return true;
        }
    }

    private boolean handleSetShop(CommandSender sender, String[] args) {
        // 检查OP权限
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "你没有权限设置商店! 此命令仅限OP使用.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /shop set <商店名称>");
            return true;
        }

        Player player = (Player) sender;
        String shopName = args[1];
        Location loc = player.getLocation();

        // 检查是否已存在
        if (shops.containsKey(shopName.toLowerCase())) {
            sender.sendMessage(ChatColor.YELLOW + "商店 '" + shopName + "' 已存在，已更新位置!");
        } else {
            sender.sendMessage(ChatColor.GREEN + "成功创建商店: " + ChatColor.GOLD + shopName);
        }

        shops.put(shopName.toLowerCase(), new ShopLocation(shopName, loc));

        // Folia 兼容：保存配置到主线程
        Runnable saveRunnable = () -> saveShopsToConfigAsync();
        if (isFolia()) {
            Bukkit.getMainScheduler().execute(this, saveRunnable, 0L);
        } else {
            saveRunnable.run();
        }

        sender.sendMessage(ChatColor.GRAY + "位置: " + ChatColor.WHITE +
            String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()) +
            ChatColor.GRAY + " 世界: " + ChatColor.WHITE + loc.getWorld().getName());

        return true;
    }

    private boolean handleDeleteShop(CommandSender sender, String[] args) {
        // 检查OP权限
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "你没有权限删除商店! 此命令仅限OP使用.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /shop delete <商店名称>");
            return true;
        }

        String shopName = args[1].toLowerCase();

        if (!shops.containsKey(shopName)) {
            sender.sendMessage(ChatColor.RED + "商店 '" + args[1] + "' 不存在!");
            return true;
        }

        ShopLocation removed = shops.remove(shopName);

        // Folia 兼容：保存配置到主线程
        Runnable saveRunnable = () -> saveShopsToConfigAsync();
        if (isFolia()) {
            Bukkit.getMainScheduler().execute(this, saveRunnable, 0L);
        } else {
            saveRunnable.run();
        }
        sender.sendMessage(ChatColor.GREEN + "成功删除商店: " + ChatColor.GOLD + removed.getName());

        return true;
    }

    private boolean handleListShops(CommandSender sender) {
        if (shops.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "目前没有设置任何商店");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "========== " + ChatColor.GOLD + "商店列表" + ChatColor.GREEN + " ==========");
        for (ShopLocation shop : shops.values()) {
            Location loc = shop.getLocation();
            sender.sendMessage(ChatColor.GOLD + "• " + ChatColor.WHITE + shop.getName() +
                ChatColor.GRAY + " - " + ChatColor.WHITE +
                String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()) +
                ChatColor.DARK_GRAY + " | " + ChatColor.AQUA + loc.getWorld().getName());
        }
        sender.sendMessage(ChatColor.GREEN + "共 " + ChatColor.YELLOW + shops.size() + ChatColor.GREEN + " 个商店");

        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /shop tp <商店名称>");
            sender.sendMessage(ChatColor.GRAY + "使用 /shop list 查看所有商店");
            return true;
        }

        Player player = (Player) sender;
        String shopName = args[1].toLowerCase();

        if (!shops.containsKey(shopName)) {
            sender.sendMessage(ChatColor.RED + "商店 '" + args[1] + "' 不存在!");
            sender.sendMessage(ChatColor.GRAY + "使用 /shop list 查看所有商店");
            return true;
        }

        ShopLocation shop = shops.get(shopName);
        Location loc = shop.getLocation();

        // Folia 兼容：TeleportAsync 在 player 所在区域线程上调度
        // 通过 player.scheduler() 确保在正确的线程执行
        try {
            // 使用 player 自己的 RegionScheduler 执行传送
            player.scheduler().run(player, (plugin, runnable) -> {
                // 在玩家的区域内执行传送
                org.bukkit.scheduler.BukkitRunnable task = new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            player.teleport(loc);
                            player.sendMessage(ChatColor.GREEN + "已传送到商店: " + ChatColor.GOLD + shop.getName());
                        } else {
                            player.sendMessage(ChatColor.RED + "传送失败，玩家已离线!");
                        }
                    }
                };
                task.run(plugin);
                return null;
            }, null, 0L);
        } catch (Exception e) {
            // 回退到直接传送
            if (player.isOnline()) {
                player.teleport(loc);
                player.sendMessage(ChatColor.GREEN + "已传送到商店: " + ChatColor.GOLD + shop.getName());
            }
        }

        sender.sendMessage(ChatColor.YELLOW + "正在传送到 " + ChatColor.GOLD + shop.getName() + ChatColor.YELLOW + "...");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令!");
            return true;
        }

        shops.clear();
        reloadConfig();
        loadShops();
        sender.sendMessage(ChatColor.GREEN + "配置已重新加载! 共加载 " + shops.size() + " 个商店");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "========== " + ChatColor.GOLD + "商店传送帮助" + ChatColor.GREEN + " ==========");
        sender.sendMessage(ChatColor.YELLOW + "/shop set <名称>" + ChatColor.GRAY + " - 设置商店位置 (仅OP)");
        sender.sendMessage(ChatColor.YELLOW + "/shop delete <名称>" + ChatColor.GRAY + " - 删除商店 (仅OP)");
        sender.sendMessage(ChatColor.YELLOW + "/shop list" + ChatColor.GRAY + " - 列出所有商店");
        sender.sendMessage(ChatColor.YELLOW + "/shop tp <名称>" + ChatColor.GRAY + " - 传送到商店");
        sender.sendMessage(ChatColor.YELLOW + "/shop reload" + ChatColor.GRAY + " - 重载配置 (仅OP)");
        sender.sendMessage(ChatColor.DARK_GRAY + "Folia 1.21.11 兼容版本");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> subCommands = new ArrayList<>();
            subCommands.add("set");
            subCommands.add("delete");
            subCommands.add("list");
            subCommands.add("tp");
            subCommands.add("help");
            if (sender.isOp()) {
                subCommands.add("reload");
            }
            for (String sub : subCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();

            if (subCommand.equals("delete") || subCommand.equals("del") || subCommand.equals("tp") || subCommand.equals("teleport") || subCommand.equals("go")) {
                for (String shopName : shops.keySet()) {
                    if (shopName.startsWith(input)) {
                        completions.add(shopName);
                    }
                }
            }
        }

        return completions;
    }

    /** 检测是否为 Folia 服务端 */
    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // 内部类：商店位置
    private static class ShopLocation {
        private final String name;
        private final Location location;

        public ShopLocation(String name, Location location) {
            this.name = name;
            this.location = location;
        }

        public String getName() {
            return name;
        }

        public Location getLocation() {
            return location;
        }
    }
}
