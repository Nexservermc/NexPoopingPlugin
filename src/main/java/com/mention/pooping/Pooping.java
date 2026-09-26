package com.mention.pooping;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Pooping extends JavaPlugin {

    private PoopManager poopManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.poopManager = new PoopManager(this);
        getServer().getPluginManager().registerEvents(new PoopListener(poopManager), this);

        PluginCommand poopCommand = getCommand("poop");
        if (poopCommand != null) {
            poopCommand.setExecutor(new PoopCommand(this, poopManager));
        }

        poopManager.start();
    }

    @Override
    public void onDisable() {
        if (poopManager != null) {
            poopManager.shutdown();
        }
    }
}
