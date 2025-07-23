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
import java.lang.reflect.Method;

/**
 * Handles Discord voice channel events through DiscordSRV API and periodic checks
 */
public class DiscordListener implements Listener {
    
    private final VCTimeRewards plugin;
    private final TimeManager timeManager;
    private final Map<UUID, String> currentVoiceChannels = new HashMap<>();
    private BukkitRunnable voiceCheckTask;
    
    public DiscordListener(VCTimeRewards plugin, TimeManager timeManager) {
        this.plugin = plugin;
        this.timeManager = timeManager;
    }
    
    /**
     * Initialize the Discord listener with periodic voice channel checking
     */
    public void initializeListener() {
        plugin.getLogger().info("DiscordListener initialized. Starting periodic voice channel checks...");
        
        // Wait for DiscordSRV to be ready
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            startVoiceChannelChecking();
        }, 100L); // 5 seconds delay
    }
    
    /**
     * Start periodic voice channel checking
     */
    private void startVoiceChannelChecking() {
        if (voiceCheckTask != null) {
            voiceCheckTask.cancel();
        }
        
        voiceCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkVoiceChannels();
            }
        };
        
        // Check every 15 seconds
        voiceCheckTask.runTaskTimerAsynchronously(plugin, 0L, 300L);
        plugin.getLogger().info("Started voice channel checking every 15 seconds.");
    }
    
    /**
     * Check current voice channel status for all linked players
     */
    private void checkVoiceChannels() {
        try {
            Object jda = DiscordSRV.getPlugin().getJda();
            if (jda == null) {
                return;
            }
            
            // Get all guilds from JDA
            Object guildsCollection = jda.getClass().getMethod("getGuilds").invoke(jda);
            java.util.List<?> guilds = (java.util.List<?>) guildsCollection;
            
            Set<UUID> playersInVoice = new HashSet<>();
            
            for (Object guild : guilds) {
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
                    
                    // Check minimum members requirement
                    int minMembers = plugin.getConfigUtil().getMinimumMembers();
                    if (members.size() < minMembers) {
                        continue;
                    }
                    
                    // Process each member in the channel
                    for (Object member : members) {
                        String discordId = (String) member.getClass().getMethod("getId").invoke(member);
                        String memberName = (String) member.getClass().getMethod("getEffectiveName").invoke(member);
                        
                        UUID playerUuid = getLinkedPlayerUuid(discordId);
                        if (playerUuid != null) {
                            playersInVoice.add(playerUuid);
                            
                            // Check if player just joined this channel
                            String previousChannel = currentVoiceChannels.get(playerUuid);
                            if (!channelId.equals(previousChannel)) {
                                // Player joined new channel or switched channels
                                if (previousChannel != null) {
                                    // Stop tracking previous channel
                                    plugin.getLogger().info("Player " + memberName + " left previous voice channel");
                                    timeManager.stopTracking(playerUuid);
                                }
                                
                                // Start tracking new channel
                                plugin.getLogger().info("Player " + memberName + " joined voice channel: " + channelName);
                                timeManager.startTracking(playerUuid, channelId);
                                currentVoiceChannels.put(playerUuid, channelId);
                            }
                        }
                    }
                }
            }
            
            // Check for players who left voice channels
            Set<UUID> playersToRemove = new HashSet<>();
            for (Map.Entry<UUID, String> entry : currentVoiceChannels.entrySet()) {
                UUID playerUuid = entry.getKey();
                if (!playersInVoice.contains(playerUuid)) {
                    // Player left voice channel
                    plugin.getLogger().info("Player left voice channel, stopping tracking");
                    timeManager.stopTracking(playerUuid);
                    playersToRemove.add(playerUuid);
                }
            }
            
            // Remove players who left
            for (UUID playerUuid : playersToRemove) {
                currentVoiceChannels.remove(playerUuid);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking voice channels: " + e.getMessage());
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
