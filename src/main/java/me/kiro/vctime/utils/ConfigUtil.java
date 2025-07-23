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
     * Get reward commands mapped to hour thresholds
     */
    public Map<Integer, String> getRewardCommands() {
        Map<Integer, String> rewards = new HashMap<>();
        
        if (config.isConfigurationSection("rewards")) {
            for (String key : config.getConfigurationSection("rewards").getKeys(false)) {
                try {
                    int hours = Integer.parseInt(key);
                    String command = config.getString("rewards." + key);
                    rewards.put(hours, command);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid reward threshold: " + key);
                }
            }
        }
        
        return rewards;
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
    public boolean isSendNotifications() {
        return config.getBoolean("send-notifications", true);
    }
    
    /**
     * Get the notification message for time milestones
     */
    public String getNotificationMessage() {
        return config.getString("notification-message", 
                "&aYou have spent {time} in Discord voice channels!");
    }
}
