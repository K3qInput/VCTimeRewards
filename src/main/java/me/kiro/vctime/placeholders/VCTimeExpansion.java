package me.kiro.vctime.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.kiro.vctime.VCTimeRewards;
import me.kiro.vctime.managers.TimeManager;
import me.kiro.vctime.utils.TimeFormatter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * PlaceholderAPI expansion for VCTimeRewards
 * Provides placeholders for voice time, chat messages, and session data
 */
public class VCTimeExpansion extends PlaceholderExpansion {
    
    private final VCTimeRewards plugin;
    
    public VCTimeExpansion(VCTimeRewards plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "vctime";
    }
    
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true; // This expansion will persist through PlaceholderAPI reloads
    }
    
    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }
        
        UUID playerId = player.getUniqueId();
        
        // Voice time placeholders
        if (params.equals("total")) {
            long totalMillis = plugin.getTimeManager().getTotalTime(playerId);
            long totalMinutes = totalMillis / 60000; // Convert milliseconds to minutes
            return TimeFormatter.formatTime(totalMinutes);
        }
        
        if (params.equals("total_minutes")) {
            long totalMillis = plugin.getTimeManager().getTotalTime(playerId);
            return String.valueOf(totalMillis / 60000); // Convert milliseconds to minutes
        }
        
        if (params.equals("total_hours")) {
            long totalMillis = plugin.getTimeManager().getTotalTime(playerId);
            long totalMinutes = totalMillis / 60000; // Convert milliseconds to minutes
            return String.valueOf(totalMinutes / 60);
        }
        
        if (params.equals("total_formatted")) {
            long totalMillis = plugin.getTimeManager().getTotalTime(playerId);
            long totalMinutes = totalMillis / 60000; // Convert milliseconds to minutes
            return TimeFormatter.formatTimeDetailed(totalMinutes);
        }
        
        if (params.equals("total_seconds")) {
            long totalMillis = plugin.getTimeManager().getTotalTime(playerId);
            return String.valueOf(totalMillis / 1000); // Convert milliseconds to seconds
        }
        
        // Session placeholders (only work for online players)
        if (player.isOnline()) {
            Player onlinePlayer = player.getPlayer();
            
            if (params.equals("session")) {
                long sessionMillis = plugin.getTimeManager().getSessionTime(playerId);
                long sessionMinutes = sessionMillis / 60000; // Convert milliseconds to minutes
                return TimeFormatter.formatTime(sessionMinutes);
            }
            
            if (params.equals("session_minutes")) {
                long sessionMillis = plugin.getTimeManager().getSessionTime(playerId);
                return String.valueOf(sessionMillis / 60000); // Convert milliseconds to minutes
            }
            
            if (params.equals("session_formatted")) {
                long sessionMillis = plugin.getTimeManager().getSessionTime(playerId);
                long sessionMinutes = sessionMillis / 60000; // Convert milliseconds to minutes
                return TimeFormatter.formatTimeDetailed(sessionMinutes);
            }
            
            if (params.equals("status")) {
                boolean inVoice = plugin.getTimeManager().isPlayerInTrackedVoiceChannel(playerId);
                return inVoice ? "In Voice" : "Not in Voice";
            }
            
            if (params.equals("status_color")) {
                boolean inVoice = plugin.getTimeManager().isPlayerInTrackedVoiceChannel(playerId);
                return inVoice ? "&a" : "&c";
            }
            
            if (params.equals("status_symbol")) {
                boolean inVoice = plugin.getTimeManager().isPlayerInTrackedVoiceChannel(playerId);
                return inVoice ? "🔊" : "🔇";
            }
            
            if (params.equals("channel")) {
                // First try StatisticsManager for current channel
                if (plugin.getStatisticsManager() != null) {
                    me.kiro.vctime.managers.StatisticsManager.PlayerStatistics stats = plugin.getStatisticsManager().getStats(playerId);
                    String channelId = stats.getCurrentChannel();
                    if (channelId != null) {
                        // Try to get channel name instead of ID
                        try {
                            Object jda = github.scarsz.discordsrv.DiscordSRV.getPlugin().getJda();
                            if (jda != null) {
                                Object channel = jda.getClass().getMethod("getVoiceChannelById", String.class).invoke(jda, channelId);
                                if (channel != null) {
                                    return (String) channel.getClass().getMethod("getName").invoke(channel);
                                }
                            }
                        } catch (Exception e) {
                            // Fall back to ID if name lookup fails
                        }
                        return channelId;
                    }
                }
                
                // Fallback to TimeManager
                String channelId = plugin.getTimeManager().getCurrentChannel(playerId);
                if (channelId != null) {
                    // Try to get channel name instead of ID
                    try {
                        Object jda = github.scarsz.discordsrv.DiscordSRV.getPlugin().getJda();
                        if (jda != null) {
                            Object channel = jda.getClass().getMethod("getVoiceChannelById", String.class).invoke(jda, channelId);
                            if (channel != null) {
                                return (String) channel.getClass().getMethod("getName").invoke(channel);
                            }
                        }
                    } catch (Exception e) {
                        // Fall back to ID if name lookup fails
                    }
                    return channelId;
                }
                return "None";
            }
            
            if (params.equals("is_online")) {
                boolean inVoice = plugin.getTimeManager().isPlayerInTrackedVoiceChannel(playerId);
                return String.valueOf(inVoice);
            }
            
            if (params.equals("is_tracking")) {
                boolean tracking = plugin.getTimeManager().isPlayerInTrackedVoiceChannel(playerId);
                return String.valueOf(tracking);
            }
        }
        
        // Chat message placeholders
        if (params.equals("messages")) {
            return String.valueOf(plugin.getChatManager().getMessageCount(playerId));
        }
        
        if (params.equals("messages_formatted")) {
            int messages = plugin.getChatManager().getMessageCount(playerId);
            return formatNumber(messages);
        }
        
        // Leaderboard placeholders
        if (params.startsWith("top_")) {
            return handleLeaderboardPlaceholder(params);
        }
        
        // Rank placeholders
        if (params.equals("rank")) {
            return String.valueOf(plugin.getTimeManager().getPlayerRank(playerId));
        }
        
        if (params.equals("rank_suffix")) {
            int rank = plugin.getTimeManager().getPlayerRank(playerId);
            return getOrdinalSuffix(rank);
        }
        
        // Progress placeholders
        if (params.startsWith("progress_")) {
            return handleProgressPlaceholder(playerId, params);
        }
        
        // Statistics
        if (params.equals("avg_session")) {
            long avgMillis = plugin.getTimeManager().getAverageSessionTime(playerId);
            long avgMinutes = avgMillis / 60000; // Convert milliseconds to minutes
            return TimeFormatter.formatTime(avgMinutes);
        }
        
        if (params.equals("sessions_count")) {
            return String.valueOf(plugin.getTimeManager().getSessionCount(playerId));
        }
        
        // Daily time placeholders using StatisticsManager
        if (params.equals("today")) {
            if (plugin.getStatisticsManager() != null) {
                me.kiro.vctime.managers.StatisticsManager.PlayerStatistics stats = plugin.getStatisticsManager().getStats(playerId);
                long todayMillis = stats.getVoiceTimeToday();
                long todayMinutes = todayMillis / 60000; // Convert milliseconds to minutes
                return TimeFormatter.formatTime(todayMinutes);
            }
            return "0m";
        }
        
        if (params.equals("today_formatted")) {
            if (plugin.getStatisticsManager() != null) {
                me.kiro.vctime.managers.StatisticsManager.PlayerStatistics stats = plugin.getStatisticsManager().getStats(playerId);
                long todayMillis = stats.getVoiceTimeToday();
                long todayMinutes = todayMillis / 60000;
                return TimeFormatter.formatTimeDetailed(todayMinutes);
            }
            return "0 minutes";
        }
        
        if (params.equals("yesterday")) {
            if (plugin.getStatisticsManager() != null) {
                me.kiro.vctime.managers.StatisticsManager.PlayerStatistics stats = plugin.getStatisticsManager().getStats(playerId);
                long yesterdayMillis = stats.getVoiceTimeYesterday();
                long yesterdayMinutes = yesterdayMillis / 60000;
                return TimeFormatter.formatTime(yesterdayMinutes);
            }
            return "0m";
        }
        
        // Message placeholders using StatisticsManager
        if (params.equals("messages_today")) {
            if (plugin.getStatisticsManager() != null) {
                me.kiro.vctime.managers.StatisticsManager.PlayerStatistics stats = plugin.getStatisticsManager().getStats(playerId);
                return String.valueOf(stats.getMessagesToday());
            }
            return "0";
        }
        
        if (params.equals("sessions_today")) {
            if (plugin.getStatisticsManager() != null) {
                me.kiro.vctime.managers.StatisticsManager.PlayerStatistics stats = plugin.getStatisticsManager().getStats(playerId);
                return String.valueOf(stats.getSessionsToday());
            }
            return "0";
        }
        
        if (params.equals("longest_session")) {
            if (plugin.getStatisticsManager() != null) {
                me.kiro.vctime.managers.StatisticsManager.PlayerStatistics stats = plugin.getStatisticsManager().getStats(playerId);
                long longestMillis = stats.getLongestSession();
                long longestMinutes = longestMillis / 60000;
                return TimeFormatter.formatTime(longestMinutes);
            }
            return "0m";
        }
        
        if (params.equals("last_seen")) {
            if (plugin.getStatisticsManager() != null) {
                me.kiro.vctime.managers.StatisticsManager.PlayerStatistics stats = plugin.getStatisticsManager().getStats(playerId);
                long lastSeenTime = stats.getLastSeenTime();
                if (lastSeenTime > 0) {
                    // Format as "X hours ago", "X days ago", etc.
                    long timeDiff = System.currentTimeMillis() - lastSeenTime;
                    long minutes = timeDiff / (60 * 1000);
                    long hours = minutes / 60;
                    long days = hours / 24;
                    
                    if (days > 0) {
                        return days + (days == 1 ? " day ago" : " days ago");
                    } else if (hours > 0) {
                        return hours + (hours == 1 ? " hour ago" : " hours ago");
                    } else if (minutes > 0) {
                        return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
                    } else {
                        return "Just now";
                    }
                }
            }
            return "Never";
        }
        
        if (params.equals("streak_days")) {
            if (plugin.getStatisticsManager() != null) {
                me.kiro.vctime.managers.StatisticsManager.PlayerStatistics stats = plugin.getStatisticsManager().getStats(playerId);
                return String.valueOf(stats.getCurrentStreak());
            }
            return "0";
        }
        
        if (params.equals("best_streak")) {
            if (plugin.getStatisticsManager() != null) {
                me.kiro.vctime.managers.StatisticsManager.PlayerStatistics stats = plugin.getStatisticsManager().getStats(playerId);
                return String.valueOf(stats.getBestStreak());
            }
            return "0";
        }
        
        return null; // Placeholder not found
    }
    
    /**
     * Handle leaderboard placeholders like top_1_name, top_1_time, etc.
     */
    private String handleLeaderboardPlaceholder(String params) {
        // Parse: top_<position>_<type>
        String[] parts = params.split("_", 3);
        if (parts.length != 3) {
            return "";
        }
        
        try {
            int position = Integer.parseInt(parts[1]);
            String type = parts[2];
            
            // Get leaderboard data
            List<TimeManager.PlayerTimeEntry> leaderboard = plugin.getTimeManager().getTopPlayers(Math.max(position, 10));
            
            if (position < 1 || position > leaderboard.size()) {
                return "";
            }
            
            TimeManager.PlayerTimeEntry entry = leaderboard.get(position - 1);
            
            switch (type) {
                case "name":
                    return entry.getPlayerName();
                case "time":
                    long timeMinutes = entry.getTotalTime() / 60000; // Convert milliseconds to minutes
                    return TimeFormatter.formatTime(timeMinutes);
                case "time_minutes":
                    return String.valueOf(entry.getTotalTime() / 60000); // Convert milliseconds to minutes
                case "time_hours":
                    long totalMinutes = entry.getTotalTime() / 60000; // Convert milliseconds to minutes
                    return String.valueOf(totalMinutes / 60);
                case "messages":
                    return String.valueOf(plugin.getChatManager().getMessageCount(entry.getPlayerId()));
                default:
                    return "";
            }
        } catch (NumberFormatException e) {
            return "";
        }
    }
    
    /**
     * Handle progress placeholders like progress_1h_percent, progress_1h_bar
     */
    private String handleProgressPlaceholder(UUID playerId, String params) {
        // Parse: progress_<threshold>_<type>
        String[] parts = params.split("_", 3);
        if (parts.length != 3) {
            return "";
        }
        
        String threshold = parts[1];
        String type = parts[2];
        
        try {
            long targetMinutes = parseTimeThreshold(threshold);
            long currentMillis = plugin.getTimeManager().getTotalTime(playerId);
            long currentMinutes = currentMillis / 60000; // Convert milliseconds to minutes
            
            if (type.equals("percent")) {
                double progress = Math.min((double) currentMinutes / targetMinutes * 100, 100);
                return String.format("%.1f", progress);
            } else if (type.equals("bar")) {
                return generateProgressBar(currentMinutes, targetMinutes, 20);
            } else if (type.equals("remaining")) {
                long remaining = Math.max(0, targetMinutes - currentMinutes);
                return TimeFormatter.formatTime(remaining);
            }
        } catch (Exception e) {
            return "";
        }
        
        return "";
    }
    
    /**
     * Parse time threshold like "1h", "30m", "2h30m"
     */
    private long parseTimeThreshold(String threshold) {
        threshold = threshold.toLowerCase();
        long totalMinutes = 0;
        
        // Handle hours
        if (threshold.contains("h")) {
            int hourIndex = threshold.indexOf("h");
            String hoursPart = threshold.substring(0, hourIndex);
            try {
                totalMinutes += Integer.parseInt(hoursPart) * 60;
                threshold = threshold.substring(hourIndex + 1);
            } catch (NumberFormatException e) {
                // Invalid format
            }
        }
        
        // Handle minutes
        if (threshold.contains("m")) {
            int minuteIndex = threshold.indexOf("m");
            String minutesPart = threshold.substring(0, minuteIndex);
            try {
                totalMinutes += Integer.parseInt(minutesPart);
            } catch (NumberFormatException e) {
                // Invalid format
            }
        }
        
        // If no unit specified, assume hours
        if (!threshold.contains("h") && !threshold.contains("m")) {
            try {
                totalMinutes = Integer.parseInt(threshold) * 60;
            } catch (NumberFormatException e) {
                // Invalid format
            }
        }
        
        return totalMinutes;
    }
    
    /**
     * Generate a progress bar
     */
    private String generateProgressBar(long current, long target, int length) {
        double progress = Math.min((double) current / target, 1.0);
        int filled = (int) (progress * length);
        
        StringBuilder bar = new StringBuilder("&7[");
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("&a▰");
            } else {
                bar.append("&7▱");
            }
        }
        bar.append("&7]");
        
        return bar.toString();
    }
    
    /**
     * Format numbers with commas
     */
    private String formatNumber(int number) {
        return String.format("%,d", number);
    }
    
    /**
     * Get ordinal suffix (1st, 2nd, 3rd, etc.)
     */
    private String getOrdinalSuffix(int number) {
        if (number >= 11 && number <= 13) {
            return "th";
        }
        switch (number % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }
}