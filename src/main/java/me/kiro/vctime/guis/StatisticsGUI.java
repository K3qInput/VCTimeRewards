package me.kiro.vctime.guis;

import me.kiro.vctime.VCTimeRewards;
import me.kiro.vctime.managers.AchievementManager;
import me.kiro.vctime.managers.StatisticsManager;
import me.kiro.vctime.utils.ColorUtil;
import me.kiro.vctime.utils.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Professional statistics GUI showing detailed player data
 */
public class StatisticsGUI implements Listener {
    
    private final VCTimeRewards plugin;
    private final Map<UUID, String> openGuis = new HashMap<>();
    
    public StatisticsGUI(VCTimeRewards plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Open the main statistics menu for a player
     */
    public void openStatisticsMenu(Player player, Player target) {
        UUID targetId = target.getUniqueId();
        StatisticsManager.PlayerStatistics stats = plugin.getStatisticsManager().getStats(targetId);
        Set<String> achievements = plugin.getAchievementManager().getPlayerAchievements(targetId);
        
        String title = player.equals(target) ? 
            ColorUtil.translateColors("&e&l📊 Your Statistics") :
            ColorUtil.translateColors("&e&l📊 " + target.getName() + "'s Statistics");
            
        Inventory gui = Bukkit.createInventory(null, 54, title);
        
        // Add decorative border
        addStatsBorder(gui);
        
        // Player head
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(target);
            headMeta.setDisplayName(ColorUtil.translateColors("&e&l" + target.getName()));
            headMeta.setLore(Arrays.asList(
                ColorUtil.translateColors("&7📊 Detailed player statistics"),
                ColorUtil.translateColors("&7🏆 Achievement progress: &e" + 
                    String.format("%.1f%%", plugin.getAchievementManager().getAchievementProgress(targetId) * 100)),
                "",
                ColorUtil.translateColors("&7First joined: &f" + formatDate(stats.getFirstJoinTime())),
                ColorUtil.translateColors("&7Last seen: &f" + formatDate(stats.getLastSeenTime()))
            ));
            playerHead.setItemMeta(headMeta);
        }
        gui.setItem(13, playerHead);
        
        // Voice Time Statistics
        gui.setItem(20, createItem(Material.DIAMOND, 
            "&b&l🎤 Voice Statistics", 
            "&7Total voice time: &f" + TimeFormatter.formatTime(stats.getTotalVoiceTime()),
            "&7Today: &f" + TimeFormatter.formatTime(stats.getVoiceTimeToday()),
            "&7Yesterday: &f" + TimeFormatter.formatTime(stats.getVoiceTimeYesterday()),
            "&7Total sessions: &f" + stats.getTotalSessions(),
            "&7Average session: &f" + TimeFormatter.formatTime(stats.getAverageSessionLength()),
            "&7Longest session: &f" + TimeFormatter.formatTime(stats.getLongestSession()),
            "&7Current streak: &f" + stats.getCurrentStreak() + " days",
            "",
            ColorUtil.translateColors("&e➤ Click for detailed voice stats")));
            
        // Chat Statistics
        gui.setItem(22, createItem(Material.WRITABLE_BOOK, 
            "&a&l💬 Chat Statistics", 
            "&7Total messages: &f" + formatNumber(stats.getTotalMessages()),
            "&7Messages today: &f" + formatNumber(stats.getMessagesToday()),
            "&7Messages per session: &f" + formatNumber(stats.getTotalSessions() > 0 ? 
                stats.getTotalMessages() / stats.getTotalSessions() : 0),
            "",
            "&7Chat activity shows your engagement",
            "&7in the Discord community!",
            "",
            ColorUtil.translateColors("&e➤ Click for detailed chat stats")));
            
        // Achievement Progress
        gui.setItem(24, createItem(Material.GOLD_BLOCK, 
            "&6&l🏆 Achievements", 
            "&7Unlocked: &f" + achievements.size() + "&7/&f" + plugin.getAchievementManager().getAllAchievements().size(),
            "&7Progress: &e" + String.format("%.1f%%", plugin.getAchievementManager().getAchievementProgress(targetId) * 100),
            "",
            "&7Recent achievements:",
            getRecentAchievements(achievements),
            "",
            ColorUtil.translateColors("&e➤ Click to view all achievements")));
            
        // Current Status
        gui.setItem(31, createItem(Material.REDSTONE_LAMP, 
            "&d&l📍 Current Status", 
            "&7Currently in voice: " + (stats.isCurrentlyInVoice() ? "&aYes" : "&cNo"),
            "&7Current channel: &f" + (stats.getCurrentChannel() != null ? stats.getCurrentChannel() : "None"),
            "&7Session time: &f" + (stats.isCurrentlyInVoice() ? 
                TimeFormatter.formatTime(System.currentTimeMillis() - stats.getLastSeenTime()) : "N/A"),
            "",
            "&7Real-time status information",
            "",
            ColorUtil.translateColors("&e➤ Click to refresh status")));
            
        // Ranking Information
        gui.setItem(33, createItem(Material.EMERALD, 
            "&2&l📈 Rankings", 
            "&7Voice time rank: &f#" + getPlayerRank(targetId, "voice"),
            "&7Chat rank: &f#" + getPlayerRank(targetId, "chat"),
            "&7Combined rank: &f#" + getPlayerRank(targetId, "combined"),
            "",
            "&7Your position among all players",
            "&7on the server leaderboards.",
            "",
            ColorUtil.translateColors("&e➤ Click to view leaderboards")));
            
        // Rewards Statistics
        gui.setItem(29, createItem(Material.CHEST, 
            "&c&l🎁 Rewards", 
            "&7Total rewards: &f" + stats.getTotalRewards(),
            "&7Recent rewards:",
            getRecentRewards(stats),
            "",
            "&7All the rewards you've earned",
            "&7through voice channel participation!",
            "",
            ColorUtil.translateColors("&e➤ Click for reward history")));
        
        // Navigation buttons
        gui.setItem(45, createItem(Material.ARROW, 
            "&c&l← Back", 
            "&7Return to main menu",
            "",
            "&c➤ Click to go back"));
            
        gui.setItem(53, createItem(Material.COMPASS, 
            "&b&l🔄 Refresh Stats", 
            "&7Update all statistics with",
            "&7the latest data from the server.",
            "",
            "&b➤ Click to refresh"));
        
        openGuis.put(player.getUniqueId(), "statistics:" + target.getName());
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
    }
    
