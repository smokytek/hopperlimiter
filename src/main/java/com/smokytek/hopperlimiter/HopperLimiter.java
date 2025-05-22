package com.smokytek.hopperlimiter;

import com.smokytek.hopperlimiter.listeners.HopperPlacementListeners;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HopperLimiter extends JavaPlugin {

    public HopperPlacementListeners hopperPlacementListeners;
    private Set<String> excludedWorlds;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        
        excludedWorlds = new HashSet<>(getConfig().getStringList("excluded-worlds"));

        hopperPlacementListeners = new HopperPlacementListeners(this);
        getServer().getPluginManager().registerEvents(hopperPlacementListeners, this);

        updateHopperCountsAsync();
    }

    public void updateHopperCountsAsync() {
        long startTime = System.currentTimeMillis();

        getLogger().info("Inizio scansione async degli hopper nei chunk caricati...");

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            Map<Chunk, Integer> tempCounts = new ConcurrentHashMap<>();
            AtomicInteger totalChunks = new AtomicInteger();
            AtomicInteger totalHoppers = new AtomicInteger();

            for (World world : Bukkit.getWorlds()) {
                if (excludedWorlds.contains(world.getName())) {
                    getLogger().info("Mondo escluso dalla scansione: " + world.getName());
                    continue;
                }

                for (Chunk chunk : world.getLoadedChunks()) {
                    int hoppers = 0;

                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                        	int minY = chunk.getWorld().getMinHeight();
                        	int maxY = chunk.getWorld().getMaxHeight();

                        	for (int y = minY; y < maxY; y++) {
                        	    Block block = chunk.getBlock(x, y, z);
                        	    if (block.getType() == Material.HOPPER) {
                        	        hoppers++;
                        	    }
                        	}

                        }
                    }

                    if (hoppers > 0) {
                        tempCounts.put(chunk, hoppers);
                        totalChunks.incrementAndGet();
                        totalHoppers.addAndGet(hoppers);
                    }
                }
            }

            Bukkit.getScheduler().runTask(this, () -> {
                hopperPlacementListeners.hopperCounts.clear();
                hopperPlacementListeners.hopperCounts.putAll(tempCounts);

                long elapsed = System.currentTimeMillis() - startTime;
                getLogger().info("Scansione completata: " + totalHoppers.get() + " hopper trovati in " +
                        totalChunks.get() + " chunk. Tempo: " + elapsed + " ms.");
            });
        });
    }
}
