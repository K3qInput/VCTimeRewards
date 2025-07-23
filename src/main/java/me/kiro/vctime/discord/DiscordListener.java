package me.kiro.vctime.discord;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.DiscordSRV;
import me.kiro.vctime.VCTimeRewards;
import me.kiro.vctime.managers.TimeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.UUID;
import java.lang.reflect.Method;

/**
 * Handles Discord voice channel events through DiscordSRV API
 */
public class DiscordListener implements Listener {
    
    private final VCTimeRewards plugin;
    private final TimeManager timeManager;
    
    public DiscordListener(VCTimeRewards plugin, TimeManager timeManager) {
        this.plugin = plugin;
        this.timeManager = timeManager;
    }
    
    /**
     * Initialize the Discord listener using reflection to access JDA events
     */
    public void initializeListener() {
        plugin.getLogger().info("DiscordListener initialized. Voice channel tracking is ready.");
        
        // Use reflection to register JDA event listener through DiscordSRV
        try {
            // Wait for DiscordSRV JDA to be ready
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                registerJDAListener();
            }, 100L); // 5 seconds delay
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize Discord listener: " + e.getMessage());
        }
    }
    
    /**
     * Register JDA event listener using reflection
     */
    private void registerJDAListener() {
        try {
            Object jda = DiscordSRV.getPlugin().getJda();
            if (jda != null) {
                // Create a listener that uses reflection to handle events
                Object listener = new Object() {
                    @SuppressWarnings("unused")
                    public void onGuildVoiceJoin(Object event) {
                        handleVoiceJoin(event);
                    }
                    
                    @SuppressWarnings("unused")
                    public void onGuildVoiceLeave(Object event) {
                        handleVoiceLeave(event);
                    }
                    
                    @SuppressWarnings("unused")
                    public void onGuildVoiceMove(Object event) {
                        handleVoiceMove(event);
                    }
                };
                
                // Register the listener with JDA
                Method addEventListenerMethod = jda.getClass().getMethod("addEventListener", Object[].class);
                addEventListenerMethod.invoke(jda, new Object[]{listener});
                
                plugin.getLogger().info("Successfully registered Discord voice channel event listeners using reflection.");
            } else {
                plugin.getLogger().warning("DiscordSRV JDA not ready yet. Will retry later.");
                plugin.getServer().getScheduler().runTaskLater(plugin, this::registerJDAListener, 100L);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register JDA listener: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle Discord voice channel join events using reflection
     */
    private void handleVoiceJoin(Object event) {
        try {
            // Get member and channel using reflection
            Method getMemberMethod = event.getClass().getMethod("getMember");
            Method getChannelJoinedMethod = event.getClass().getMethod("getChannelJoined");
            
            Object member = getMemberMethod.invoke(event);
            Object channel = getChannelJoinedMethod.invoke(event);
            
            if (member == null || channel == null) {
                return;
            }
            
            // Get IDs and names using reflection
            String discordId = (String) member.getClass().getMethod("getId").invoke(member);
            String channelId = (String) channel.getClass().getMethod("getId").invoke(channel);
            String memberName = (String) member.getClass().getMethod("getEffectiveName").invoke(member);
            String channelName = (String) channel.getClass().getMethod("getName").invoke(channel);
            
            plugin.getLogger().info("Voice join detected: " + memberName + " joined " + channelName);
            
            processVoiceJoin(discordId, channelId, memberName, channelName, channel);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling voice join event: " + e.getMessage());
        }
    }
    
    /**
     * Handle Discord voice channel leave events using reflection
     */
    private void handleVoiceLeave(Object event) {
        try {
            // Get member and channel using reflection
            Method getMemberMethod = event.getClass().getMethod("getMember");
            Method getChannelLeftMethod = event.getClass().getMethod("getChannelLeft");
            
            Object member = getMemberMethod.invoke(event);
            Object channel = getChannelLeftMethod.invoke(event);
            
            if (member == null || channel == null) {
                return;
            }
            
            // Get IDs and names using reflection
            String discordId = (String) member.getClass().getMethod("getId").invoke(member);
            String channelId = (String) channel.getClass().getMethod("getId").invoke(channel);
            String memberName = (String) member.getClass().getMethod("getEffectiveName").invoke(member);
            String channelName = (String) channel.getClass().getMethod("getName").invoke(channel);
            
            plugin.getLogger().info("Voice leave detected: " + memberName + " left " + channelName);
            
            processVoiceLeave(discordId, channelId, memberName);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling voice leave event: " + e.getMessage());
        }
    }
    
    /**
     * Handle Discord voice channel move events using reflection
     */
    private void handleVoiceMove(Object event) {
        try {
            // Get member and channels using reflection
            Method getMemberMethod = event.getClass().getMethod("getMember");
            Method getChannelLeftMethod = event.getClass().getMethod("getChannelLeft");
            Method getChannelJoinedMethod = event.getClass().getMethod("getChannelJoined");
            
            Object member = getMemberMethod.invoke(event);
            Object oldChannel = getChannelLeftMethod.invoke(event);
            Object newChannel = getChannelJoinedMethod.invoke(event);
            
            if (member == null) {
                return;
            }
            
            // Get member info
            String discordId = (String) member.getClass().getMethod("getId").invoke(member);
            String memberName = (String) member.getClass().getMethod("getEffectiveName").invoke(member);
            
            // Get channel info
            String oldChannelName = oldChannel != null ? (String) oldChannel.getClass().getMethod("getName").invoke(oldChannel) : "unknown";
            String newChannelName = newChannel != null ? (String) newChannel.getClass().getMethod("getName").invoke(newChannel) : "unknown";
            
            plugin.getLogger().info("Voice move detected: " + memberName + " moved from " + oldChannelName + " to " + newChannelName);
            
            processVoiceMove(discordId, oldChannel, newChannel, memberName);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling voice move event: " + e.getMessage());
        }
    }
    
    /**
     * Process voice join with extracted data
     */
    private void processVoiceJoin(String discordId, String channelId, String memberName, String channelName, Object channel) {
        // Check if channel should be tracked
        if (!shouldTrackChannel(channelId)) {
            plugin.getLogger().info("Channel " + channelName + " is not configured for tracking.");
            return;
        }
        
        // Check minimum members requirement using reflection
        try {
            Object membersList = channel.getClass().getMethod("getMembers").invoke(channel);
            int memberCount = ((java.util.List<?>) membersList).size();
            int minMembers = plugin.getConfigUtil().getMinimumMembers();
            
            if (memberCount < minMembers) {
                plugin.getLogger().info("Channel " + channelName + " has " + memberCount + " members, minimum required: " + minMembers);
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not check member count: " + e.getMessage());
        }
        
        // Get linked player UUID
        UUID playerUuid = getLinkedPlayerUuid(discordId);
        if (playerUuid != null) {
            Player player = plugin.getServer().getPlayer(playerUuid);
            String playerName = player != null ? player.getName() : "Unknown Player";
            plugin.getLogger().info("Starting time tracking for player: " + playerName + " (" + playerUuid + ")");
            timeManager.startTracking(playerUuid, channelId);
        } else {
            plugin.getLogger().info("No linked Minecraft account found for Discord user: " + memberName);
        }
    }
    
    /**
     * Process voice leave with extracted data
     */
    private void processVoiceLeave(String discordId, String channelId, String memberName) {
        // Check if channel was being tracked
        if (!shouldTrackChannel(channelId)) {
            return;
        }
        
        // Get linked player UUID
        UUID playerUuid = getLinkedPlayerUuid(discordId);
        if (playerUuid != null) {
            Player player = plugin.getServer().getPlayer(playerUuid);
            String playerName = player != null ? player.getName() : "Unknown Player";
            plugin.getLogger().info("Stopping time tracking for player: " + playerName + " (" + playerUuid + ")");
            timeManager.stopTracking(playerUuid);
        }
    }
    
    /**
     * Process voice move with extracted data
     */
    private void processVoiceMove(String discordId, Object oldChannel, Object newChannel, String memberName) {
        // Get linked player UUID
        UUID playerUuid = getLinkedPlayerUuid(discordId);
        if (playerUuid == null) {
            return;
        }
        
        Player player = plugin.getServer().getPlayer(playerUuid);
        String playerName = player != null ? player.getName() : "Unknown Player";
        
        // Stop tracking old channel if it was being tracked
        if (oldChannel != null) {
            try {
                String oldChannelId = (String) oldChannel.getClass().getMethod("getId").invoke(oldChannel);
                if (shouldTrackChannel(oldChannelId)) {
                    String oldChannelName = (String) oldChannel.getClass().getMethod("getName").invoke(oldChannel);
                    plugin.getLogger().info("Stopping tracking for old channel: " + oldChannelName);
                    timeManager.stopTracking(playerUuid);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error processing old channel: " + e.getMessage());
            }
        }
        
        // Start tracking new channel if it should be tracked
        if (newChannel != null) {
            try {
                String newChannelId = (String) newChannel.getClass().getMethod("getId").invoke(newChannel);
                if (shouldTrackChannel(newChannelId)) {
                    Object membersList = newChannel.getClass().getMethod("getMembers").invoke(newChannel);
                    int memberCount = ((java.util.List<?>) membersList).size();
                    int minMembers = plugin.getConfigUtil().getMinimumMembers();
                    
                    if (memberCount >= minMembers) {
                        String newChannelName = (String) newChannel.getClass().getMethod("getName").invoke(newChannel);
                        plugin.getLogger().info("Starting tracking for new channel: " + newChannelName);
                        timeManager.startTracking(playerUuid, newChannelId);
                    } else {
                        String newChannelName = (String) newChannel.getClass().getMethod("getName").invoke(newChannel);
                        plugin.getLogger().info("New channel " + newChannelName + " has " + memberCount + " members, minimum required: " + minMembers);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error processing new channel: " + e.getMessage());
            }
        }
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
