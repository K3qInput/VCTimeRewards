package me.kiro.vctime;

import me.kiro.vctime.commands.VCTimeCommand;
import me.kiro.vctime.commands.VCTimeAdminCommand;
import me.kiro.vctime.discord.DiscordListener;
import me.kiro.vctime.managers.ChatManager;
import me.kiro.vctime.managers.TimeManager;
import me.kiro.vctime.placeholders.VCTimeExpansion;
import me.kiro.vctime.utils.ConfigUtil;
import org.bukkit.plugin.java.JavaPlugin;
import github.scarsz.discordsrv.DiscordSRV;

/**
 * Main plugin class for VCTimeRewards
 * Handles plugin initialization and Discord integration
 */
public class VCTimeRewards extends JavaPlugin {
    
    private TimeManager timeManager;
    private ChatManager chatManager;
    private ConfigUtil configUtil;
    private DiscordListener discordListener;
    private VCTimeExpansion placeholderExpansion;
    
    @Override
    public void onEnable() {
        getLogger().info("Enabling VCTimeRewards plugin...");
        
        // Initialize configuration
        saveDefaultConfig();
        configUtil = new ConfigUtil(this);
        
        // Initialize managers
        timeManager = new TimeManager(this);
        chatManager = new ChatManager(this);
        
        // Wait for DiscordSRV to be ready
        if (getServer().getPluginManager().getPlugin("DiscordSRV") == null) {
            getLogger().severe("DiscordSRV not found! This plugin requires DiscordSRV to function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register Discord listener
        discordListener = new DiscordListener(this, timeManager, chatManager);
        discordListener.initializeListener();
        getServer().getPluginManager().registerEvents(discordListener, this);
        
        // Register commands
        getCommand("vctime").setExecutor(new VCTimeCommand(this));
        getCommand("vctimeadmin").setExecutor(new VCTimeAdminCommand(this));
        
        // Register PlaceholderAPI expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new VCTimeExpansion(this);
            placeholderExpansion.register();
            getLogger().info("PlaceholderAPI integration enabled!");
        } else {
            getLogger().info("PlaceholderAPI not found - placeholders will not be available");
        }
        
        getLogger().info("VCTimeRewards plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Disabling VCTimeRewards plugin...");
        
        // Clean up Discord listener
        if (discordListener != null) {
            try {
                discordListener.stopChecking();
                getLogger().info("Discord voice channel monitoring stopped.");
            } catch (Exception e) {
                getLogger().warning("Error during Discord listener cleanup: " + e.getMessage());
            }
        }
        
        // Clean up managers
        if (timeManager != null) {
            try {
                timeManager.shutdown();
                getLogger().info("TimeManager shutdown completed.");
            } catch (Exception e) {
                getLogger().warning("Error during TimeManager shutdown: " + e.getMessage());
            }
        }
        
        if (chatManager != null) {
            try {
                chatManager.saveData();
                getLogger().info("ChatManager data saved.");
            } catch (Exception e) {
                getLogger().warning("Error during ChatManager shutdown: " + e.getMessage());
            }
        }
        
        getLogger().info("VCTimeRewards plugin disabled!");
    }
    
    public TimeManager getTimeManager() {
        return timeManager;
    }
    
    public ChatManager getChatManager() {
        return chatManager;
    }
    
    public ConfigUtil getConfigUtil() {
        return configUtil;
    }
    
    public DiscordListener getDiscordListener() {
        return discordListener;
    }
    
    public void initializeConfigUtil() {
        this.configUtil = new ConfigUtil(this);
    }
}
