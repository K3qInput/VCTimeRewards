package me.kiro.vctime.guis;

import me.kiro.vctime.VCTimeRewards;
import me.kiro.vctime.managers.TimeManager.PlayerTimeEntry;
import me.kiro.vctime.utils.ColorUtil;
import me.kiro.vctime.utils.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Modern and professional leaderboard GUI system
 * Features animated elements, interactive navigation, and beautiful design
 */
public class ModernLeaderboardGUI implements Listener {
    
    private final VCTimeRewards plugin;
    private final Map<UUID, String> openGuis = new HashMap<>();
    private final Map<UUID, Integer> currentPages = new HashMap<>();
    
    public ModernLeaderboardGUI(VCTimeRewards plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Open the main leaderboard selection menu
     */
    public void openLeaderboardSelection(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, ColorUtil.translateColors("&6&l✨ Leaderboard Center ✨"));
        
        // Decorative border with gradient effect
        addGradientBorder(gui);
        
        // Voice Time Leaderboard
        gui.setItem(11, createItem(Material.DIAMOND, 
            "&b&l🎤 Voice Time Rankings", 
            "&7View the top players ranked by",
            "&7total Discord voice channel time.",
            "",
            "&e📊 &fTop performers:",
            "&7• Amazing dedication rewards",
            "&7• Real-time ranking updates",
            "&7• Interactive player profiles",
            "",
            "&a➤ Click to view voice rankings"));
            
        // Chat Message Leaderboard
        gui.setItem(13, createItem(Material.WRITABLE_BOOK, 
            "&a&l💬 Chat Activity Rankings", 
            "&7See who's most active in Discord chat",
            "&7with message count leaderboards.",
            "",
            "&e📊 &fFeatures:",
            "&7• Discord message tracking",
            "&7• Daily and total statistics",
            "&7• Community engagement metrics",
            "",
            "&a➤ Click to view chat rankings"));
            
        // Combined Leaderboard
        gui.setItem(15, createItem(Material.NETHER_STAR, 
            "&d&l⭐ Combined Rankings", 
            "&7Ultimate leaderboard combining",
            "&7voice time and chat activity.",
            "",
            "&e📊 &fScoring system:",
            "&7• Voice minutes = points",
            "&7• Chat messages = points",
            "&7• Total combined score ranking",
            "",
            "&a➤ Click to view combined rankings"));
            
        // Competition Info
        gui.setItem(29, createItem(Material.GOLD_BLOCK, 
            "&6&l🏆 Competition Info", 
            "&7Learn about ongoing competitions",
            "&7and special events.",
            "",
            "&e🎯 &fCurrent Events:",
            "&7• Weekly voice time challenges",
            "&7• Monthly chat competitions",
            "&7• Special seasonal rewards",
            "",
            "&a➤ Click for competition details"));
            
        // Player Stats
        gui.setItem(31, createItem(Material.PLAYER_HEAD, 
            "&e&l📊 Your Statistics", 
            "&7View your detailed performance",
            "&7statistics and progress.",
            "",
            "&e📈 &fYour metrics:",
            "&7• Personal voice time data",
            "&7• Chat activity analysis",
            "&7• Ranking progression",
            "",
            "&a➤ Click to view your stats"));
            
        // Settings
        gui.setItem(33, createItem(Material.COMPARATOR, 
            "&3&l⚙️ Display Settings", 
            "&7Customize how leaderboards",
            "&7are displayed to you.",
            "",
            "&e🎨 &fOptions:",
            "&7• Animation preferences",
            "&7• Sound settings",
            "&7• Display format options",
            "",
            "&a➤ Click to customize"));
        
        // Info item
        gui.setItem(40, createItem(Material.BOOK, 
            "&f&l📖 Leaderboard Guide", 
            "&7Learn how rankings work and",
            "&7how to climb the leaderboards.",
            "",
            "&7Rankings update every 10 seconds",
            "&7for the most accurate results!"));
        
        openGuis.put(player.getUniqueId(), "selection");
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
    }
    
