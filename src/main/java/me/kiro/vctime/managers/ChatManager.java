package me.kiro.vctime.managers;

import me.kiro.vctime.VCTimeRewards;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Manages Discord chat message tracking and rewards
 */
public class ChatManager {
    
    private final VCTimeRewards plugin;
    private final Map<UUID, Integer> playerMessageCounts;
    private final Map<UUID, Set<Integer>> playerChatRewards;
    private YamlConfiguration chatData;
    private File chatDataFile;
    
    public ChatManager(VCTimeRewards plugin) {
        this.plugin = plugin;
        this.playerMessageCounts = new HashMap<>();
        this.playerChatRewards = new HashMap<>();
        setupDataFile();
        loadData();
    }
    
    /**
     * Setup the chat data file
     */
    private void setupDataFile() {
        chatDataFile = new File(plugin.getDataFolder(), "chat-data.yml");
        if (!chatDataFile.exists()) {
            try {
                chatDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create chat data file: " + e.getMessage());
            }
        }
        chatData = YamlConfiguration.loadConfiguration(chatDataFile);
    }
    
    /**
     * Load chat data from file
     */
    private void loadData() {
        if (chatData.isConfigurationSection("message-counts")) {
            for (String uuidStr : chatData.getConfigurationSection("message-counts").getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidStr);
                    int messageCount = chatData.getInt("message-counts." + uuidStr);
                    playerMessageCounts.put(playerId, messageCount);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in chat data: " + uuidStr);
                }
            }
        }
        
        if (chatData.isConfigurationSection("chat-rewards")) {
            for (String uuidStr : chatData.getConfigurationSection("chat-rewards").getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidStr);
                    Set<Integer> rewards = new HashSet<>();
                    for (String rewardStr : chatData.getStringList("chat-rewards." + uuidStr)) {
                        rewards.add(Integer.parseInt(rewardStr));
                    }
                    playerChatRewards.put(playerId, rewards);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid data in chat rewards: " + uuidStr);
                }
            }
        }
        
        plugin.getLogger().info("Loaded chat data for " + playerMessageCounts.size() + " players");
    }
    
    /**
     * Save chat data to file
     */
    public void saveData() {
        try {
            // Save message counts
            for (Map.Entry<UUID, Integer> entry : playerMessageCounts.entrySet()) {
                chatData.set("message-counts." + entry.getKey().toString(), entry.getValue());
            }
            
            // Save chat rewards
            for (Map.Entry<UUID, Set<Integer>> entry : playerChatRewards.entrySet()) {
                String[] rewardStrings = entry.getValue().stream()
                        .map(String::valueOf)
                        .toArray(String[]::new);
                chatData.set("chat-rewards." + entry.getKey().toString(), java.util.Arrays.asList(rewardStrings));
            }
            
            chatData.save(chatDataFile);
            plugin.getLogger().fine("Saved chat data for " + playerMessageCounts.size() + " players");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save chat data: " + e.getMessage());
        }
    }
    
    /**
     * Handle a Discord message from a linked player
     */
    public void handleMessage(UUID playerId) {
        if (!plugin.getConfigUtil().isChatRewardsEnabled()) {
            return;
        }
        
        // Increment message count
        int currentCount = playerMessageCounts.getOrDefault(playerId, 0) + 1;
        playerMessageCounts.put(playerId, currentCount);
        
        plugin.getLogger().fine("Player " + playerId + " now has " + currentCount + " messages");
        
        // Check for rewards
        checkChatRewards(playerId, currentCount);
    }
    
    /**
     * Check if player has earned any chat rewards
     */
    private void checkChatRewards(UUID playerId, int messageCount) {
        Map<Integer, String> rewards = plugin.getConfigUtil().getChatRewardCommands();
        
        for (Map.Entry<Integer, String> entry : rewards.entrySet()) {
            int threshold = entry.getKey();
            String command = entry.getValue();
            
            if (messageCount >= threshold && !hasReceivedChatReward(playerId, threshold)) {
                // Give reward
                giveChatReward(playerId, command, threshold);
                markChatRewardReceived(playerId, threshold);
                plugin.getLogger().info("CHAT REWARD: Player " + playerId + " reached " + threshold + " messages!");
            }
        }
    }
    
    /**
     * Give a chat reward to a player
     */
    private void giveChatReward(UUID playerId, String command, int threshold) {
        // Try to get online player first
        Player player = Bukkit.getPlayer(playerId);
        String playerName;
        
        if (player != null) {
            playerName = player.getName();
        } else {
            // Get offline player name
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
            playerName = offlinePlayer.getName();
            if (playerName == null) {
                plugin.getLogger().warning("Could not find player name for UUID: " + playerId);
                return;
            }
        }
        
        // Replace placeholders
        String finalCommand = command
                .replace("{player}", playerName)
                .replace("{messages}", String.valueOf(threshold));
        
        // Execute command with fallback formats
        executeRewardCommand(finalCommand, playerName + " (chat reward)");
    }
    
    /**
     * Execute reward command with multiple format fallbacks
     */
    private void executeRewardCommand(String command, String logContext) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String[] commandFormats = {
                command, // Original format
                command.replace("minecraft:", ""), // Without namespace
                "/" + command, // With slash prefix
                extractSimpleGiveCommand(command) // Simple give format
            };
            
            boolean executed = false;
            for (String cmdFormat : commandFormats) {
                try {
                    boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdFormat);
                    if (success) {
                        plugin.getLogger().info("✓ CHAT REWARD SUCCESS (" + logContext + "): " + cmdFormat);
                        executed = true;
                        break;
                    }
                } catch (Exception e) {
                    plugin.getLogger().fine("Chat reward command format failed: " + cmdFormat);
                }
            }
            
            if (!executed) {
                plugin.getLogger().severe("✗ ALL CHAT REWARD FORMATS FAILED for: " + command);
            }
        });
    }
    
    /**
     * Extract simple give command for fallback
     */
    private String extractSimpleGiveCommand(String command) {
        try {
            String[] parts = command.split(" ");
            if (parts.length >= 3) {
                String player = parts[1];
                String item = parts[2].replace("minecraft:", "");
                String quantity = parts.length > 3 ? parts[3] : "1";
                return "give " + player + " " + item + " " + quantity;
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Could not extract simple command from: " + command);
        }
        return "give unknown_player coal 1"; // Safe fallback
    }
    
    /**
     * Check if player has received a specific chat reward
     */
    private boolean hasReceivedChatReward(UUID playerId, int threshold) {
        Set<Integer> rewards = playerChatRewards.getOrDefault(playerId, new HashSet<>());
        return rewards.contains(threshold);
    }
    
    /**
     * Mark that a player has received a specific chat reward
     */
    private void markChatRewardReceived(UUID playerId, int threshold) {
        playerChatRewards.computeIfAbsent(playerId, k -> new HashSet<>()).add(threshold);
    }
    
    /**
     * Get message count for a player
     */
    public int getMessageCount(UUID playerId) {
        return playerMessageCounts.getOrDefault(playerId, 0);
    }
    
    /**
     * Reset message count for a player
     */
    public void resetMessageCount(UUID playerId) {
        playerMessageCounts.remove(playerId);
        playerChatRewards.remove(playerId);
        plugin.getLogger().info("Reset chat data for player: " + playerId);
    }
    
    /**
     * Handle server boost reward
     */
    public void handleServerBoost(UUID playerId) {
        if (!plugin.getConfigUtil().isBoostRewardsEnabled()) {
            return;
        }
        
        String command = plugin.getConfigUtil().getBoostRewardCommand();
        if (command.isEmpty()) {
            return;
        }
        
        // Try to get player name
        Player player = Bukkit.getPlayer(playerId);
        String playerName;
        
        if (player != null) {
            playerName = player.getName();
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
            playerName = offlinePlayer.getName();
            if (playerName == null) {
                plugin.getLogger().warning("Could not find player name for boost reward: " + playerId);
                return;
            }
        }
        
        // Replace placeholder
        String finalCommand = command.replace("{player}", playerName);
        
        // Execute command
        executeRewardCommand(finalCommand, playerName + " (boost reward)");
        
        // Announce if enabled
        if (plugin.getConfigUtil().shouldAnnounceBoostRewards()) {
            Bukkit.broadcastMessage("§6🚀 " + playerName + " boosted the Discord server and received a reward!");
        }
        
        plugin.getLogger().info("BOOST REWARD: " + playerName + " boosted the server!");
    }
}