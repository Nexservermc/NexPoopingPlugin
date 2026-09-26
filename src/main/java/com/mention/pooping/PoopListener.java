package com.mention.pooping;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/**
 * 事件监听：蹲下触发、受伤打断、禁止拾取。
 */
public class PoopListener implements Listener {

    private final PoopManager manager;

    public PoopListener(PoopManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            manager.handleSneakDown(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            manager.handleDamage(player);
        }
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (manager.isPoopItem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (manager.isPoopItem(event.getItem())) {
            event.setCancelled(true);
        }
    }
}