    /**
     * Open voice time leaderboard with pagination
     */
    public void openVoiceLeaderboard(Player player, int page) {
        currentPages.put(player.getUniqueId(), page);
        
        // Load data asynchronously then display on main thread
        plugin.getDataManager().getLeaderboardAsync(100).thenAccept(leaderboard -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                displayVoiceLeaderboard(player, leaderboard, page);
            });
        }).exceptionally(throwable -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(ColorUtil.translateColors("&cFailed to load leaderboard data."));
            });
            return null;
        });
    }
    
    /**
     * Display the voice leaderboard GUI
     */
    private void displayVoiceLeaderboard(Player player, List<PlayerTimeEntry> leaderboard, int page) {
        int itemsPerPage = 28; // 4 rows of 7 items
        int totalPages = (int) Math.ceil((double) leaderboard.size() / itemsPerPage);
        page = Math.max(1, Math.min(page, totalPages));
        
        String title = String.format("&b&l🎤 Voice Leaderboard &f(&7Page %d/%d&f)", page, Math.max(1, totalPages));
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.translateColors(title));
        
        // Add navigation and decoration
        addLeaderboardBorder(gui);
        addNavigationItems(gui, page, totalPages, "voice");
        
        // Add leaderboard entries
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, leaderboard.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            PlayerTimeEntry entry = leaderboard.get(i);
            int guiSlot = 10 + ((i - startIndex) % 7) + (((i - startIndex) / 7) * 9);
            
            if (guiSlot >= 10 && guiSlot <= 37 && guiSlot % 9 != 0 && guiSlot % 9 != 8) {
                ItemStack playerItem = createPlayerItem(entry, i + 1);
                gui.setItem(guiSlot, playerItem);
            }
        }
        
        // Add special items for top 3
        if (page == 1 && leaderboard.size() >= 3) {
            addTopThreeHighlights(gui, leaderboard);
        }
        
        openGuis.put(player.getUniqueId(), "voice");
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
    }
    
    /**
     * Open chat leaderboard
     */
    public void openChatLeaderboard(Player player, int page) {
        currentPages.put(player.getUniqueId(), page);
        
        // Get chat leaderboard data
        plugin.getChatManager().getChatLeaderboard(100).forEach(entry -> {
            // Process chat leaderboard (implementation similar to voice)
        });
        
        // For now, show a placeholder
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.translateColors("&a&l💬 Chat Leaderboard"));
        addLeaderboardBorder(gui);
        
        gui.setItem(22, createItem(Material.WRITABLE_BOOK, 
            "&a&l💬 Chat Leaderboard", 
            "&7Discord message activity rankings",
            "&7will be displayed here.",
            "",
            "&7This feature tracks Discord messages",
            "&7sent by linked players.",
            "",
            "&eCheck back soon for rankings!"));
            
        // Back button
        gui.setItem(45, createItem(Material.ARROW, 
            "&c&l← Back", 
            "&7Return to leaderboard selection",
            "",
            "&c➤ Click to go back"));
        
        openGuis.put(player.getUniqueId(), "chat");
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
    }
    
    /**
     * Create a player item for leaderboard display
     */
    private ItemStack createPlayerItem(PlayerTimeEntry entry, int rank) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            // Set player head
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getPlayerName());
            meta.setOwningPlayer(player);
            
            // Create display name with rank
            String rankDisplay = getRankDisplay(rank);
            meta.setDisplayName(ColorUtil.translateColors(rankDisplay + " &f" + entry.getPlayerName()));
            
            // Create detailed lore
            meta.setLore(Arrays.asList(
                ColorUtil.translateColors("&7▪ &eRank: &f#" + rank),
                ColorUtil.translateColors("&7▪ &bVoice Time: &f" + TimeFormatter.formatTime(entry.getTotalTime())),
                ColorUtil.translateColors("&7▪ &aDetailed: &f" + TimeFormatter.formatTimeDetailed(entry.getTotalTime())),
                "",
                ColorUtil.translateColors("&7▪ &dSessions: &f" + getSessionCount(entry.getPlayerName())),
                ColorUtil.translateColors("&7▪ &6Average: &f" + getAverageSession(entry.getPlayerName())),
                "",
                getRankBadge(rank),
                "",
                ColorUtil.translateColors("&e➤ Click for detailed stats")
            ));
            
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    /**
     * Get rank display with colors and emojis
     */
    private String getRankDisplay(int rank) {
        switch (rank) {
            case 1: return "&6&l👑 #1";
            case 2: return "&7&l🥈 #2";
            case 3: return "&c&l🥉 #3";
            default: 
                if (rank <= 10) return "&e&l⭐ #" + rank;
                else return "&7#" + rank;
        }
    }
    
    /**
     * Get rank badge description
     */
    private String getRankBadge(int rank) {
        if (rank == 1) return ColorUtil.translateColors("&6&l🏆 CHAMPION - Voice Time Leader!");
        else if (rank == 2) return ColorUtil.translateColors("&7&l🥈 LEGEND - Outstanding dedication!");
        else if (rank == 3) return ColorUtil.translateColors("&c&l🥉 EXPERT - Impressive commitment!");
        else if (rank <= 10) return ColorUtil.translateColors("&e&l⭐ TOP 10 - Elite performer!");
        else if (rank <= 25) return ColorUtil.translateColors("&a&l🌟 TOP 25 - Great participant!");
        else return ColorUtil.translateColors("&7Active community member");
    }
    
    /**
     * Add gradient border to GUI
     */
    private void addGradientBorder(Inventory gui) {
        // Top row - gradient from purple to blue
        ItemStack purple = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ", "");
        ItemStack magenta = createItem(Material.MAGENTA_STAINED_GLASS_PANE, " ", "");
        ItemStack blue = createItem(Material.BLUE_STAINED_GLASS_PANE, " ", "");
        
        gui.setItem(0, purple);
        gui.setItem(1, magenta);
        gui.setItem(2, blue);
        gui.setItem(3, blue);
        gui.setItem(4, blue);
        gui.setItem(5, blue);
        gui.setItem(6, blue);
        gui.setItem(7, magenta);
        gui.setItem(8, purple);
        
        // Bottom row - same gradient
        for (int i = 36; i < 45; i++) {
            gui.setItem(i, gui.getItem(i - 36));
        }
    }
    
    /**
     * Add leaderboard border
     */
    private void addLeaderboardBorder(Inventory gui) {
        ItemStack border = createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ", "");
        
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
     * Add navigation items (previous/next page, back button)
     */
    private void addNavigationItems(Inventory gui, int currentPage, int totalPages, String type) {
        // Previous page
        if (currentPage > 1) {
            gui.setItem(45, createItem(Material.ARROW, 
                "&a&l← Previous Page", 
                "&7Go to page " + (currentPage - 1),
                "",
                "&a➤ Click to go back"));
        }
        
        // Page info
        gui.setItem(49, createItem(Material.BOOK, 
            "&e&lPage " + currentPage + " of " + totalPages, 
            "&7Viewing leaderboard rankings",
            "",
            "&7Total entries: &e" + (totalPages * 28),
            "&7Updated every 10 seconds"));
        
        // Next page
        if (currentPage < totalPages) {
            gui.setItem(53, createItem(Material.ARROW, 
                "&a&lNext Page →", 
                "&7Go to page " + (currentPage + 1),
                "",
                "&a➤ Click to continue"));
        }
        
        // Back to selection
        gui.setItem(46, createItem(Material.BARRIER, 
            "&c&l← Back to Selection", 
            "&7Return to leaderboard menu",
            "",
            "&c➤ Click to go back"));
    }
    
    /**
     * Add special highlights for top 3 players
     */
    private void addTopThreeHighlights(Inventory gui, List<PlayerTimeEntry> leaderboard) {
        // Add special crown item for #1
        if (leaderboard.size() >= 1) {
            gui.setItem(4, createItem(Material.GOLDEN_APPLE, 
                "&6&l👑 CURRENT CHAMPION", 
                "&7" + leaderboard.get(0).getPlayerName(),
                "&7" + TimeFormatter.formatTime(leaderboard.get(0).getTotalTime()),
                "",
                "&6The ultimate voice time leader!"));
        }
    }
    
    /**
     * Handle GUI click events
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String guiType = openGuis.get(player.getUniqueId());
        
        if (guiType == null) return;
        
        event.setCancelled(true); // Prevent item taking
        
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }
        
        ItemStack item = event.getCurrentItem();
        if (item.getItemMeta() == null) return;
        
        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        
        // Handle navigation
        if (displayName.contains("Back")) {
            if (guiType.equals("selection")) {
                player.closeInventory();
                openGuis.remove(player.getUniqueId());
            } else {
                openLeaderboardSelection(player);
            }
            return;
        }
        
        if (displayName.contains("Previous Page")) {
            int currentPage = currentPages.getOrDefault(player.getUniqueId(), 1);
            if (guiType.equals("voice")) {
                openVoiceLeaderboard(player, currentPage - 1);
            } else if (guiType.equals("chat")) {
                openChatLeaderboard(player, currentPage - 1);
            }
            return;
        }
        
        if (displayName.contains("Next Page")) {
            int currentPage = currentPages.getOrDefault(player.getUniqueId(), 1);
            if (guiType.equals("voice")) {
                openVoiceLeaderboard(player, currentPage + 1);
            } else if (guiType.equals("chat")) {
                openChatLeaderboard(player, currentPage + 1);
            }
            return;
        }
        
        // Handle selection menu clicks
        if (guiType.equals("selection")) {
            if (displayName.contains("Voice Time Rankings")) {
                openVoiceLeaderboard(player, 1);
            } else if (displayName.contains("Chat Activity Rankings")) {
                openChatLeaderboard(player, 1);
            } else if (displayName.contains("Combined Rankings")) {
                // TODO: Implement combined leaderboard
                player.sendMessage(ColorUtil.translateColors("&dCombined leaderboard coming soon!"));
            } else if (displayName.contains("Your Statistics")) {
                player.performCommand("vctime");
                player.closeInventory();
            }
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
        if (openGuis.containsKey(player.getUniqueId())) {
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
     * Get session count for a player (placeholder)
     */
    private String getSessionCount(String playerName) {
        return "N/A"; // TODO: Implement session tracking
    }
    
    /**
     * Get average session time for a player (placeholder)
     */
    private String getAverageSession(String playerName) {
        return "N/A"; // TODO: Implement average calculation
    }
    
    /**
     * Clean up when player leaves
     */
    public void cleanupPlayer(Player player) {
        openGuis.remove(player.getUniqueId());
        currentPages.remove(player.getUniqueId());
    }
}