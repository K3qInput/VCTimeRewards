package me.kiro.vctime.utils;

import me.kiro.vctime.VCTimeRewards;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for handling plugin configuration
 */
public class ConfigUtil {
    
    private final VCTimeRewards plugin;
    private final FileConfiguration config;
    
    public ConfigUtil(VCTimeRewards plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }
    
    /**
     * Check if a Discord channel should be tracked based on whitelist/blacklist mode
     */
    public boolean shouldTrackChannel(String channelId) {
        String mode = config.getString("tracking-mode", "blacklist");
        
        if ("whitelist".equals(mode)) {
            // Whitelist mode: only track channels in the tracked-channels list
            List<String> trackedChannels = config.getStringList("tracked-channels");
            return trackedChannels.contains(channelId);
        } else {
            // Blacklist mode: track all channels except those in blacklisted-channels
            List<String> blacklistedChannels = config.getStringList("blacklisted-channels");
            return !blacklistedChannels.contains(channelId);
        }
    }
    
    /**
     * Check if a Discord channel is blacklisted (legacy method for backwards compatibility)
     */
    public boolean isChannelBlacklisted(String channelId) {
        return !shouldTrackChannel(channelId);
    }
    
    /**
     * Get the minimum number of members required in a voice channel for tracking
     */
    public int getMinimumMembers() {
        return config.getInt("minimum-members", 2);
    }
    
    /**
     * Get reward commands mapped to minute thresholds (supports both minutes and hours)
     */
    public Map<Long, String> getRewardCommands() {
        Map<Long, String> rewards = new HashMap<>();
        
        if (config.isConfigurationSection("rewards")) {
            for (String key : config.getConfigurationSection("rewards").getKeys(false)) {
                try {
                    long minutes = parseTimeToMinutes(key);
                    String command = config.getString("rewards." + key);
                    rewards.put(minutes, command);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid reward threshold: " + key + " (use format like '30m', '1h', or just number for hours)");
                }
            }
        }
        
        return rewards;
    }
    
    /**
     * Parse time string to minutes
     * Supports formats: "30m" (30 minutes), "2h" (2 hours), "120" (120 hours for backwards compatibility)
     */
    private long parseTimeToMinutes(String timeStr) throws NumberFormatException {
        timeStr = timeStr.toLowerCase().trim();
        
        if (timeStr.endsWith("m")) {
            // Minutes format: "30m"
            String numberStr = timeStr.substring(0, timeStr.length() - 1);
            return Long.parseLong(numberStr);
        } else if (timeStr.endsWith("h")) {
            // Hours format: "2h"  
            String numberStr = timeStr.substring(0, timeStr.length() - 1);
            return Long.parseLong(numberStr) * 60; // Convert hours to minutes
        } else {
            // Plain number - treat as hours for backwards compatibility
            return Long.parseLong(timeStr) * 60; // Convert hours to minutes
        }
    }
    
    /**
     * Get whether to track time only when both users are online in Minecraft
     */
    public boolean isRequireBothOnline() {
        return config.getBoolean("require-both-online", false);
    }
    
    /**
     * Get whether to send notifications to players
     */
    public boolean shouldSendNotifications() {
        return config.getBoolean("send-notifications", true);
    }
    
    /**
     * Get the notification message for time milestones
     */
    public String getNotificationMessage() {
        return config.getString("notification-message", 
                "&aYou have spent {time} in Discord voice channels!");
    }
    
    /**
     * Get whether both Discord and Minecraft user must be online for time tracking
     */
    public boolean requireBothOnline() {
        return config.getBoolean("require-both-online", false);
    }
}
