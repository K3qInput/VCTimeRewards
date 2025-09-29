package me.kiro.vctime.commands;

import me.kiro.vctime.VCTimeRewards;
import me.kiro.vctime.managers.TimeManager.PlayerTimeEntry;
import me.kiro.vctime.utils.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Advanced leaderboard command with multiple display modes
 * Provides text-based and GUI leaderboards for various categories
 */
public class LeaderboardCommand implements CommandExecutor {
    
    private final VCTimeRewards plugin;
    
    public LeaderboardCommand(VCTimeRewards plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Execute on main thread to ensure thread safety
        try {
            handleLeaderboardCommand(sender, args);
        } catch (Exception e) {
            plugin.getErrorHandler().handleException("LeaderboardCommand", e, () -> {
                sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&cAn error occurred while loading the leaderboard."));
                return null;
            });
        }
        
        return true;
    }
    
    private void handleLeaderboardCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showLeaderboardHelp(sender);
            return;
        }
        
        String type = args[0].toLowerCase();
        boolean isGui = args.length > 1 && args[1].equalsIgnoreCase("gui");
        
        switch (type) {
            case "voice":
            case "time":
                showVoiceTimeLeaderboard(sender, isGui);
                break;
            case "chat":
            case "messages":
                showChatLeaderboard(sender, isGui);
                break;
            case "combined":
            case "total":
                showCombinedLeaderboard(sender, isGui);
                break;
            case "top":
                showTopPlayersAnnouncement(sender);
                break;
            default:
                showLeaderboardHelp(sender);
                break;
        }
    }
    
    /**
     * Show voice time leaderboard
     */
    private void showVoiceTimeLeaderboard(CommandSender sender, boolean isGui) {
        if (isGui && sender instanceof Player) {
            // Fetch data async, then display on main thread
            plugin.getDataManager().getLeaderboardAsync(45).thenAccept(leaderboard -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        showVoiceTimeGui((Player) sender, leaderboard);
                    } catch (Exception e) {
                        plugin.getErrorHandler().handleException("LeaderboardGUI", e, () -> {
                            sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&cError displaying leaderboard GUI."));
                            return null;
                        });
                    }
                });
            }).exceptionally(throwable -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&cFailed to load leaderboard data."));
                });
                return null;
            });
        } else {
            // Fetch data async, then display on main thread
            plugin.getDataManager().getLeaderboardAsync(10).thenAccept(leaderboard -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        showVoiceTimeText(sender, leaderboard);
                    } catch (Exception e) {
                        plugin.getErrorHandler().handleException("LeaderboardText", e, () -> {
                            sender.sendMessage(ChatColor.RED + "Error displaying leaderboard.");
                            return null;
                        });
                    }
                });
            }).exceptionally(throwable -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&cFailed to load leaderboard data."));
                });
                return null;
            });
        }
    }
    
    /**
     * Show chat message leaderboard
     */
    private void showChatLeaderboard(CommandSender sender, boolean isGui) {
        // Get top chat participants async, then display on main thread
        CompletableFuture<List<ChatLeaderEntry>> chatLeaderboard = getChatLeaderboard(10);
        
        if (isGui && sender instanceof Player) {
            chatLeaderboard.thenAccept(leaderboard -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        showChatGui((Player) sender, leaderboard);
                    } catch (Exception e) {
                        plugin.getErrorHandler().handleException("ChatLeaderboardGUI", e, () -> {
                            sender.sendMessage(ChatColor.RED + "Error displaying chat leaderboard GUI.");
                            return null;
                        });
                    }
                });
            }).exceptionally(throwable -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "Failed to load chat leaderboard data.");
                });
                return null;
            });
        } else {
            chatLeaderboard.thenAccept(leaderboard -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        showChatText(sender, leaderboard);
                    } catch (Exception e) {
                        plugin.getErrorHandler().handleException("ChatLeaderboardText", e, () -> {
                            sender.sendMessage(ChatColor.RED + "Error displaying chat leaderboard.");
                            return null;
                        });
                    }
                });
            }).exceptionally(throwable -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "Failed to load chat leaderboard data.");
                });
                return null;
            });
        }
    }
    
    /**
     * Show combined score leaderboard
     */
    private void showCombinedLeaderboard(CommandSender sender, boolean isGui) {
        CompletableFuture<List<CombinedLeaderEntry>> combinedLeaderboard = getCombinedLeaderboard(10);
        
        if (isGui && sender instanceof Player) {
            combinedLeaderboard.thenAccept(leaderboard -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        showCombinedGui((Player) sender, leaderboard);
                    } catch (Exception e) {
                        plugin.getErrorHandler().handleException("CombinedLeaderboardGUI", e, () -> {
                            sender.sendMessage(ChatColor.RED + "Error displaying combined leaderboard GUI.");
                            return null;
                        });
                    }
                });
            }).exceptionally(throwable -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "Failed to load combined leaderboard data.");
                });
                return null;
            });
        } else {
            combinedLeaderboard.thenAccept(leaderboard -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        showCombinedText(sender, leaderboard);
                    } catch (Exception e) {
                        plugin.getErrorHandler().handleException("CombinedLeaderboardText", e, () -> {
                            sender.sendMessage(ChatColor.RED + "Error displaying combined leaderboard.");
                            return null;
                        });
                    }
                });
            }).exceptionally(throwable -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "Failed to load combined leaderboard data.");
                });
                return null;
            });
        }
    }
    
    /**
     * Show voice time leaderboard in text format
     */
    private void showVoiceTimeText(CommandSender sender, List<PlayerTimeEntry> leaderboard) {
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&6═══════════════════════════════════"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e🎤 &6&lVOICE TIME LEADERBOARD"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&6═══════════════════════════════════"));
        
        if (leaderboard.isEmpty()) {
            sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&7No data available yet."));
            return;
        }
        
        for (int i = 0; i < leaderboard.size(); i++) {
            PlayerTimeEntry entry = leaderboard.get(i);
            String position = getPositionFormat(i + 1);
            String timeFormatted = TimeFormatter.formatTime(entry.getTotalTime());
            
            sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors(String.format("%s &f%s &7- &a%s",
                    position,
                    entry.getPlayerName(),
                    timeFormatted)));
        }
        
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&6═══════════════════════════════════"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&7Use &e/leaderboard voice gui &7for interactive view"));
    }
    
    /**
     * Show voice time leaderboard in GUI format
     */
    private void showVoiceTimeGui(Player player, List<PlayerTimeEntry> leaderboard) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.GOLD + "🎤 Voice Time Leaderboard");
        
        // Add decoration
        addGuiDecoration(gui);
        
        // Add leaderboard entries
        for (int i = 0; i < Math.min(leaderboard.size(), 45); i++) {
            PlayerTimeEntry entry = leaderboard.get(i);
            
            ItemStack playerHead = createPlayerHead(entry.getPlayerName());
            ItemMeta meta = playerHead.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName(getPositionFormat(i + 1) + " " + ChatColor.WHITE + entry.getPlayerName());
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Voice Time: " + ChatColor.GREEN + TimeFormatter.formatTime(entry.getTotalTime()),
                        ChatColor.GRAY + "Detailed: " + ChatColor.YELLOW + TimeFormatter.formatTimeDetailed(entry.getTotalTime()),
                        "",
                        ChatColor.GRAY + "Rank: " + ChatColor.GOLD + "#" + (i + 1)
                ));
                playerHead.setItemMeta(meta);
            }
            
            gui.setItem(i, playerHead);
        }
        
        player.openInventory(gui);
        plugin.getEnhancedLogger().userAction(player, "VIEW_VOICE_LEADERBOARD_GUI", "Opened voice time leaderboard GUI");
    }
    
    /**
     * Show chat leaderboard in text format
     */
    private void showChatText(CommandSender sender, List<ChatLeaderEntry> leaderboard) {
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&9═══════════════════════════════════"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&b💬 &9&lCHAT MESSAGES LEADERBOARD"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&9═══════════════════════════════════"));
        
        if (leaderboard.isEmpty()) {
            sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&7No data available yet."));
            return;
        }
        
        for (int i = 0; i < leaderboard.size(); i++) {
            ChatLeaderEntry entry = leaderboard.get(i);
            String position = getPositionFormat(i + 1);
            
            sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors(String.format("%s &f%s &7- &b%s messages",
                    position,
                    entry.playerName,
                    formatNumber(entry.messageCount))));
        }
        
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&9═══════════════════════════════════"));
    }
    
    /**
     * Show combined leaderboard in text format
     */
    private void showCombinedText(CommandSender sender, List<CombinedLeaderEntry> leaderboard) {
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&d═══════════════════════════════════"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&d⭐ &lCOMBINED SCORE LEADERBOARD"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&d═══════════════════════════════════"));
        
        if (leaderboard.isEmpty()) {
            sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&7No data available yet."));
            return;
        }
        
        for (int i = 0; i < leaderboard.size(); i++) {
            CombinedLeaderEntry entry = leaderboard.get(i);
            String position = getPositionFormat(i + 1);
            
            sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors(String.format("%s &f%s &7- &d%s points",
                    position,
                    entry.playerName,
                    formatNumber(entry.combinedScore))));
            
            sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors(String.format("   &7Voice: &a%s &7| Messages: &b%s",
                             TimeFormatter.formatTime(entry.voiceTime),
                             formatNumber(entry.messageCount))));
        }
        
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&d═══════════════════════════════════"));
    }
    
    /**
     * Show top players announcement
     */
    private void showTopPlayersAnnouncement(CommandSender sender) {
        plugin.getDataManager().getLeaderboardAsync(3).thenAccept(voiceLeaderboard -> {
            getChatLeaderboard(3).thenAccept(chatLeaderboard -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    announceTopPlayers(sender, voiceLeaderboard, chatLeaderboard);
                });
            });
        });
    }
    
    /**
     * Announce top players in all categories
     */
    private void announceTopPlayers(CommandSender sender, List<PlayerTimeEntry> voiceTop, List<ChatLeaderEntry> chatTop) {
        sender.sendMessage("");
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&6🏆 &e&lTOP PERFORMERS &6🏆"));
        sender.sendMessage("");
        
        // Voice time top 3
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e🎤 Voice Time Champions:"));
        for (int i = 0; i < Math.min(3, voiceTop.size()); i++) {
            PlayerTimeEntry entry = voiceTop.get(i);
            String medal = getMedal(i + 1);
            sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors(String.format("  %s &f%s &7- &a%s",
                    medal,
                    entry.getPlayerName(),
                    TimeFormatter.formatTime(entry.getTotalTime()))));
        }
        
        sender.sendMessage("");
        
        // Chat messages top 3
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&b💬 Chat Champions:"));
        for (int i = 0; i < Math.min(3, chatTop.size()); i++) {
            ChatLeaderEntry entry = chatTop.get(i);
            String medal = getMedal(i + 1);
            sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors(String.format("  %s &f%s &7- &b%s messages",
                    medal,
                    entry.playerName,
                    formatNumber(entry.messageCount))));
        }
        
        sender.sendMessage("");
        plugin.getEnhancedLogger().userAction((Player) sender, "VIEW_TOP_ANNOUNCEMENT", "Viewed top players announcement");
    }
    
    /**
     * Get chat leaderboard data
     */
    private CompletableFuture<List<ChatLeaderEntry>> getChatLeaderboard(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            // This would be implemented with actual chat data from ChatManager
            // For now, return a placeholder implementation
            return Arrays.asList(
                    new ChatLeaderEntry("TopChatter1", 1500),
                    new ChatLeaderEntry("ChatMaster", 1200),
                    new ChatLeaderEntry("TalkativePlayer", 950)
            );
        });
    }
    
    /**
     * Get combined leaderboard data
     */
    private CompletableFuture<List<CombinedLeaderEntry>> getCombinedLeaderboard(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            // Calculate combined scores (voice time in minutes + message count)
            // This would be implemented with actual data
            return Arrays.asList(
                    new CombinedLeaderEntry("SuperActive", 2500, 300, 800),
                    new CombinedLeaderEntry("VoiceAndChat", 2200, 250, 650),
                    new CombinedLeaderEntry("AllRounder", 1900, 200, 500)
            );
        });
    }
    
    /**
     * Show leaderboard command help
     */
    private void showLeaderboardHelp(CommandSender sender) {
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&6═══════════════════════════════════"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e📊 &6&lLEADERBOARD COMMANDS"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&6═══════════════════════════════════"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e/leaderboard voice [gui] &7- Voice time rankings"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e/leaderboard chat [gui] &7- Chat message rankings"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e/leaderboard combined [gui] &7- Combined score rankings"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e/leaderboard top &7- Announce top performers"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&6═══════════════════════════════════"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&7Add 'gui' for interactive leaderboard!"));
    }
    
    /**
     * Create player head item
     */
    private ItemStack createPlayerHead(String playerName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    /**
     * Add decorative items to GUI
     */
    private void addGuiDecoration(Inventory gui) {
        ItemStack border = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        
        // Add border
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, border);
        }
        
        // Add info item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.GOLD + "📊 Leaderboard Info");
            infoMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "This shows the top players",
                    ChatColor.GRAY + "ranked by voice channel time.",
                    "",
                    ChatColor.YELLOW + "Keep participating to climb",
                    ChatColor.YELLOW + "the rankings!"
            ));
            info.setItemMeta(infoMeta);
        }
        gui.setItem(49, info);
    }
    
    /**
     * Get position formatting with colors and symbols
     */
    private String getPositionFormat(int position) {
        switch (position) {
            case 1:
                return ChatColor.GOLD + "🥇 #1";
            case 2:
                return ChatColor.GRAY + "🥈 #2";
            case 3:
                return ChatColor.YELLOW + "🥉 #3";
            default:
                return ChatColor.WHITE + "#" + position;
        }
    }
    
    /**
     * Get medal emoji for position
     */
    private String getMedal(int position) {
        switch (position) {
            case 1: return "🥇";
            case 2: return "🥈";
            case 3: return "🥉";
            default: return "🏅";
        }
    }
    
    /**
     * Format numbers with commas
     */
    private String formatNumber(int number) {
        return String.format("%,d", number);
    }
    
    // Helper classes for leaderboard entries
    private static class ChatLeaderEntry {
        final String playerName;
        final int messageCount;
        
        ChatLeaderEntry(String playerName, int messageCount) {
            this.playerName = playerName;
            this.messageCount = messageCount;
        }
    }
    
    private static class CombinedLeaderEntry {
        final String playerName;
        final int combinedScore;
        final long voiceTime;
        final int messageCount;
        
        CombinedLeaderEntry(String playerName, int combinedScore, long voiceTime, int messageCount) {
            this.playerName = playerName;
            this.combinedScore = combinedScore;
            this.voiceTime = voiceTime;
            this.messageCount = messageCount;
        }
    }
    
    // GUI functionality would be handled by a separate listener class
    private void showChatGui(Player player, List<ChatLeaderEntry> leaderboard) {
        // Similar to voice GUI but for chat data
        player.sendMessage(ChatColor.AQUA + "Chat leaderboard GUI coming soon!");
    }
    
    private void showCombinedGui(Player player, List<CombinedLeaderEntry> leaderboard) {
        // Similar to voice GUI but for combined data
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Combined leaderboard GUI coming soon!");
    }
}