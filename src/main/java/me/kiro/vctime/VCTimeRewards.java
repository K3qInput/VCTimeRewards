package me.kiro.vctime;

import me.kiro.vctime.commands.LeaderboardCommand;
import me.kiro.vctime.commands.VCTimeCommand;
import me.kiro.vctime.commands.VCTimeAdminCommand;
import me.kiro.vctime.discord.DiscordListener;
import me.kiro.vctime.managers.ChatManager;
import me.kiro.vctime.managers.OptimizedDataManager;
import me.kiro.vctime.managers.TimeManager;
import me.kiro.vctime.placeholders.VCTimeExpansion;
import me.kiro.vctime.utils.ConfigUtil;
import me.kiro.vctime.utils.EnhancedLogger;
import me.kiro.vctime.utils.ErrorHandler;
import me.kiro.vctime.utils.PerformanceMonitor;
import org.bukkit.plugin.java.JavaPlugin;
import github.scarsz.discordsrv.DiscordSRV;

/**
 * Main plugin class for VCTimeRewards
 * Handles plugin initialization and Discord integration
 */
public class VCTimeRewards extends JavaPlugin {
    
    private TimeManager timeManager;
    private ChatManager chatManager;
    private OptimizedDataManager dataManager;
    private PerformanceMonitor performanceMonitor;
    private EnhancedLogger enhancedLogger;
    private ErrorHandler errorHandler;
    private ConfigUtil configUtil;
    private DiscordListener discordListener;
    private VCTimeExpansion placeholderExpansion;
    
    @Override
    public void onEnable() {
        getLogger().info("Enabling VCTimeRewards plugin...");
        
        // Initialize configuration
        saveDefaultConfig();
        configUtil = new ConfigUtil(this);
        
        // Initialize enhanced logging and error handling first
        enhancedLogger = new EnhancedLogger(this);
        errorHandler = new ErrorHandler(this, enhancedLogger);
        
        // Initialize managers with enhanced error handling
        timeManager = errorHandler.handleException("TimeManager_Init", 
                () -> new TimeManager(this), null).orElse(null);
        chatManager = errorHandler.handleException("ChatManager_Init", 
                () -> new ChatManager(this), null).orElse(null);
        dataManager = errorHandler.handleException("DataManager_Init", 
                () -> new OptimizedDataManager(this), null).orElse(null);
        performanceMonitor = errorHandler.handleException("PerformanceMonitor_Init", 
                () -> new PerformanceMonitor(this), null).orElse(null);
        
        // Validate critical managers initialization
        if (timeManager == null) {
            enhancedLogger.error("STARTUP", "Failed to initialize TimeManager - disabling plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        if (chatManager == null) {
            enhancedLogger.error("STARTUP", "Failed to initialize ChatManager - disabling plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Warm up cache for better performance (only if dataManager initialized)
        if (dataManager != null) {
            dataManager.warmUpCache();
        } else {
            enhancedLogger.warn("STARTUP", "DataManager failed to initialize - running without optimization features");
        }
        
        // Wait for DiscordSRV to be ready
        if (getServer().getPluginManager().getPlugin("DiscordSRV") == null) {
            enhancedLogger.error("STARTUP", "DiscordSRV not found! This plugin requires DiscordSRV to function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register Discord listener with error handling
        discordListener = errorHandler.handleException("DiscordListener_Init", 
                () -> new DiscordListener(this, timeManager, chatManager), null).orElse(null);
        
        if (discordListener != null) {
            errorHandler.handleOperation("DiscordListener_Initialize", 
                    () -> discordListener.initializeListener());
            errorHandler.handleOperation("DiscordListener_Register", 
                    () -> getServer().getPluginManager().registerEvents(discordListener, this));
        }
        
        // Register commands
        getCommand("vctime").setExecutor(new VCTimeCommand(this));
        getCommand("vctimeadmin").setExecutor(new VCTimeAdminCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));
        
        // Register PlaceholderAPI expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = errorHandler.handleException("PlaceholderAPI_Init", 
                    () -> new VCTimeExpansion(this), null).orElse(null);
            
            if (placeholderExpansion != null) {
                errorHandler.handleOperation("PlaceholderAPI_Register", 
                        () -> placeholderExpansion.register());
                enhancedLogger.info("STARTUP", "PlaceholderAPI integration enabled!");
            }
        } else {
            enhancedLogger.info("STARTUP", "PlaceholderAPI not found - placeholders will not be available");
        }
        
        enhancedLogger.info("STARTUP", "VCTimeRewards plugin enabled successfully!");
        enhancedLogger.info("STARTUP", "All systems operational - World-class Discord voice rewards plugin ready!");
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
        
        if (dataManager != null) {
            try {
                dataManager.shutdown();
                getLogger().info("OptimizedDataManager shutdown completed.");
            } catch (Exception e) {
                getLogger().warning("Error during OptimizedDataManager shutdown: " + e.getMessage());
            }
        }
        
        if (performanceMonitor != null) {
            try {
                performanceMonitor.stopMonitoring();
                enhancedLogger.info("SHUTDOWN", "PerformanceMonitor stopped.");
            } catch (Exception e) {
                enhancedLogger.error("SHUTDOWN", "Error during PerformanceMonitor shutdown", e);
            }
        }
        
        if (enhancedLogger != null) {
            try {
                enhancedLogger.info("SHUTDOWN", "VCTimeRewards plugin shutdown complete!");
                enhancedLogger.shutdown();
            } catch (Exception e) {
                getLogger().warning("Error during EnhancedLogger shutdown: " + e.getMessage());
            }
        } else {
            getLogger().info("VCTimeRewards plugin disabled!");
        }
    }
    
    public TimeManager getTimeManager() {
        return timeManager;
    }
    
    public ChatManager getChatManager() {
        return chatManager;
    }
    
    public OptimizedDataManager getDataManager() {
        return dataManager;
    }
    
    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    public EnhancedLogger getEnhancedLogger() {
        return enhancedLogger;
    }
    
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
    
    public ConfigUtil getConfigUtil() {
        return configUtil;
    }
    
    public DiscordListener getDiscordListener() {
        return discordListener;
    }
    
    /**
     * Helper method to initialize ConfigUtil safely
     */
    public void initializeConfigUtil() {
        this.configUtil = new ConfigUtil(this);
    }
}
