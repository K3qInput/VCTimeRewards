package me.kiro.vctime.utils;

import me.kiro.vctime.VCTimeRewards;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Utility class to test reward commands manually
 */
public class RewardTester {
    
    private final VCTimeRewards plugin;
    
    public RewardTester(VCTimeRewards plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Test if a reward command works by executing it directly
     */
    public boolean testRewardCommand(Player player, String command) {
        try {
            // Replace placeholder
            String finalCommand = command.replace("{player}", player.getName()).replace("{threshold}", "test");
            
            plugin.getLogger().info("Testing command: " + finalCommand);
            
            // Execute command
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
            
            if (success) {
                plugin.getLogger().info("✓ Command executed successfully: " + finalCommand);
                return true;
            } else {
                plugin.getLogger().warning("✗ Command failed to execute: " + finalCommand);
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("✗ Command execution error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test all reward commands configured in the config
     */
    public void testAllRewards(Player player) {
        java.util.Map<Long, String> rewards = plugin.getConfigUtil().getRewardCommands();
        
        plugin.getLogger().info("=== TESTING ALL REWARD COMMANDS ===");
        plugin.getLogger().info("Found " + rewards.size() + " reward commands to test");
        
        for (java.util.Map.Entry<Long, String> entry : rewards.entrySet()) {
            long threshold = entry.getKey();
            String command = entry.getValue();
            
            plugin.getLogger().info("Testing " + threshold + " minute reward...");
            testRewardCommand(player, command);
        }
        
        plugin.getLogger().info("=== REWARD TESTING COMPLETE ===");
    }
}