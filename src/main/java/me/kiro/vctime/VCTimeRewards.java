package me.kiro.vctime;

import me.kiro.vctime.commands.VCTimeCommand;
import me.kiro.vctime.discord.DiscordListener;
import me.kiro.vctime.managers.TimeManager;
import me.kiro.vctime.utils.ConfigUtil;
import org.bukkit.plugin.java.JavaPlugin;
import github.scarsz.discordsrv.DiscordSRV;

/**
 * Main plugin class for VCTimeRewards
 * Handles plugin initialization and Discord integration
 */
public class VCTimeRewards extends JavaPlugin {
    
    private TimeManager timeManager;
    private ConfigUtil configUtil;
    private DiscordListener discordListener;
    
    @Override
    public void onEnable() {
        getLogger().info("Enabling VCTimeRewards plugin...");
        
        // Initialize configuration
        saveDefaultConfig();
        configUtil = new ConfigUtil(this);
        
        // Initialize time manager
        timeManager = new TimeManager(this);
        
        // Wait for DiscordSRV to be ready
        if (getServer().getPluginManager().getPlugin("DiscordSRV") == null) {
            getLogger().severe("DiscordSRV not found! This plugin requires DiscordSRV to function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register Discord listener
        discordListener = new DiscordListener(this, timeManager);
        discordListener.initializeListener();
        getServer().getPluginManager().registerEvents(discordListener, this);
        
        // Register commands
        getCommand("vctime").setExecutor(new VCTimeCommand(this));
        
        getLogger().info("VCTimeRewards plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Disabling VCTimeRewards plugin...");
        
        // Clean up Discord listener
        if (discordListener != null) {
            try {
                // Unregister from JDA through DiscordSRV
                if (DiscordSRV.getPlugin() != null && DiscordSRV.getPlugin().getJda() != null) {
                    DiscordSRV.getPlugin().getJda().removeEventListener(discordListener);
                    getLogger().info("Successfully unregistered Discord event listeners.");
                }
            } catch (Exception e) {
                getLogger().warning("Error unregistering Discord listeners: " + e.getMessage());
            }
        }
        
        // Stop all active tracking sessions and save data
        if (timeManager != null) {
            timeManager.stopAllTracking();
            timeManager.saveAll();
        }
        
        getLogger().info("VCTimeRewards plugin disabled!");
    }
    
    public TimeManager getTimeManager() {
        return timeManager;
    }
    
    public ConfigUtil getConfigUtil() {
        return configUtil;
    }
}
