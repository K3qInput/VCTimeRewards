package me.kiro.vctime.discord;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import me.kiro.vctime.VCTimeRewards;
import me.kiro.vctime.managers.TimeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

/**
 * Enhanced Discord voice channel tracker with comprehensive monitoring
 */
public class DiscordListener implements Listener {
    
    private final VCTimeRewards plugin;
    private final TimeManager timeManager;
    private final me.kiro.vctime.managers.ChatManager chatManager;
    private final Map<UUID, String> currentVoiceChannels = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> channelMembers = new ConcurrentHashMap<>();
    private BukkitRunnable voiceCheckTask;
    private boolean isEnabled = false;
    private int failedAttempts = 0;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    
    public DiscordListener(VCTimeRewards plugin, TimeManager timeManager, me.kiro.vctime.managers.ChatManager chatManager) {
        this.plugin = plugin;
        this.timeManager = timeManager;
        this.chatManager = chatManager;
    }
    
    /**
     * Initialize the Discord listener with enhanced monitoring
     */
    public void initializeListener() {
        plugin.getLogger().info("Initializing enhanced Discord voice channel tracking...");
        
        // Register DiscordSRV API events
        DiscordSRV.api.subscribe(this);
        
        // Wait for DiscordSRV to be ready and start checking
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            startVoiceChannelChecking();
        }, 100L); // 5 seconds delay
    }
    
    /**
     * Handle DiscordSRV ready event
     */
    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event) {
        plugin.getLogger().info("DiscordSRV is ready - voice channel tracking can begin");
        isEnabled = true;
        failedAttempts = 0;
    }
    
    /**
     * Handle Discord message events for chat tracking
     */
    @Subscribe
    public void onDiscordMessage(DiscordGuildMessageReceivedEvent event) {
        try {
            // Skip bot messages
            if (event.getAuthor().isBot()) {
                return;
            }
            
            // Skip if chat rewards are disabled
            if (!plugin.getConfigUtil().isChatRewardsEnabled()) {
                return;
            }
            
            // Get Discord user ID
            String discordUserId = event.getAuthor().getId();
            String messageContent = event.getMessage().getContentDisplay();
            
            // Skip empty messages
            if (messageContent == null || messageContent.trim().isEmpty()) {
                return;
            }
            
            // Handle the message for chat rewards with enhanced logging
            handleDiscordMessage(discordUserId, messageContent);
            plugin.getLogger().fine("✓ Discord message processed from user: " + discordUserId + " (content: " + messageContent.substring(0, Math.min(50, messageContent.length())) + ")");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing Discord message event: " + e.getMessage());
            plugin.getErrorHandler().handleException("DiscordMessage", e, () -> null);
        }
    }
    
    /**
     * Start comprehensive voice channel monitoring
     */
    private void startVoiceChannelChecking() {
        if (voiceCheckTask != null) {
            voiceCheckTask.cancel();
        }
        
        voiceCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (isEnabled) {
                    checkVoiceChannels();
                } else {
                    // Try to initialize DiscordSRV connection
                    tryInitializeDiscordSRV();
                }
            }
        };
        
        // Check every 10 seconds for more responsive tracking
        voiceCheckTask.runTaskTimerAsynchronously(plugin, 0L, 200L);
        plugin.getLogger().info("Started enhanced voice channel monitoring (10-second intervals)");
    }
    
    /**
     * Try to initialize DiscordSRV connection
     */
    private void tryInitializeDiscordSRV() {
        try {
            if (DiscordSRV.getPlugin() != null && DiscordSRV.getPlugin().getJda() != null) {
                isEnabled = true;
                failedAttempts = 0;
                plugin.getLogger().info("DiscordSRV connection established - voice tracking enabled");
            } else {
                failedAttempts++;
                if (failedAttempts % 12 == 0) { // Log every 2 minutes
                    plugin.getLogger().warning("Waiting for DiscordSRV to be ready... (attempt " + failedAttempts + ")");
                }
            }
        } catch (Exception e) {
            failedAttempts++;
            if (failedAttempts % 12 == 0) {
                plugin.getLogger().warning("Error connecting to DiscordSRV: " + e.getMessage());
            }
        }
    }
    
    /**
     * Enhanced voice channel monitoring with detailed tracking
     */
    private void checkVoiceChannels() {
        try {
            Object jda = DiscordSRV.getPlugin().getJda();
            if (jda == null) {
                if (failedAttempts < MAX_FAILED_ATTEMPTS) {
                    failedAttempts++;
                }
                return;
            }
            
            // Reset failed attempts on successful connection
            failedAttempts = 0;
            
            // Get all guilds from JDA
            Object guildsCollection = jda.getClass().getMethod("getGuilds").invoke(jda);
            java.util.List<?> guilds = (java.util.List<?>) guildsCollection;
            
            Set<UUID> activeVoicePlayers = new HashSet<>();
            Map<String, Set<UUID>> newChannelMembers = new HashMap<>();
            
            for (Object guild : guilds) {
                // Get guild information for logging
                String guildName = (String) guild.getClass().getMethod("getName").invoke(guild);
                
                // Get all voice channels in this guild
                Object voiceChannels = guild.getClass().getMethod("getVoiceChannels").invoke(guild);
                java.util.List<?> channels = (java.util.List<?>) voiceChannels;
                
                for (Object channel : channels) {
                    String channelId = (String) channel.getClass().getMethod("getId").invoke(channel);
                    String channelName = (String) channel.getClass().getMethod("getName").invoke(channel);
                    
                    // Check if this channel should be tracked
                    if (!shouldTrackChannel(channelId)) {
                        continue;
                    }
                    
                    // Get members in this channel
                    Object membersCollection = channel.getClass().getMethod("getMembers").invoke(channel);
                    java.util.List<?> members = (java.util.List<?>) membersCollection;
                    
                    Set<UUID> channelPlayerUuids = new HashSet<>();
                    
                    // Process each member in the channel
                    for (Object member : members) {
                        String discordId = (String) member.getClass().getMethod("getId").invoke(member);
                        String memberName = (String) member.getClass().getMethod("getEffectiveName").invoke(member);
                        
                        UUID playerUuid = getLinkedPlayerUuid(discordId);
                        if (playerUuid != null) {
                            channelPlayerUuids.add(playerUuid);
                        }
                    }
                    
                    // Check minimum members requirement (only count linked players)
                    int minMembers = plugin.getConfigUtil().getMinimumMembers();
                    if (channelPlayerUuids.size() < minMembers) {
                        // Stop tracking players in this channel if it doesn't meet requirements
                        for (UUID playerUuid : channelPlayerUuids) {
                            if (currentVoiceChannels.containsKey(playerUuid) && 
                                channelId.equals(currentVoiceChannels.get(playerUuid))) {
                                plugin.getLogger().info("Channel " + channelName + " no longer meets minimum member requirement (" + 
                                                      channelPlayerUuids.size() + "/" + minMembers + ") - stopping tracking");
                                timeManager.stopTracking(playerUuid);
                                currentVoiceChannels.remove(playerUuid);
                            }
                        }
                        continue;
                    }
                    
                    // Store channel members for comparison
                    newChannelMembers.put(channelId, channelPlayerUuids);
                    activeVoicePlayers.addAll(channelPlayerUuids);
                    
                    // Process each linked player in the channel
                    for (UUID playerUuid : channelPlayerUuids) {
                        String previousChannel = currentVoiceChannels.get(playerUuid);
                        
                        if (!channelId.equals(previousChannel)) {
                            // Player joined new channel or switched channels
                            if (previousChannel != null) {
                                // Stop tracking previous channel
                                plugin.getLogger().info("Player switched from channel " + previousChannel + " to " + channelName);
                                timeManager.stopTracking(playerUuid);
                            } else {
                                plugin.getLogger().info("Player joined voice channel: " + channelName + " (" + channelPlayerUuids.size() + "/" + members.size() + " linked players)");
                            }
                            
                            // Start tracking new channel
                            timeManager.startTracking(playerUuid, channelId);
                            currentVoiceChannels.put(playerUuid, channelId);
                        }
                    }
                }
            }
            
            // Check for players who left voice channels
            Set<UUID> playersToRemove = new HashSet<>();
            for (Map.Entry<UUID, String> entry : currentVoiceChannels.entrySet()) {
                UUID playerUuid = entry.getKey();
                if (!activeVoicePlayers.contains(playerUuid)) {
                    // Player left voice channel
                    plugin.getLogger().info("Player left voice channel - stopping tracking");
                    timeManager.stopTracking(playerUuid);
                    playersToRemove.add(playerUuid);
                }
            }
            
            // Remove players who left
            for (UUID playerUuid : playersToRemove) {
                currentVoiceChannels.remove(playerUuid);
            }
            
            // Update channel members tracking
            channelMembers.clear();
            channelMembers.putAll(newChannelMembers);
            
            // Log status every 5 minutes (30 checks at 10-second intervals)
            if (System.currentTimeMillis() % 300000 < 10000) { // Every 5 minutes
                int totalTracking = currentVoiceChannels.size();
                int totalChannels = newChannelMembers.size();
                if (totalTracking > 0 || totalChannels > 0) {
                    plugin.getLogger().info("Voice tracking status: " + totalTracking + " players being tracked across " + totalChannels + " eligible channels");
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error in voice channel monitoring: " + e.getMessage());
            if (plugin.getServer().getLogger().isLoggable(java.util.logging.Level.FINE)) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Stop voice channel checking
     */
    public void stopChecking() {
        if (voiceCheckTask != null) {
            voiceCheckTask.cancel();
            voiceCheckTask = null;
        }
        
        // Stop tracking all players
        for (UUID playerUuid : currentVoiceChannels.keySet()) {
            timeManager.stopTracking(playerUuid);
        }
        currentVoiceChannels.clear();
        
        plugin.getLogger().info("Stopped voice channel checking.");
    }

    
    /**
     * Get the linked Minecraft player UUID from a Discord user ID
     * Uses safe approach to handle different DiscordSRV API versions
     */
    private UUID getLinkedPlayerUuid(String discordId) {
        if (discordId == null || discordId.isEmpty()) {
            return null;
        }
        
        try {
            // Use reflection-based approach to handle interface/class compatibility
            UUID playerUuid = null;
            
            // Method 1: Try getting AccountLinkManager and call getUuid
            try {
                Object accountLinkManager = DiscordSRV.getPlugin().getAccountLinkManager();
                if (accountLinkManager != null) {
                    // Use reflection to safely call getUuid method
                    java.lang.reflect.Method getUuidMethod = accountLinkManager.getClass().getMethod("getUuid", String.class);
                    Object result = getUuidMethod.invoke(accountLinkManager, discordId);
                    if (result instanceof UUID) {
                        playerUuid = (UUID) result;
                    }
                }
            } catch (Exception e1) {
                // AccountLinkManager approach failed, try alternatives
                plugin.getLogger().fine("AccountLinkManager approach failed: " + e1.getMessage());
                
                // Method 2: Try using DiscordSRV static methods (fallback for older versions)
                try {
                    // Try commonly available static method
                    java.lang.reflect.Method[] methods = DiscordSRV.class.getDeclaredMethods();
                    for (java.lang.reflect.Method method : methods) {
                        if (method.getName().contains("getUuid") && method.getParameterCount() == 1) {
                            method.setAccessible(true);
                            Object result = method.invoke(null, discordId);
                            if (result instanceof UUID) {
                                playerUuid = (UUID) result;
                                break;
                            }
                        }
                    }
                } catch (Exception e2) {
                    plugin.getLogger().fine("Static method fallback failed: " + e2.getMessage());
                }
            }
            
            if (playerUuid == null) {
                plugin.getLogger().fine("No linked Minecraft account found for Discord ID: " + discordId);
                return null;
            }
            
            // Check if both Discord and Minecraft user must be online
            if (plugin.getConfigUtil().requireBothOnline()) {
                Player player = plugin.getServer().getPlayer(playerUuid);
                if (player == null || !player.isOnline()) {
                    plugin.getLogger().fine("Player with UUID " + playerUuid + " is not online, skipping tracking");
                    return null;
                }
            }
            
            return playerUuid;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting linked player UUID for Discord ID " + discordId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the linked Minecraft player from a Discord user ID
     * Uses DiscordSRV's linking system with compatibility handling
     */
    private Player getLinkedPlayer(String discordId) {
        if (discordId == null || discordId.isEmpty()) {
            return null;
        }
        
        try {
            // Get UUID using the compatible method
            UUID playerUuid = getLinkedPlayerUuid(discordId);
            
            if (playerUuid == null) {
                plugin.getLogger().fine("No linked Minecraft account found for Discord ID: " + discordId);
                return null;
            }
            
            // Get the online player from the UUID
            Player player = plugin.getServer().getPlayer(playerUuid);
            
            if (player == null || !player.isOnline()) {
                // Check if both Discord and Minecraft user must be online
                if (plugin.getConfigUtil().requireBothOnline()) {
                    plugin.getLogger().fine("Player with UUID " + playerUuid + " is not online, skipping tracking");
                    return null;
                } else {
                    // Allow tracking even if player is offline
                    plugin.getLogger().fine("Player with UUID " + playerUuid + " is offline but tracking allowed");
                    // For offline tracking, we still need some way to identify the player
                    // We'll return a dummy player object or handle this in the TimeManager
                    return null; // For now, let TimeManager handle offline players
                }
            }
            
            return player;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting linked player for Discord ID " + discordId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if a voice channel should be tracked based on configuration
     */
    private boolean shouldTrackChannel(String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            return false;
        }
        
        // Use the ConfigUtil method that handles both whitelist and blacklist modes
        return plugin.getConfigUtil().shouldTrackChannel(channelId);
    }
    
    /**
     * Handle Discord message events for chat rewards with enhanced tracking
     */
    public void handleDiscordMessage(String discordUserId, String messageContent) {
        try {
            // Get linked Minecraft player UUID
            UUID minecraftUUID = getLinkedPlayerUuid(discordUserId);
            
            if (minecraftUUID != null) {
                // Handle the message for chat rewards
                chatManager.handleMessage(minecraftUUID);
                
                // Record in statistics manager
                if (plugin.getStatisticsManager() != null) {
                    plugin.getStatisticsManager().recordDiscordMessage(minecraftUUID);
                }
                
                // Check achievements
                if (plugin.getAchievementManager() != null && plugin.getStatisticsManager() != null) {
                    plugin.getAchievementManager().checkAchievements(minecraftUUID, 
                        plugin.getStatisticsManager().getStats(minecraftUUID));
                }
                
                plugin.getLogger().fine("✓ CHAT TRACKING: Processed Discord message from linked player: " + minecraftUUID);
                
                // Log for debugging if debug mode is enabled
                if (plugin.getConfig().getBoolean("debug-mode", false)) {
                    plugin.getLogger().info("DEBUG: Discord message tracked - User: " + discordUserId + " → UUID: " + minecraftUUID + " → Message: " + messageContent.substring(0, Math.min(100, messageContent.length())));
                }
            } else {
                // Log unlinked messages in debug mode
                if (plugin.getConfig().getBoolean("debug-mode", false)) {
                    plugin.getLogger().fine("DEBUG: Discord message from unlinked user: " + discordUserId);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling Discord message: " + e.getMessage());
            plugin.getErrorHandler().handleException("DiscordMessageHandler", e, () -> null);
        }
    }
    
    /**
     * Handle Discord server boost events
     */
    public void handleDiscordBoost(String discordUserId) {
        try {
            // Get linked Minecraft player UUID
            UUID minecraftUUID = getLinkedPlayerUuid(discordUserId);
            
            if (minecraftUUID != null) {
                // Handle the boost for rewards
                chatManager.handleServerBoost(minecraftUUID);
                plugin.getLogger().info("Processed Discord server boost from linked player: " + minecraftUUID);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling Discord boost: " + e.getMessage());
        }
    }
}
