package me.kiro.vctime.managers;

import me.kiro.vctime.VCTimeRewards;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Enhanced time tracking manager with comprehensive persistence and reward handling
 */
public class TimeManager {
    
    private final VCTimeRewards plugin;
    private final Map<UUID, Long> playerStartTimes;
    private final Map<UUID, String> playerChannels;
    private final Map<UUID, Long> totalTimes;
    private final Map<UUID, Set<Integer>> playerRewards; // Track which rewards players have received
    private final File dataFile;
    private FileConfiguration dataConfig;
    private BukkitRunnable saveTask;
    
    public TimeManager(VCTimeRewards plugin) {
        this.plugin = plugin;
        this.playerStartTimes = new ConcurrentHashMap<>();
        this.playerChannels = new ConcurrentHashMap<>();
        this.totalTimes = new ConcurrentHashMap<>();
        this.playerRewards = new ConcurrentHashMap<>();
        
        // Initialize data file
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        
        // Load existing data
        loadData();
        
        // Start periodic tasks
        startSaveTask();
        
        plugin.getLogger().info("TimeManager initialized with " + totalTimes.size() + " player records");
    }
    
    /**
     * Start tracking time for a player in a voice channel
     */
    public void startTracking(Player player, String channelId) {
        if (player == null || channelId == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        startTracking(playerId, channelId);
    }
    
    /**
     * Start tracking time for a player UUID in a voice channel
     */
    public void startTracking(UUID playerId, String channelId) {
        if (playerId == null || channelId == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Stop any existing tracking first
        stopTracking(playerId);
        
        // Start new tracking session
        playerStartTimes.put(playerId, currentTime);
        playerChannels.put(playerId, channelId);
        
        // Initialize total time if not exists
        if (!totalTimes.containsKey(playerId)) {
            totalTimes.put(playerId, 0L);
        }
        
        // Initialize rewards tracking if not exists
        if (!playerRewards.containsKey(playerId)) {
            playerRewards.put(playerId, new HashSet<>());
        }
        
        Player player = plugin.getServer().getPlayer(playerId);
        String playerName = player != null ? player.getName() : getPlayerNameFromUUID(playerId);
        plugin.getLogger().info("Started tracking " + playerName + " in Discord voice channel " + channelId);
    }
    
    /**
     * Stop tracking time for a player
     */
    public void stopTracking(Player player) {
        if (player == null) {
            return;
        }
        stopTracking(player.getUniqueId());
    }
    
    /**
     * Stop tracking time for a player UUID
     */
    public void stopTracking(UUID playerId) {
        if (playerId == null) {
            return;
        }
        
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
        
        Player player = plugin.getServer().getPlayer(playerId);
        String playerName = player != null ? player.getName() : playerId.toString();
        plugin.getLogger().info("Stopped tracking " + playerName + 
                              ". Session time: " + (sessionTime / 1000) + " seconds");
        
        // Check for rewards if player is online
        if (player != null) {
            checkForRewards(player, currentTotal + sessionTime);
        }
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
     * Stop all active tracking sessions
     */
    public void stopAllTracking() {
        // Stop all active sessions and save their time
        for (UUID playerId : new HashMap<>(playerStartTimes).keySet()) {
            stopTracking(playerId);
        }
        plugin.getLogger().info("Stopped all active voice channel tracking sessions");
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
        UUID playerId = player.getUniqueId();
        Set<Integer> rewards = playerRewards.getOrDefault(playerId, new HashSet<>());
        return rewards.contains(threshold);
    }
    
    /**
     * Mark that a player has received a specific reward
     */
    private void markRewardReceived(Player player, int threshold) {
        UUID playerId = player.getUniqueId();
        playerRewards.computeIfAbsent(playerId, k -> new HashSet<>()).add(threshold);
        
        // Send notification if enabled
        if (plugin.getConfigUtil().shouldSendNotifications()) {
            String message = plugin.getConfigUtil().getNotificationMessage()
                    .replace("{time}", String.valueOf(threshold));
            player.sendMessage(message);
        }
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
     * Save all player data to YAML file
     */
    public void saveAll() {
        try {
            if (dataConfig == null) {
                dataConfig = new YamlConfiguration();
            }
            
            // Save total times
            for (Map.Entry<UUID, Long> entry : totalTimes.entrySet()) {
                dataConfig.set("players." + entry.getKey().toString() + ".totalTime", entry.getValue());
            }
            
            // Save received rewards
            for (Map.Entry<UUID, Set<Integer>> entry : playerRewards.entrySet()) {
                dataConfig.set("players." + entry.getKey().toString() + ".rewards", 
                             entry.getValue().toArray(new Integer[0]));
            }
            
            // Save current tracking sessions
            for (Map.Entry<UUID, Long> entry : playerStartTimes.entrySet()) {
                UUID playerId = entry.getKey();
                long sessionTime = System.currentTimeMillis() - entry.getValue();
                long currentTotal = totalTimes.getOrDefault(playerId, 0L);
                dataConfig.set("players." + playerId.toString() + ".totalTime", currentTotal + sessionTime);
            }
            
            dataConfig.save(dataFile);
            plugin.getLogger().info("Saved time data for " + totalTimes.size() + " players");
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data: " + e.getMessage());
        }
    }
    
    /**
     * Load player data from YAML file
     */
    private void loadData() {
        try {
            if (!dataFile.exists()) {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
                plugin.getLogger().info("Created new player data file");
                return;
            }
            
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            
            if (dataConfig.getConfigurationSection("players") == null) {
                plugin.getLogger().info("No existing player data found");
                return;
            }
            
            // Load player data
            for (String uuidString : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    
                    // Load total time
                    long totalTime = dataConfig.getLong("players." + uuidString + ".totalTime", 0);
                    if (totalTime > 0) {
                        totalTimes.put(playerId, totalTime);
                    }
                    
                    // Load received rewards
                    java.util.List<Integer> rewardsList = dataConfig.getIntegerList("players." + uuidString + ".rewards");
                    if (!rewardsList.isEmpty()) {
                        playerRewards.put(playerId, new HashSet<>(rewardsList));
                    }
                    
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in data file: " + uuidString);
                }
            }
            
            plugin.getLogger().info("Loaded time data for " + totalTimes.size() + " players");
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load player data: " + e.getMessage());
        }
    }
    
    /**
     * Get player name from UUID (for offline players)
     */
    private String getPlayerNameFromUUID(UUID playerId) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            return player.getName();
        }
        
        // Try to get from Bukkit's offline player
        try {
            return plugin.getServer().getOfflinePlayer(playerId).getName();
        } catch (Exception e) {
            return playerId.toString();
        }
    }
    
    /**
     * Get current tracking status for a player
     */
    public boolean isTracking(UUID playerId) {
        return playerStartTimes.containsKey(playerId);
    }
    
    /**
     * Get the channel a player is currently being tracked in
     */
    public String getCurrentChannel(UUID playerId) {
        return playerChannels.get(playerId);
    }
    
    /**
     * Get all currently tracked players
     */
    public Set<UUID> getTrackedPlayers() {
        return new HashSet<>(playerStartTimes.keySet());
    }
    
    /**
     * Cleanup method called when plugin disables
     */
    public void shutdown() {
        // Stop all tracking and save final times
        stopAllTracking();
        saveAll();
        
        // Cancel save task
        if (saveTask != null) {
            saveTask.cancel();
        }
        
        plugin.getLogger().info("TimeManager shutdown complete");
    }
}
