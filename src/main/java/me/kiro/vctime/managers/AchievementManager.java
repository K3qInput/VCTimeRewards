package me.kiro.vctime.managers;

import me.kiro.vctime.VCTimeRewards;
import me.kiro.vctime.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Achievement system for tracking player milestones and progress
 */
public class AchievementManager {
    
    private final VCTimeRewards plugin;
    private final Map<UUID, Set<String>> playerAchievements;
    private final Map<String, Achievement> achievements;
    
    public AchievementManager(VCTimeRewards plugin) {
        this.plugin = plugin;
        this.playerAchievements = new ConcurrentHashMap<>();
        this.achievements = new HashMap<>();
        
        initializeAchievements();
    }
    
    /**
     * Initialize all achievements
     */
    private void initializeAchievements() {
        // Voice Time Achievements
        achievements.put("first_minute", new Achievement("first_minute", 
            "&e🎤 First Words", "&7Spend your first minute in voice chat", 
            "spend 1 minute in voice", "&e🎤 Welcome to voice chat!"));
            
        achievements.put("voice_novice", new Achievement("voice_novice", 
            "&a🎤 Voice Novice", "&7Spend 1 hour in voice channels", 
            "spend 1 hour in voice", "&a🎤 You're getting the hang of this!"));
            
        achievements.put("voice_enthusiast", new Achievement("voice_enthusiast", 
            "&b🎤 Voice Enthusiast", "&7Spend 10 hours in voice channels", 
            "spend 10 hours in voice", "&b🎤 You love chatting!"));
            
        achievements.put("voice_addict", new Achievement("voice_addict", 
            "&d🎤 Voice Addict", "&7Spend 100 hours in voice channels", 
            "spend 100 hours in voice", "&d🎤 Voice chat is your life!"));
            
        achievements.put("voice_legend", new Achievement("voice_legend", 
            "&6🎤 Voice Legend", "&7Spend 1000 hours in voice channels", 
            "spend 1000 hours in voice", "&6🎤 You are a voice chat legend!"));
        
        // Chat Achievements
        achievements.put("first_message", new Achievement("first_message", 
            "&e💬 First Message", "&7Send your first Discord message", 
            "send 1 Discord message", "&e💬 Welcome to the chat!"));
            
        achievements.put("chatter", new Achievement("chatter", 
            "&a💬 Chatter", "&7Send 100 Discord messages", 
            "send 100 Discord messages", "&a💬 You're quite talkative!"));
            
        achievements.put("conversationalist", new Achievement("conversationalist", 
            "&b💬 Conversationalist", "&7Send 1000 Discord messages", 
            "send 1000 Discord messages", "&b💬 Master of conversation!"));
            
        achievements.put("social_butterfly", new Achievement("social_butterfly", 
            "&d💬 Social Butterfly", "&7Send 10000 Discord messages", 
            "send 10000 Discord messages", "&d💬 You never stop talking!"));
        
        // Session Achievements
        achievements.put("marathon_session", new Achievement("marathon_session", 
            "&c🏃 Marathon Session", "&7Stay in voice for 6 hours straight", 
            "stay in voice for 6 continuous hours", "&c🏃 That's dedication!"));
            
        achievements.put("night_owl", new Achievement("night_owl", 
            "&9🦉 Night Owl", "&7Be in voice between 2-6 AM", 
            "be in voice during late night hours", "&9🦉 Up late chatting!"));
            
        achievements.put("early_bird", new Achievement("early_bird", 
            "&e🐦 Early Bird", "&7Be in voice between 5-8 AM", 
            "be in voice during early morning hours", "&e🐦 Rise and shine!"));
        
        // Streak Achievements
        achievements.put("consistent", new Achievement("consistent", 
            "&a📅 Consistent", "&7Join voice chat 7 days in a row", 
            "maintain a 7-day voice streak", "&a📅 Great consistency!"));
            
        achievements.put("dedicated", new Achievement("dedicated", 
            "&b📅 Dedicated", "&7Join voice chat 30 days in a row", 
            "maintain a 30-day voice streak", "&b📅 Incredible dedication!"));
            
        achievements.put("unstoppable", new Achievement("unstoppable", 
            "&d📅 Unstoppable", "&7Join voice chat 100 days in a row", 
            "maintain a 100-day voice streak", "&d📅 You are unstoppable!"));
        
        // Social Achievements
        achievements.put("team_player", new Achievement("team_player", 
            "&e👥 Team Player", "&7Be in voice with 5+ people for 1 hour", 
            "spend time in active voice channels", "&e👥 Great teamwork!"));
            
        achievements.put("popular", new Achievement("popular", 
            "&b👥 Popular", "&7Have voice sessions with 50+ different people", 
            "voice chat with many different people", "&b👥 Everyone loves you!"));
        
        // Reward Achievements
        achievements.put("first_reward", new Achievement("first_reward", 
            "&e🎁 First Reward", "&7Receive your first voice time reward", 
            "earn your first reward", "&e🎁 Many more to come!"));
            
        achievements.put("reward_collector", new Achievement("reward_collector", 
            "&a🎁 Reward Collector", "&7Receive 50 voice time rewards", 
            "collect 50 rewards", "&a🎁 You love collecting rewards!"));
            
        achievements.put("treasure_hunter", new Achievement("treasure_hunter", 
            "&d🎁 Treasure Hunter", "&7Receive 500 voice time rewards", 
            "collect 500 rewards", "&d🎁 Master treasure hunter!"));
        
        plugin.getLogger().info("Initialized " + achievements.size() + " achievements");
    }
    
