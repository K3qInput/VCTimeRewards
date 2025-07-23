package me.kiro.vctime.managers;

import me.kiro.vctime.VCTimeRewards;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages voice channel time tracking for players
 */
public class TimeManager {
    
    private final VCTimeRewards plugin;
    private final Map<UUID, Long> playerStartTimes;
    private final Map<UUID, String> playerChannels;
    private final Map<UUID, Long> totalTimes;
    
    public TimeManager(VCTimeRewards plugin) {
        this.plugin = plugin;
        this.playerStartTimes = new ConcurrentHashMap<>();
        this.playerChannels = new ConcurrentHashMap<>();
        this.totalTimes = new ConcurrentHashMap<>();
        
        // Start periodic save task
        startSaveTask();
        
        // Load existing data
        loadData();
    }
    
    /**
     * Start tracking time for a player in a voice channel
     */
    public void startTracking(Player player, String channelId) {
        if (player == null || channelId == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Stop any existing tracking
        stopTracking(player);
        
        playerStartTimes.put(playerId, currentTime);
        playerChannels.put(playerId, channelId);
        
        plugin.getLogger().info("Started tracking " + player.getName() + " in channel " + channelId);
    }
    
    /**
     * Stop tracking time for a player
     */
    public void stopTracking(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        stopTracking(player, null);
    }
    
    /**
     * Stop tracking time for a player in a specific channel
     */
    public void stopTracking(Player player, String channelId) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        if (!playerStartTimes.containsKey(playerId)) {
            return;
        }
        
        long startTime = playerStartTimes.get(playerId);
        long currentTime = System.currentTimeMillis();
        long sessionTime = currentTime - startTime;
        
        // Add to total time
        long currentTotal = totalTimes.getOrDefault(playerId, 0L);
        totalTimes.put(playerId, currentTotal + sessionTime);
        
        // Remove from tracking
        playerStartTimes.remove(playerId);
        playerChannels.remove(playerId);
        
        plugin.getLogger().info("Stopped tracking " + player.getName() + 
                              ". Session time: " + (sessionTime / 1000) + " seconds");
        
        // Check for rewards
        checkForRewards(player, currentTotal + sessionTime);
    }
    
    /**
     * Get total time spent by a player (in milliseconds)
     */
    public long getTotalTime(Player player) {
        if (player == null) {
            return 0;
        }
        
        UUID playerId = player.getUniqueId();
        long total = totalTimes.getOrDefault(playerId, 0L);
        
        // Add current session time if player is being tracked
        if (playerStartTimes.containsKey(playerId)) {
            long sessionTime = System.currentTimeMillis() - playerStartTimes.get(playerId);
            total += sessionTime;
        }
        
        return total;
    }
    
    /**
     * Check if a player should receive rewards based on their total time
     */
    private void checkForRewards(Player player, long totalTime) {
        // Convert to hours
        double hours = totalTime / (1000.0 * 60.0 * 60.0);
        
        // Get reward thresholds from config
        Map<Integer, String> rewards = plugin.getConfigUtil().getRewardCommands();
        
        for (Map.Entry<Integer, String> entry : rewards.entrySet()) {
            int threshold = entry.getKey();
            String command = entry.getValue();
            
            if (hours >= threshold && !hasReceivedReward(player, threshold)) {
                // Give reward
                giveReward(player, command, threshold);
                markRewardReceived(player, threshold);
            }
        }
    }
    
    /**
     * Give a reward to a player
     */
    private void giveReward(Player player, String command, int threshold) {
        // Replace placeholders
        String finalCommand = command
                .replace("{player}", player.getName())
                .replace("{threshold}", String.valueOf(threshold));
        
        // Execute command on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        });
        
        plugin.getLogger().info("Gave reward to " + player.getName() + " for " + threshold + " hours");
    }
    
    /**
     * Check if a player has already received a specific reward
     */
    private boolean hasReceivedReward(Player player, int threshold) {
        // This would typically be stored in a database or file
        // For simplicity, we'll use a simple check here
        return false; // TODO: Implement proper reward tracking
    }
    
    /**
     * Mark that a player has received a specific reward
     */
    private void markRewardReceived(Player player, int threshold) {
        // TODO: Implement reward tracking storage
    }
    
    /**
     * Start periodic save task
     */
    private void startSaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAll();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60L * 5L, 20L * 60L * 5L); // Save every 5 minutes
    }
    
    /**
     * Save all player data
     */
    public void saveAll() {
        // TODO: Implement data saving to file or database
        plugin.getLogger().info("Saved player time data");
    }
    
    /**
     * Load player data from storage
     */
    private void loadData() {
        // TODO: Implement data loading from file or database
        plugin.getLogger().info("Loaded player time data");
    }
}
