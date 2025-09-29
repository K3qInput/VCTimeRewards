package me.kiro.vctime.managers;

import me.kiro.vctime.VCTimeRewards;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced statistics tracking manager
 * Tracks detailed player statistics for achievements and analytics
 */
public class StatisticsManager {
    
    private final VCTimeRewards plugin;
    private final Map<UUID, PlayerStatistics> playerStats;
    private YamlConfiguration statsData;
    private File statsDataFile;
    
    public StatisticsManager(VCTimeRewards plugin) {
        this.plugin = plugin;
        this.playerStats = new ConcurrentHashMap<>();
        setupDataFile();
        loadData();
    }
    
    /**
     * Setup the statistics data file
     */
    private void setupDataFile() {
        statsDataFile = new File(plugin.getDataFolder(), "player-statistics.yml");
        if (!statsDataFile.exists()) {
            try {
                statsDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create statistics data file: " + e.getMessage());
            }
        }
        statsData = YamlConfiguration.loadConfiguration(statsDataFile);
    }
    
    /**
     * Load statistics data from file
     */
    private void loadData() {
        if (statsData.isConfigurationSection("players")) {
            for (String uuidStr : statsData.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidStr);
                    PlayerStatistics stats = PlayerStatistics.fromConfig(statsData, "players." + uuidStr);
                    playerStats.put(playerId, stats);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in statistics data: " + uuidStr);
                }
            }
        }
        
        plugin.getLogger().info("Loaded statistics for " + playerStats.size() + " players");
    }
    
    /**
     * Save statistics data to file
     */
    public void saveData() {
        try {
            for (Map.Entry<UUID, PlayerStatistics> entry : playerStats.entrySet()) {
                String path = "players." + entry.getKey().toString();
                entry.getValue().saveToConfig(statsData, path);
            }
            
            statsData.save(statsDataFile);
            plugin.getLogger().fine("Saved statistics for " + playerStats.size() + " players");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save statistics data: " + e.getMessage());
        }
    }
    
    /**
     * Record a voice session start
     */
    public void recordVoiceSessionStart(UUID playerId, String channelId) {
        PlayerStatistics stats = getOrCreateStats(playerId);
        stats.recordSessionStart(channelId);
    }
    
    /**
     * Record a voice session end
     */
    public void recordVoiceSessionEnd(UUID playerId, long durationMs) {
        PlayerStatistics stats = getOrCreateStats(playerId);
        stats.recordSessionEnd(durationMs);
    }
    
    /**
     * Record a Discord message
     */
    public void recordDiscordMessage(UUID playerId) {
        PlayerStatistics stats = getOrCreateStats(playerId);
        stats.recordMessage();
    }
    
    /**
     * Record a reward received
     */
    public void recordReward(UUID playerId, String rewardType, String rewardValue) {
        PlayerStatistics stats = getOrCreateStats(playerId);
        stats.recordReward(rewardType, rewardValue);
    }
    
    /**
     * Get or create player statistics
     */
    private PlayerStatistics getOrCreateStats(UUID playerId) {
        return playerStats.computeIfAbsent(playerId, k -> new PlayerStatistics());
    }
    
    /**
     * Get player statistics
     */
    public PlayerStatistics getStats(UUID playerId) {
        return playerStats.getOrDefault(playerId, new PlayerStatistics());
    }
    
    /**
     * Get top players by total voice time
     */
    public List<Map.Entry<UUID, PlayerStatistics>> getTopByVoiceTime(int limit) {
        return playerStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().getTotalVoiceTime(), a.getValue().getTotalVoiceTime()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get top players by message count
     */
    public List<Map.Entry<UUID, PlayerStatistics>> getTopByMessages(int limit) {
        return playerStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getTotalMessages(), a.getValue().getTotalMessages()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get top players by session count
     */
    public List<Map.Entry<UUID, PlayerStatistics>> getTopBySessions(int limit) {
        return playerStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getTotalSessions(), a.getValue().getTotalSessions()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Player statistics data class
     */
    public static class PlayerStatistics {
        private long totalVoiceTime = 0;
        private int totalMessages = 0;
        private int totalSessions = 0;
        private int totalRewards = 0;
        private long longestSession = 0;
        private long shortestSession = Long.MAX_VALUE;
        private long firstJoinTime = 0;
        private long lastSeenTime = 0;
        private int currentStreak = 0;
        private int bestStreak = 0;
        private long currentSessionStart = 0;
        private String currentChannel = null;
        private final Map<String, Long> dailyTimes = new HashMap<>();
        private final Map<String, Integer> dailyMessages = new HashMap<>();
        private final List<String> recentRewards = new ArrayList<>();
        
        public PlayerStatistics() {
            this.firstJoinTime = System.currentTimeMillis();
        }
        
        public void recordSessionStart(String channelId) {
            currentSessionStart = System.currentTimeMillis();
            currentChannel = channelId;
            lastSeenTime = currentSessionStart;
            
            if (firstJoinTime == 0) {
                firstJoinTime = currentSessionStart;
            }
        }
        
        public void recordSessionEnd(long durationMs) {
            if (currentSessionStart > 0) {
                totalVoiceTime += durationMs;
                totalSessions++;
                
                // Update longest/shortest session
                if (durationMs > longestSession) {
                    longestSession = durationMs;
                }
                if (durationMs < shortestSession) {
                    shortestSession = durationMs;
                }
                
                // Update daily statistics
                String today = java.time.LocalDate.now().toString();
                dailyTimes.put(today, dailyTimes.getOrDefault(today, 0L) + durationMs);
                
                // Update streak
                updateStreak();
                
                currentSessionStart = 0;
                currentChannel = null;
            }
        }
        
        public void recordMessage() {
            totalMessages++;
            lastSeenTime = System.currentTimeMillis();
            
            String today = java.time.LocalDate.now().toString();
            dailyMessages.put(today, dailyMessages.getOrDefault(today, 0) + 1);
        }
        
        public void recordReward(String type, String value) {
            totalRewards++;
            String rewardRecord = type + ":" + value + ":" + System.currentTimeMillis();
            recentRewards.add(0, rewardRecord);
            
            // Keep only last 10 rewards
            if (recentRewards.size() > 10) {
                recentRewards.remove(recentRewards.size() - 1);
            }
        }
        
        private void updateStreak() {
            String today = java.time.LocalDate.now().toString();
            String yesterday = java.time.LocalDate.now().minusDays(1).toString();
            
            if (dailyTimes.containsKey(today)) {
                if (dailyTimes.containsKey(yesterday)) {
                    currentStreak++;
                } else {
                    currentStreak = 1;
                }
                
                if (currentStreak > bestStreak) {
                    bestStreak = currentStreak;
                }
            }
        }
        
        // Getters
        public long getTotalVoiceTime() { return totalVoiceTime; }
        public int getTotalMessages() { return totalMessages; }
        public int getTotalSessions() { return totalSessions; }
        public int getTotalRewards() { return totalRewards; }
        public long getLongestSession() { return longestSession == 0 ? 0 : longestSession; }
        public long getShortestSession() { return shortestSession == Long.MAX_VALUE ? 0 : shortestSession; }
        public long getFirstJoinTime() { return firstJoinTime; }
        public long getLastSeenTime() { return lastSeenTime; }
        public int getCurrentStreak() { return currentStreak; }
        public int getBestStreak() { return bestStreak; }
        public boolean isCurrentlyInVoice() { return currentSessionStart > 0; }
        public String getCurrentChannel() { return currentChannel; }
        
        public long getVoiceTimeToday() {
            String today = java.time.LocalDate.now().toString();
            return dailyTimes.getOrDefault(today, 0L);
        }
        
        public long getVoiceTimeYesterday() {
            String yesterday = java.time.LocalDate.now().minusDays(1).toString();
            return dailyTimes.getOrDefault(yesterday, 0L);
        }
        
        public int getMessagesToday() {
            String today = java.time.LocalDate.now().toString();
            return dailyMessages.getOrDefault(today, 0);
        }
        
        public long getAverageSessionLength() {
            return totalSessions > 0 ? totalVoiceTime / totalSessions : 0;
        }
        
        public int getSessionsToday() {
            // Count sessions that started today
            return isCurrentlyInVoice() && 
                   java.time.Instant.ofEpochMilli(currentSessionStart).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                   .equals(java.time.LocalDate.now()) ? 1 : 0;
        }
        
        public List<String> getRecentRewards() {
            return new ArrayList<>(recentRewards);
        }
        
        /**
         * Save statistics to configuration
         */
        public void saveToConfig(YamlConfiguration config, String path) {
            config.set(path + ".totalVoiceTime", totalVoiceTime);
            config.set(path + ".totalMessages", totalMessages);
            config.set(path + ".totalSessions", totalSessions);
            config.set(path + ".totalRewards", totalRewards);
            config.set(path + ".longestSession", longestSession);
            config.set(path + ".shortestSession", shortestSession == Long.MAX_VALUE ? 0 : shortestSession);
            config.set(path + ".firstJoinTime", firstJoinTime);
            config.set(path + ".lastSeenTime", lastSeenTime);
            config.set(path + ".currentStreak", currentStreak);
            config.set(path + ".bestStreak", bestStreak);
            config.set(path + ".currentSessionStart", currentSessionStart);
            config.set(path + ".currentChannel", currentChannel);
            
            // Save daily data
            for (Map.Entry<String, Long> entry : dailyTimes.entrySet()) {
                config.set(path + ".dailyTimes." + entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Integer> entry : dailyMessages.entrySet()) {
                config.set(path + ".dailyMessages." + entry.getKey(), entry.getValue());
            }
            
            config.set(path + ".recentRewards", recentRewards);
        }
        
        /**
         * Load statistics from configuration
         */
        public static PlayerStatistics fromConfig(YamlConfiguration config, String path) {
            PlayerStatistics stats = new PlayerStatistics();
            
            stats.totalVoiceTime = config.getLong(path + ".totalVoiceTime", 0);
            stats.totalMessages = config.getInt(path + ".totalMessages", 0);
            stats.totalSessions = config.getInt(path + ".totalSessions", 0);
            stats.totalRewards = config.getInt(path + ".totalRewards", 0);
            stats.longestSession = config.getLong(path + ".longestSession", 0);
            stats.shortestSession = config.getLong(path + ".shortestSession", Long.MAX_VALUE);
            stats.firstJoinTime = config.getLong(path + ".firstJoinTime", System.currentTimeMillis());
            stats.lastSeenTime = config.getLong(path + ".lastSeenTime", 0);
            stats.currentStreak = config.getInt(path + ".currentStreak", 0);
            stats.bestStreak = config.getInt(path + ".bestStreak", 0);
            stats.currentSessionStart = config.getLong(path + ".currentSessionStart", 0);
            stats.currentChannel = config.getString(path + ".currentChannel");
            
            // Load daily data
            if (config.isConfigurationSection(path + ".dailyTimes")) {
                for (String day : config.getConfigurationSection(path + ".dailyTimes").getKeys(false)) {
                    stats.dailyTimes.put(day, config.getLong(path + ".dailyTimes." + day));
                }
            }
            if (config.isConfigurationSection(path + ".dailyMessages")) {
                for (String day : config.getConfigurationSection(path + ".dailyMessages").getKeys(false)) {
                    stats.dailyMessages.put(day, config.getInt(path + ".dailyMessages." + day));
                }
            }
            
            List<String> rewards = config.getStringList(path + ".recentRewards");
            stats.recentRewards.addAll(rewards);
            
            return stats;
        }
    }
}