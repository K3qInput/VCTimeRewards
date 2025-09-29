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
        // Test multiple command formats to find what works on this server
        String[] formats = {
            command.replace("{player}", player.getName()).replace("{threshold}", "test"),
            command.replace("minecraft:", "").replace("{player}", player.getName()).replace("{threshold}", "test"),
            "/" + command.replace("{player}", player.getName()).replace("{threshold}", "test"),
            "give " + player.getName() + " coal 1", // Simple test command
            "minecraft:give " + player.getName() + " minecraft:coal 1" // Full format test
        };
        
        for (String testCommand : formats) {
            try {
                plugin.getLogger().info("Testing command: " + testCommand);
                
                boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), testCommand);
                
                if (success) {
                    plugin.getLogger().info("✓ SUCCESS: " + testCommand);
                    return true;
                } else {
                    plugin.getLogger().warning("✗ FAILED: " + testCommand);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("✗ ERROR with '" + testCommand + "': " + e.getMessage());
            }
        }
        
        plugin.getLogger().severe("✗ ALL FORMATS FAILED for original command: " + command);
        return false;
    }
    
    /**
     * Test all reward commands configured in the config
     */
    public void testAllRewards(Player player) {
        java.util.Map<Long, String> rewards = plugin.getConfigUtil().getVoiceRewardCommands();
        
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