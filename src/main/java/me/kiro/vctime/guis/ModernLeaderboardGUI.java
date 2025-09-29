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
        Inventory gui = Bukkit.createInventory(null, 45, ColorUtil.translateColors("&#1a1a1a&l⚡ &#3d5a80LEADERBOARDS &#1a1a1a&l⚡"));
        
        // Modern sleek border with dark theme
        addModernBorder(gui);
        
        // Voice Time Leaderboard - Modern Cyan/Blue theme
        gui.setItem(11, createItem(Material.CYAN_GLAZED_TERRACOTTA, 
            "&#00d9ff&l⚡ VOICE TIME", 
            "&#7f8c8d┃ &fView top voice contributors",
            "&#7f8c8d┃ &fRanked by total time",
            "",
            "&#00d9ff⚡ &fLive Rankings",
            "&#7f8c8d⬥ &fReal-time updates",
            "&#7f8c8d⬥ &fInteractive profiles",
            "",
            "&#00ff88➤ &fClick to Open"));
            
        // Chat Message Leaderboard - Modern Green theme
        gui.setItem(13, createItem(Material.LIME_GLAZED_TERRACOTTA, 
            "&#00ff88&l💬 CHAT ACTIVITY", 
            "&#7f8c8d┃ &fMost active Discord chatters",
            "&#7f8c8d┃ &fMessage count rankings",
            "",
            "&#00ff88💬 &fCommunity Stats",
            "&#7f8c8d⬥ &fDaily tracking",
            "&#7f8c8d⬥ &fTotal messages",
            "",
            "&#00ff88➤ &fClick to Open"));
            
        // Combined Leaderboard - Modern Purple theme
        gui.setItem(15, createItem(Material.AMETHYST_SHARD, 
            "&#b24bf3&l⭐ COMBINED", 
            "&#7f8c8d┃ &fUltimate rankings",
            "&#7f8c8d┃ &fVoice + Chat combined",
            "",
            "&#b24bf3⭐ &fTotal Score",
            "&#7f8c8d⬥ &fVoice minutes",
            "&#7f8c8d⬥ &fChat messages",
            "",
            "&#00ff88➤ &fClick to Open"));
            
        // Competition Info - Modern Gold theme
        gui.setItem(29, createItem(Material.GOLD_INGOT, 
            "&#ffd700&l🏆 EVENTS", 
            "&#7f8c8d┃ &fOngoing competitions",
            "&#7f8c8d┃ &fSpecial challenges",
            "",
            "&#ffd700🏆 &fActive Now",
            "&#7f8c8d⬥ &fWeekly challenges",
            "&#7f8c8d⬥ &fMonthly contests",
            "",
            "&#00ff88➤ &fClick to View"));
            
        // Player Stats - Modern Orange theme
        gui.setItem(31, createItem(Material.PLAYER_HEAD, 
            "&#ff6b35&l📊 YOUR STATS", 
            "&#7f8c8d┃ &fPersonal performance",
            "&#7f8c8d┃ &fDetailed analytics",
            "",
            "&#ff6b35📊 &fYour Data",
            "&#7f8c8d⬥ &fVoice time",
            "&#7f8c8d⬥ &fChat activity",
            "",
            "&#00ff88➤ &fClick to View"));
            
        // Settings - Modern Teal theme
        gui.setItem(33, createItem(Material.REPEATER, 
            "&#00ffff&l⚙ SETTINGS", 
            "&#7f8c8d┃ &fCustomize display",
            "&#7f8c8d┃ &fPreferences",
            "",
            "&#00ffff⚙ &fOptions",
            "&#7f8c8d⬥ &fAnimations",
            "&#7f8c8d⬥ &fSound effects",
            "",
            "&#00ff88➤ &fClick to Edit"));
        
        // Info item - Modern White theme
        gui.setItem(40, createItem(Material.ENCHANTED_BOOK, 
            "&#ffffff&l📖 INFO", 
            "&#7f8c8d┃ &fHow rankings work",
            "&#7f8c8d┃ &fUpdates every 10s",
            "",
            "&#7f8c8d⬥ &fLive tracking",
            "&#7f8c8d⬥ &fAccurate results"));
        
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
        
        String title = String.format("&#1a1a1a&l⚡ &#00d9ffVOICE TIME &#1a1a1a&l⚡ &#7f8c8d(%d/%d)", page, Math.max(1, totalPages));
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
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.translateColors("&#1a1a1a&l⚡ &#00ff88CHAT ACTIVITY &#1a1a1a&l⚡"));
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
            case 1: return "&#ffd700&l👑 #1";
            case 2: return "&#c0c0c0&l🥈 #2";
            case 3: return "&#cd7f32&l🥉 #3";
            default: 
                if (rank <= 10) return "&#00d9ff&l⚡ #" + rank;
                else return "&#7f8c8d#" + rank;
        }
    }
    
    /**
     * Get rank badge description
     */
    private String getRankBadge(int rank) {
        if (rank == 1) return ColorUtil.translateColors("&#ffd700&l👑 CHAMPION - Ultimate Leader!");
        else if (rank == 2) return ColorUtil.translateColors("&#c0c0c0&l🥈 LEGEND - Elite Status!");
        else if (rank == 3) return ColorUtil.translateColors("&#cd7f32&l🥉 MASTER - Top Tier!");
        else if (rank <= 10) return ColorUtil.translateColors("&#00d9ff&l⚡ TOP 10 - Rising Star!");
        else if (rank <= 25) return ColorUtil.translateColors("&#00ff88&l🌟 TOP 25 - Active Member!");
        else return ColorUtil.translateColors("&#7f8c8d⭐ Community Member");
    }
    
    /**
     * Add modern sleek border to GUI with dark theme
     */
    private void addModernBorder(Inventory gui) {
        // Sleek black and cyan border for modern look
        ItemStack black = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        ItemStack cyan = createItem(Material.CYAN_STAINED_GLASS_PANE, " ", "");
        ItemStack gray = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", "");
        
        // Top row - modern pattern: black, cyan accents
        gui.setItem(0, cyan);
        gui.setItem(1, black);
        gui.setItem(2, black);
        gui.setItem(3, gray);
        gui.setItem(4, black);
        gui.setItem(5, gray);
        gui.setItem(6, black);
        gui.setItem(7, black);
        gui.setItem(8, cyan);
        
        // Bottom row - sleek symmetrical pattern
        gui.setItem(36, cyan);
        gui.setItem(37, black);
        gui.setItem(38, black);
        gui.setItem(39, gray);
        gui.setItem(40, black);
        gui.setItem(41, gray);
        gui.setItem(42, black);
        gui.setItem(43, black);
        gui.setItem(44, cyan);
    }
    
    /**
     * Add modern leaderboard border with dark theme
     */
    private void addLeaderboardBorder(Inventory gui) {
        ItemStack black = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        ItemStack cyan = createItem(Material.CYAN_STAINED_GLASS_PANE, " ", "");
        
        // Top and bottom rows - modern black with cyan accents
        for (int i = 0; i < 9; i++) {
            if (i == 0 || i == 4 || i == 8) {
                gui.setItem(i, cyan);
                gui.setItem(i + 45, cyan);
            } else {
                gui.setItem(i, black);
                gui.setItem(i + 45, black);
            }
        }
        
        // Side columns - sleek black borders
        for (int i = 1; i < 5; i++) {
            gui.setItem(i * 9, black);
            gui.setItem(i * 9 + 8, black);
        }
    }
    
    /**
     * Add navigation items (previous/next page, back button)
     */
    private void addNavigationItems(Inventory gui, int currentPage, int totalPages, String type) {
        // Previous page - Modern cyan theme
        if (currentPage > 1) {
            gui.setItem(45, createItem(Material.SPECTRAL_ARROW, 
                "&#00d9ff&l◀ PREVIOUS", 
                "&#7f8c8d┃ &fPage " + (currentPage - 1),
                "",
                "&#00ff88➤ &fClick to Go Back"));
        }
        
        // Page info - Modern white theme
        gui.setItem(49, createItem(Material.ENCHANTED_BOOK, 
            "&#ffffff&lPAGE " + currentPage + "/" + totalPages, 
            "&#7f8c8d┃ &fLive Rankings",
            "",
            "&#7f8c8d⬥ &fUpdates every 10s",
            "&#7f8c8d⬥ &fTotal: " + (totalPages * 28) + " players"));
        
        // Next page - Modern cyan theme
        if (currentPage < totalPages) {
            gui.setItem(53, createItem(Material.SPECTRAL_ARROW, 
                "&#00d9ff&lNEXT ▶", 
                "&#7f8c8d┃ &fPage " + (currentPage + 1),
                "",
                "&#00ff88➤ &fClick to Continue"));
        }
        
        // Back to selection - Modern red theme
        gui.setItem(46, createItem(Material.BARRIER, 
            "&#ff0000&l← BACK", 
            "&#7f8c8d┃ &fReturn to menu",
            "",
            "&#ff0000➤ &fClick to Go Back"));
    }
    
    /**
     * Add special highlights for top 3 players
     */
    private void addTopThreeHighlights(Inventory gui, List<PlayerTimeEntry> leaderboard) {
        // Add special crown item for #1 - Modern gold theme
        if (leaderboard.size() >= 1) {
            gui.setItem(4, createItem(Material.ENCHANTED_GOLDEN_APPLE, 
                "&#ffd700&l👑 CHAMPION", 
                "&#ffffff" + leaderboard.get(0).getPlayerName(),
                "&#00d9ff" + TimeFormatter.formatTime(leaderboard.get(0).getTotalTime()),
                "",
                "&#ffd700⭐ &fCurrent Leader!"));
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