    /**
     * Check achievements for a player based on their statistics
     */
    public void checkAchievements(UUID playerId, StatisticsManager.PlayerStatistics stats) {
        Set<String> playerAchs = playerAchievements.computeIfAbsent(playerId, k -> new HashSet<>());
        
        // Voice time achievements
        checkVoiceTimeAchievements(playerId, stats, playerAchs);
        
        // Chat achievements
        checkChatAchievements(playerId, stats, playerAchs);
        
        // Session achievements
        checkSessionAchievements(playerId, stats, playerAchs);
        
        // Streak achievements
        checkStreakAchievements(playerId, stats, playerAchs);
        
        // Reward achievements
        checkRewardAchievements(playerId, stats, playerAchs);
    }
    
    /**
     * Check voice time related achievements
     */
    private void checkVoiceTimeAchievements(UUID playerId, StatisticsManager.PlayerStatistics stats, Set<String> playerAchs) {
        long voiceTimeMinutes = stats.getTotalVoiceTime() / (1000 * 60);
        
        if (voiceTimeMinutes >= 1 && !playerAchs.contains("first_minute")) {
            unlockAchievement(playerId, "first_minute");
        }
        if (voiceTimeMinutes >= 60 && !playerAchs.contains("voice_novice")) {
            unlockAchievement(playerId, "voice_novice");
        }
        if (voiceTimeMinutes >= 600 && !playerAchs.contains("voice_enthusiast")) {
            unlockAchievement(playerId, "voice_enthusiast");
        }
        if (voiceTimeMinutes >= 6000 && !playerAchs.contains("voice_addict")) {
            unlockAchievement(playerId, "voice_addict");
        }
        if (voiceTimeMinutes >= 60000 && !playerAchs.contains("voice_legend")) {
            unlockAchievement(playerId, "voice_legend");
        }
    }
    
    /**
     * Check chat related achievements
     */
    private void checkChatAchievements(UUID playerId, StatisticsManager.PlayerStatistics stats, Set<String> playerAchs) {
        int messages = stats.getTotalMessages();
        
        if (messages >= 1 && !playerAchs.contains("first_message")) {
            unlockAchievement(playerId, "first_message");
        }
        if (messages >= 100 && !playerAchs.contains("chatter")) {
            unlockAchievement(playerId, "chatter");
        }
        if (messages >= 1000 && !playerAchs.contains("conversationalist")) {
            unlockAchievement(playerId, "conversationalist");
        }
        if (messages >= 10000 && !playerAchs.contains("social_butterfly")) {
            unlockAchievement(playerId, "social_butterfly");
        }
    }
    
    /**
     * Check session related achievements
     */
    private void checkSessionAchievements(UUID playerId, StatisticsManager.PlayerStatistics stats, Set<String> playerAchs) {
        long longestSessionHours = stats.getLongestSession() / (1000 * 60 * 60);
        
        if (longestSessionHours >= 6 && !playerAchs.contains("marathon_session")) {
            unlockAchievement(playerId, "marathon_session");
        }
        
        // Time-based achievements would need current time checking
        if (stats.isCurrentlyInVoice()) {
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            
            if (hour >= 2 && hour < 6 && !playerAchs.contains("night_owl")) {
                unlockAchievement(playerId, "night_owl");
            }
            if (hour >= 5 && hour < 8 && !playerAchs.contains("early_bird")) {
                unlockAchievement(playerId, "early_bird");
            }
        }
    }
    
