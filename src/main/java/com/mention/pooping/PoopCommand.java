package com.mention.pooping;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * /poop reload —— 重载插件配置。
 */
public class PoopCommand implements CommandExecutor {

    private final Pooping plugin;
    private final PoopManager manager;

    public PoopCommand(Pooping plugin, PoopManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(ChatColor.YELLOW + "用法: /" + label + " reload");
            return true;
        }
        plugin.reloadConfig();
        manager.loadConfig();
        sender.sendMessage(ChatColor.GREEN + "[Pooping] 配置已重载。");
        return true;
    }
}
