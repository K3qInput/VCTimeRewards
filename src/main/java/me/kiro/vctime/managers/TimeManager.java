package me.kiro.vctime.managers;

import me.kiro.vctime.VCTimeRewards;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Enhanced time tracking manager with comprehensive persistence and reward handling
 */
public class TimeManager {
    
    private final VCTimeRewards plugin;
    private final Map<UUID, Long> playerStartTimes;
    private final Map<UUID, String> playerChannels;
    private final Map<UUID, Long> totalTimes;
    private final Map<UUID, Set<Long>> playerRewards; // Track which rewards players have received (in minutes)
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
        startRewardCheckTask();
        
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
        // Convert to minutes
        long minutes = totalTime / (1000L * 60L);
        
        // Debug logging
        plugin.getLogger().info("Checking rewards for " + player.getName() + " - Total time: " + minutes + " minutes");
        
        // Get voice reward thresholds from config (now in minutes)
        Map<Long, String> rewards = plugin.getConfigUtil().getVoiceRewardCommands();
        plugin.getLogger().info("Available rewards: " + rewards.keySet());
        
        for (Map.Entry<Long, String> entry : rewards.entrySet()) {
            long threshold = entry.getKey();
            String command = entry.getValue();
            
            boolean hasReward = hasReceivedReward(player, threshold);
            plugin.getLogger().info("Threshold " + threshold + "min: player has " + minutes + "min, already received: " + hasReward);
            
            if (minutes >= threshold && !hasReward) {
                // Give reward
                giveReward(player, command, threshold);
                markRewardReceived(player, threshold);
                plugin.getLogger().info("REWARD GIVEN: " + player.getName() + " reached " + threshold + " minutes!");
            }
        }
    }
    
    /**
     * Give a reward to a player
     */
    private void giveReward(Player player, String command, long threshold) {
        // Format threshold for display (convert minutes to readable format)
        String thresholdDisplay = formatMinutes(threshold);
        
        // Replace placeholders
        String finalCommand = command
                .replace("{player}", player.getName())
                .replace("{threshold}", thresholdDisplay);
        
        // Execute command on main thread with fallback formats
        Bukkit.getScheduler().runTask(plugin, () -> {
            String[] commandFormats = {
                finalCommand, // Original format
                finalCommand.replace("minecraft:", ""), // Without namespace
                "/" + finalCommand, // With slash prefix
                "give " + player.getName() + " " + extractItemFromCommand(finalCommand) // Simple give format
            };
            
            boolean executed = false;
            for (String cmdFormat : commandFormats) {
                try {
                    boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdFormat);
                    if (success) {
                        plugin.getLogger().info("✓ REWARD SUCCESS: " + cmdFormat);
                        executed = true;
                        break;
                    }
                } catch (Exception e) {
                    plugin.getLogger().fine("Command format failed: " + cmdFormat);
                }
            }
            
            if (!executed) {
                plugin.getLogger().severe("✗ ALL REWARD FORMATS FAILED for: " + finalCommand);
            }
        });
        
        plugin.getLogger().info("Gave reward to " + player.getName() + " for " + thresholdDisplay);
    }
    
    /**
     * Extract item and quantity from give command for fallback
     */
    private String extractItemFromCommand(String command) {
        try {
            // Extract from commands like "minecraft:give player minecraft:item quantity"
            String[] parts = command.split(" ");
            if (parts.length >= 3) {
                String item = parts[2].replace("minecraft:", "");
                String quantity = parts.length > 3 ? parts[3] : "1";
                return item + " " + quantity;
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Could not extract item from command: " + command);
        }
        return "coal 1"; // Safe fallback
    }
    
    /**
     * Format minutes into readable time string
     */
    private String formatMinutes(long minutes) {
        if (minutes < 60) {
            return minutes + "m";
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + "h";
            } else {
                return hours + "h" + remainingMinutes + "m";
            }
        }
    }
    
    /**
     * Check if a player has already received a specific reward
     */
    private boolean hasReceivedReward(Player player, long threshold) {
        UUID playerId = player.getUniqueId();
        Set<Long> rewards = playerRewards.getOrDefault(playerId, new HashSet<>());
        return rewards.contains(threshold);
    }
    
    /**
     * Mark that a player has received a specific reward
     */
    private void markRewardReceived(Player player, long threshold) {
        UUID playerId = player.getUniqueId();
        playerRewards.computeIfAbsent(playerId, k -> new HashSet<>()).add(threshold);
        
        // Send notification if enabled
        if (plugin.getConfigUtil().shouldSendNotifications()) {
            String thresholdDisplay = formatMinutes(threshold);
            String message = plugin.getConfigUtil().getNotificationMessage()
                    .replace("{time}", thresholdDisplay);
            // Translate color codes including hex colors
            player.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors(message));
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
     * Start periodic reward check task for active players
     */
    private void startRewardCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check rewards for all currently tracked players
                for (UUID playerId : playerStartTimes.keySet()) {
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        long totalTime = getTotalTime(player);
                        checkForRewards(player, totalTime);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 30L, 20L * 30L); // Check every 30 seconds
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
            for (Map.Entry<UUID, Set<Long>> entry : playerRewards.entrySet()) {
                dataConfig.set("players." + entry.getKey().toString() + ".rewards", 
                             entry.getValue().toArray(new Long[0]));
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
                    java.util.List<Long> rewardsList = dataConfig.getLongList("players." + uuidString + ".rewards");
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
     * Get total time for a player by UUID (in minutes)
     */
    public long getTotalTime(UUID playerId) {
        long totalMillis = totalTimes.getOrDefault(playerId, 0L);
        
        // Add current session time if player is being tracked
        if (playerStartTimes.containsKey(playerId)) {
            long sessionTime = System.currentTimeMillis() - playerStartTimes.get(playerId);
            totalMillis += sessionTime;
        }
        
        // Convert milliseconds to minutes
        return totalMillis / (1000L * 60L);
    }
    
    /**
     * Get current session time for a player (in minutes)
     */
    public long getSessionTime(UUID playerId) {
        if (!playerStartTimes.containsKey(playerId)) {
            return 0;
        }
        
        long startTime = playerStartTimes.get(playerId);
        long currentTime = System.currentTimeMillis();
        return (currentTime - startTime) / (1000 * 60); // Convert to minutes
    }
    
    /**
     * Check if player is currently in a tracked voice channel
     */
    public boolean isPlayerInTrackedVoiceChannel(UUID playerId) {
        return playerStartTimes.containsKey(playerId);
    }
    
    /**
     * Get player's rank based on total time (1 = highest time)
     */
    public int getPlayerRank(UUID playerId) {
        if (!totalTimes.containsKey(playerId)) {
            return -1; // Player not found
        }
        
        long playerTime = totalTimes.get(playerId);
        int rank = 1;
        
        for (long otherTime : totalTimes.values()) {
            if (otherTime > playerTime) {
                rank++;
            }
        }
        
        return rank;
    }
    
    /**
     * Get top players by voice time
     */
    public List<PlayerTimeEntry> getTopPlayers(int limit) {
        return totalTimes.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(entry -> new PlayerTimeEntry(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get average session time for a player (in minutes)
     */
    public long getAverageSessionTime(UUID playerId) {
        // For now, return a simple calculation
        // In a full implementation, you'd track all session durations
        long totalTime = getTotalTime(playerId);
        int sessionCount = getSessionCount(playerId);
        
        if (sessionCount == 0) {
            return 0;
        }
        
        return totalTime / sessionCount;
    }
    
    /**
     * Get total number of sessions for a player
     */
    public int getSessionCount(UUID playerId) {
        // For now, estimate based on total time
        // In a full implementation, you'd track actual session count
        long totalTime = getTotalTime(playerId);
        
        if (totalTime == 0) {
            return 0;
        }
        
        // Estimate: assume average session is 30 minutes
        int estimatedSessions = (int) Math.max(1, totalTime / 30);
        return estimatedSessions;
    }
    
    /**
     * Inner class for leaderboard entries
     */
    public static class PlayerTimeEntry {
        private final UUID playerId;
        private final long totalTime;
        
        public PlayerTimeEntry(UUID playerId, long totalTime) {
            this.playerId = playerId;
            this.totalTime = totalTime;
        }
        
        public UUID getPlayerId() {
            return playerId;
        }
        
        public long getTotalTime() {
            return totalTime;
        }
        
        public String getPlayerName() {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            return player.getName() != null ? player.getName() : "Unknown";
        }
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
