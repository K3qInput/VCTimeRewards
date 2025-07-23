package me.kiro.vctime.discord;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
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
    private final Map<UUID, String> currentVoiceChannels = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> channelMembers = new ConcurrentHashMap<>();
    private BukkitRunnable voiceCheckTask;
    private boolean isEnabled = false;
    private int failedAttempts = 0;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    
    public DiscordListener(VCTimeRewards plugin, TimeManager timeManager) {
        this.plugin = plugin;
        this.timeManager = timeManager;
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
     * Works for both online and offline players
     */
    private UUID getLinkedPlayerUuid(String discordId) {
        if (discordId == null || discordId.isEmpty()) {
            return null;
        }
        
        try {
            // Use DiscordSRV's account linking system to get UUID from Discord ID
            UUID playerUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordId);
            
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
     * Uses DiscordSRV's linking system
     */
    private Player getLinkedPlayer(String discordId) {
        if (discordId == null || discordId.isEmpty()) {
            return null;
        }
        
        try {
            // Use DiscordSRV's account linking system to get UUID from Discord ID
            UUID playerUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordId);
            
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
}