    /**
     * Check streak related achievements
     */
    private void checkStreakAchievements(UUID playerId, StatisticsManager.PlayerStatistics stats, Set<String> playerAchs) {
        int streak = stats.getCurrentStreak();
        
        if (streak >= 7 && !playerAchs.contains("consistent")) {
            unlockAchievement(playerId, "consistent");
        }
        if (streak >= 30 && !playerAchs.contains("dedicated")) {
            unlockAchievement(playerId, "dedicated");
        }
        if (streak >= 100 && !playerAchs.contains("unstoppable")) {
            unlockAchievement(playerId, "unstoppable");
        }
    }
    
    /**
     * Check reward related achievements
     */
    private void checkRewardAchievements(UUID playerId, StatisticsManager.PlayerStatistics stats, Set<String> playerAchs) {
        int rewards = stats.getTotalRewards();
        
        if (rewards >= 1 && !playerAchs.contains("first_reward")) {
            unlockAchievement(playerId, "first_reward");
        }
        if (rewards >= 50 && !playerAchs.contains("reward_collector")) {
            unlockAchievement(playerId, "reward_collector");
        }
        if (rewards >= 500 && !playerAchs.contains("treasure_hunter")) {
            unlockAchievement(playerId, "treasure_hunter");
        }
    }
    
    /**
     * Unlock an achievement for a player
     */
    private void unlockAchievement(UUID playerId, String achievementId) {
        Achievement achievement = achievements.get(achievementId);
        if (achievement == null) return;
        
        Set<String> playerAchs = playerAchievements.computeIfAbsent(playerId, k -> new HashSet<>());
        playerAchs.add(achievementId);
        
        // Notify player if online
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            notifyPlayerAchievement(player, achievement);
        }
        
        plugin.getLogger().info("Player " + playerId + " unlocked achievement: " + achievementId);
    }
    
    /**
     * Notify player of achievement unlock
     */
    private void notifyPlayerAchievement(Player player, Achievement achievement) {
        // Send achievement message
        player.sendMessage("");
        player.sendMessage(ColorUtil.translateColors("&6&l🏆 ACHIEVEMENT UNLOCKED! 🏆"));
        player.sendMessage(ColorUtil.translateColors("&f" + achievement.getDisplayName()));
        player.sendMessage(ColorUtil.translateColors("&7" + achievement.getDescription()));
        player.sendMessage(ColorUtil.translateColors("&e" + achievement.getUnlockMessage()));
        player.sendMessage("");
        
        // Play achievement sound
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        
        // Send title
        player.sendTitle(
            ColorUtil.translateColors("&6&l🏆 ACHIEVEMENT!"),
            ColorUtil.translateColors(achievement.getDisplayName()),
            10, 60, 20
        );
        
        // Announce to server if it's a rare achievement
        if (isRareAchievement(achievement.getId())) {
            Bukkit.broadcastMessage(ColorUtil.translateColors(
                "&6🏆 &e" + player.getName() + " &6unlocked the rare achievement " + achievement.getDisplayName() + "&6!"));
        }
    }
    
    /**
     * Check if an achievement is considered rare
     */
    private boolean isRareAchievement(String achievementId) {
        return achievementId.contains("legend") || achievementId.contains("unstoppable") || 
               achievementId.contains("treasure_hunter") || achievementId.contains("social_butterfly");
    }
    
    /**
     * Get all achievements for a player
     */
    public Set<String> getPlayerAchievements(UUID playerId) {
        return new HashSet<>(playerAchievements.getOrDefault(playerId, new HashSet<>()));
    }
    
    /**
     * Get achievement progress for a player
     */
    public double getAchievementProgress(UUID playerId) {
        Set<String> playerAchs = getPlayerAchievements(playerId);
        return (double) playerAchs.size() / achievements.size();
    }
    
    /**
     * Get all available achievements
     */
    public Map<String, Achievement> getAllAchievements() {
        return new HashMap<>(achievements);
    }
    
    /**
     * Achievement data class
     */
    public static class Achievement {
        private final String id;
        private final String displayName;
        private final String description;
        private final String requirement;
        private final String unlockMessage;
        
        public Achievement(String id, String displayName, String description, String requirement, String unlockMessage) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.requirement = requirement;
            this.unlockMessage = unlockMessage;
        }
        
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getRequirement() { return requirement; }
        public String getUnlockMessage() { return unlockMessage; }
    }
}