    /**
     * Get recent achievements text
     */
    private String getRecentAchievements(Set<String> achievements) {
        if (achievements.isEmpty()) {
            return ColorUtil.translateColors("&7None yet - keep playing!");
        }
        
        // This is simplified - would need better recent achievement tracking
        int count = Math.min(achievements.size(), 3);
        return ColorUtil.translateColors("&f" + count + " &7recent unlocks");
    }
    
    /**
     * Get recent rewards text
     */
    private String getRecentRewards(StatisticsManager.PlayerStatistics stats) {
        if (stats.getRecentRewards().isEmpty()) {
            return ColorUtil.translateColors("&7None yet - keep participating!");
        }
        
        return ColorUtil.translateColors("&f" + stats.getRecentRewards().size() + " &7recent rewards");
    }
    
    /**
     * Get player rank for a category
     */
    private int getPlayerRank(UUID playerId, String category) {
        // This would need proper ranking calculation
        return 1; // Placeholder
    }
    
    /**
     * Format a timestamp to readable date
     */
    private String formatDate(long timestamp) {
        if (timestamp == 0) return "Unknown";
        
        java.time.LocalDateTime date = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp), 
            java.time.ZoneId.systemDefault());
            
        return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }
    
    /**
     * Format large numbers with commas
     */
    private String formatNumber(int number) {
        return String.format("%,d", number);
    }
    
    /**
     * Add decorative border to statistics GUI
     */
    private void addStatsBorder(Inventory gui) {
        ItemStack border = createItem(Material.CYAN_STAINED_GLASS_PANE, " ", "");
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
            gui.setItem(i + 45, border);
        }
        
        // Side columns
        for (int i = 1; i < 5; i++) {
            gui.setItem(i * 9, border);
            gui.setItem(i * 9 + 8, border);
        }
    }
    
    /**
     * Handle GUI click events
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String guiInfo = openGuis.get(player.getUniqueId());
        
        if (guiInfo == null || !guiInfo.startsWith("statistics:")) return;
        
        event.setCancelled(true); // Prevent item taking
        
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }
        
        ItemStack item = event.getCurrentItem();
        if (item.getItemMeta() == null) return;
        
        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        
        // Handle navigation
        if (displayName.contains("Back")) {
            player.closeInventory();
            openGuis.remove(player.getUniqueId());
            // Could open help menu or previous menu here
        } else if (displayName.contains("Refresh Stats")) {
            // Refresh the statistics display
            String targetName = guiInfo.split(":")[1];
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                openStatisticsMenu(player, target);
            }
        } else if (displayName.contains("Achievements")) {
            player.sendMessage(ColorUtil.translateColors("&6🏆 Achievement system coming soon!"));
        } else if (displayName.contains("Rankings")) {
            player.performCommand("leaderboard voice gui");
            player.closeInventory();
        }
        
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
    
    /**
     * Prevent item dragging
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String guiInfo = openGuis.get(player.getUniqueId());
        if (guiInfo != null && guiInfo.startsWith("statistics:")) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Create an item with name and lore
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translateColors(name));
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore).stream()
                    .map(ColorUtil::translateColors)
                    .collect(java.util.stream.Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Clean up when player leaves
     */
    public void cleanupPlayer(Player player) {
        openGuis.remove(player.getUniqueId());
    }
}