package com.smokytek.hopperlimiter.listeners;

import com.smokytek.hopperlimiter.HopperLimiter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HopperPlacementListeners implements Listener {

    private final int maxHoppersPerChunk;
    public final ConcurrentHashMap<Chunk, Integer> hopperCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private static final long COOLDOWN_MILLIS = 2000;

    public HopperPlacementListeners(HopperLimiter plugin) {
        this.maxHoppersPerChunk = plugin.getConfig().getInt("max-hoppers-per-chunk", 10);
    }

    private int getHoppersInChunk(Chunk chunk) {
        if (hopperCounts.containsKey(chunk)) {
            return hopperCounts.get(chunk);
        }

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
        hopperCounts.put(chunk, hoppers);
        return hoppers;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.HOPPER) {
            Player player = event.getPlayer();

            if (player.hasPermission("hopperlimiter.bypass")) {
                return; // 
            }

            Chunk chunk = event.getBlock().getChunk();
            int hoppersInChunk = getHoppersInChunk(chunk);

            if (hoppersInChunk >= maxHoppersPerChunk) {
                event.setCancelled(true);
                sendCooledMessage(player, "&cNon puoi posizionare più di " + maxHoppersPerChunk + " hopper in questo chunk!");
            } else {
                hopperCounts.put(chunk, hoppersInChunk + 1);
            }
        }
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.HOPPER) {
            Chunk chunk = event.getBlock().getChunk();
            hopperCounts.computeIfPresent(chunk, (c, count) -> count > 1 ? count - 1 : null);
        }
    }

    private void sendCooledMessage(Player player, String message) {
        long now = Instant.now().toEpochMilli();
        UUID uuid = player.getUniqueId();

        if (!cooldowns.containsKey(uuid) || now - cooldowns.get(uuid) > COOLDOWN_MILLIS) {
            Component formatted = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
            player.sendMessage(formatted);
            cooldowns.put(uuid, now);
        }
    }
}
