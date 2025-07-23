package me.kiro.vctime.discord;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.DiscordSRV;
import me.kiro.vctime.VCTimeRewards;
import me.kiro.vctime.managers.TimeManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.UUID;

/**
 * Handles Discord voice channel events through JDA via DiscordSRV
 */
public class DiscordListener extends ListenerAdapter implements Listener {
    
    private final VCTimeRewards plugin;
    private final TimeManager timeManager;
    
    public DiscordListener(VCTimeRewards plugin, TimeManager timeManager) {
        this.plugin = plugin;
        this.timeManager = timeManager;
    }
    
    /**
     * Initialize the Discord listener and register it with DiscordSRV's JDA instance
     */
    public void initializeListener() {
        plugin.getLogger().info("DiscordListener initialized. Voice channel tracking is ready.");
        
        // Register this listener with JDA through DiscordSRV
        try {
            if (DiscordSRV.getPlugin().getJda() != null) {
                DiscordSRV.getPlugin().getJda().addEventListener(this);
                plugin.getLogger().info("Successfully registered Discord voice channel event listeners.");
            } else {
                plugin.getLogger().warning("DiscordSRV JDA not ready yet. Will retry later.");
                // Schedule a delayed retry
                plugin.getServer().getScheduler().runTaskLater(plugin, this::initializeListener, 100L); // 5 seconds
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register Discord listener: " + e.getMessage());
        }
    }
    
    /**
     * Handle Discord voice channel join events
     */
    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        Member member = event.getMember();
        VoiceChannel channel = event.getChannelJoined();
        
        if (member == null || channel == null) {
            return;
        }
        
        String discordId = member.getId();
        String channelId = channel.getId();
        
        plugin.getLogger().info("Voice join detected: " + member.getEffectiveName() + " joined " + channel.getName());
        
        // Check if channel should be tracked
        if (!shouldTrackChannel(channelId)) {
            plugin.getLogger().info("Channel " + channel.getName() + " is not configured for tracking.");
            return;
        }
        
        // Check minimum members requirement
        int memberCount = channel.getMembers().size();
        int minMembers = plugin.getConfigUtil().getMinimumMembers();
        if (memberCount < minMembers) {
            plugin.getLogger().info("Channel " + channel.getName() + " has " + memberCount + " members, minimum required: " + minMembers);
            return;
        }
        
        // Get linked player UUID (works for both online and offline players)
        UUID playerUuid = getLinkedPlayerUuid(discordId);
        if (playerUuid != null) {
            Player player = plugin.getServer().getPlayer(playerUuid);
            String playerName = player != null ? player.getName() : "Unknown Player";
            plugin.getLogger().info("Starting time tracking for player: " + playerName + " (" + playerUuid + ")");
            timeManager.startTracking(playerUuid, channelId);
        } else {
            plugin.getLogger().info("No linked Minecraft account found for Discord user: " + member.getEffectiveName());
        }
    }
    
    /**
     * Handle Discord voice channel leave events
     */
    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        Member member = event.getMember();
        VoiceChannel channel = event.getChannelLeft();
        
        if (member == null || channel == null) {
            return;
        }
        
        String discordId = member.getId();
        String channelId = channel.getId();
        
        plugin.getLogger().info("Voice leave detected: " + member.getEffectiveName() + " left " + channel.getName());
        
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
     * Handle Discord voice channel move events (user switches channels)
     */
    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        Member member = event.getMember();
        VoiceChannel oldChannel = event.getChannelLeft();
        VoiceChannel newChannel = event.getChannelJoined();
        
        if (member == null) {
            return;
        }
        
        String discordId = member.getId();
        plugin.getLogger().info("Voice move detected: " + member.getEffectiveName() + " moved from " + 
                               (oldChannel != null ? oldChannel.getName() : "unknown") + " to " + 
                               (newChannel != null ? newChannel.getName() : "unknown"));
        
        // Get linked player UUID
        UUID playerUuid = getLinkedPlayerUuid(discordId);
        if (playerUuid == null) {
            return;
        }
        
        Player player = plugin.getServer().getPlayer(playerUuid);
        String playerName = player != null ? player.getName() : "Unknown Player";
        
        // Stop tracking old channel if it was being tracked
        if (oldChannel != null && shouldTrackChannel(oldChannel.getId())) {
            plugin.getLogger().info("Stopping tracking for old channel: " + oldChannel.getName());
            timeManager.stopTracking(playerUuid);
        }
        
        // Start tracking new channel if it should be tracked
        if (newChannel != null && shouldTrackChannel(newChannel.getId())) {
            int memberCount = newChannel.getMembers().size();
            int minMembers = plugin.getConfigUtil().getMinimumMembers();
            
            if (memberCount >= minMembers) {
                plugin.getLogger().info("Starting tracking for new channel: " + newChannel.getName());
                timeManager.startTracking(playerUuid, newChannel.getId());
            } else {
                plugin.getLogger().info("New channel " + newChannel.getName() + " has " + memberCount + " members, minimum required: " + minMembers);
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
