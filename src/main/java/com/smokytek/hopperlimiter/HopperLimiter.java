package com.smokytek.hopperlimiter;

import org.bukkit.plugin.java.JavaPlugin;

import com.smokytek.hopperlimiter.listeners.HopperPlacementListeners;

public class HopperLimiter extends JavaPlugin {
	
	@Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new HopperPlacementListeners(this), this);

        getLogger().info("HopperLimiter è stato abilitato!");
        getLogger().info("Limite hopper per chunk: " + getConfig().getInt("max-hoppers-per-chunk", 10)); // Logga il limite
    }

    @Override
    public void onDisable() {
        getLogger().info("HopperLimiter è stato disabilitato!");
    }

}
