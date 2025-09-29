package me.kiro.vctime.commands;

import me.kiro.vctime.VCTimeRewards;
import me.kiro.vctime.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Professional Help Menu and Tutorial System
 * Provides interactive help system with modern GUI design
 */
public class HelpMenuCommand implements CommandExecutor, Listener {
    
    private final VCTimeRewards plugin;
    private final Map<UUID, String> openMenus = new HashMap<>();
    
    public HelpMenuCommand(VCTimeRewards plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.translateColors("&cThis command can only be used by players!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            openMainMenu(player);
        } else {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "menu":
                case "gui":
                    openMainMenu(player);
                    break;
                case "commands":
                    openCommandsMenu(player);
                    break;
                case "placeholders":
                    openPlaceholdersMenu(player);
                    break;
                case "rewards":
                    openRewardsMenu(player);
                    break;
                case "setup":
                    openSetupMenu(player);
                    break;
                default:
                    openMainMenu(player);
                    break;
            }
        }
        
        return true;
    }
    
    /**
     * Open the main help menu
     */
    private void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, ColorUtil.translateColors("&6&l✨ VCTimeRewards Help Center ✨"));
        
        // Add decorative border
        ItemStack border = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ", "");
        for (int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 36, 37, 38, 39, 40, 41, 42, 43, 44}) {
            gui.setItem(i, border);
        }
        
        // Main menu items
        gui.setItem(10, createItem(Material.BOOK, 
            "&e&l📚 Getting Started", 
            "&7Learn the basics of VCTimeRewards",
            "&7and how to earn rewards through",
            "&7Discord voice channel participation.",
            "",
            "&a➤ Click to learn more"));
            
        gui.setItem(12, createItem(Material.COMMAND_BLOCK, 
            "&b&l⌨️ Commands Guide", 
            "&7Complete list of all commands",
            "&7available to players and admins.",
            "&7Learn how to check your time,",
            "&7view leaderboards, and more.",
            "",
            "&a➤ Click to view commands"));
            
        gui.setItem(14, createItem(Material.DIAMOND, 
            "&d&l🎁 Rewards System", 
            "&7Discover all the amazing rewards",
            "&7you can earn for spending time",
            "&7in Discord voice channels.",
            "&7From items to special privileges!",
            "",
            "&a➤ Click to see rewards"));
            
        gui.setItem(16, createItem(Material.NAME_TAG, 
            "&6&l🔤 Placeholders", 
            "&7View all available PlaceholderAPI",
            "&7placeholders for use in scoreboards,",
            "&7holograms, and other plugins.",
            "",
            "&a➤ Click to browse placeholders"));
            
        gui.setItem(28, createItem(Material.WRITABLE_BOOK, 
            "&c&l⚙️ Setup Guide", 
            "&7Administrator guide for setting",
            "&7up VCTimeRewards on your server.",
            "&7Configuration, permissions, and",
            "&7troubleshooting tips.",
            "",
            "&a➤ Click for setup guide"));
            
        gui.setItem(30, createItem(Material.COMPASS, 
            "&3&l🌐 Discord Integration", 
            "&7Learn how to link your Discord",
            "&7and Minecraft accounts to start",
            "&7earning rewards for voice activity.",
            "",
            "&a➤ Click to learn linking"));
            
        gui.setItem(32, createItem(Material.EMERALD, 
            "&a&l📊 Statistics", 
            "&7View detailed information about",
            "&7your voice time, rankings, and",
            "&7progress towards rewards.",
            "",
            "&a➤ Click to view your stats"));
            
        gui.setItem(34, createItem(Material.BARRIER, 
            "&c&l❌ Close Menu", 
            "&7Close this help menu.",
            "",
            "&c➤ Click to close"));
        
        // Info item at bottom
        gui.setItem(40, createItem(Material.PAPER, 
            "&f&l📝 VCTimeRewards v1.0.0", 
            "&7Professional Discord Voice Rewards",
            "&7Plugin with advanced features.",
            "",
            "&eAuthor: &fKiro",
            "&eWebsite: &fBuiltByBit Marketplace"));
        
        openMenus.put(player.getUniqueId(), "main");
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
    
    /**
     * Open commands help menu
     */
    private void openCommandsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.translateColors("&b&l⌨️ Commands Guide"));
        
        addMenuBorder(gui, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        
        // Player commands
        gui.setItem(10, createItem(Material.PLAYER_HEAD, 
            "&e&l👤 Player Commands", 
            "&7Commands available to all players:",
            "",
            "&f/vctime &7- Check your voice time",
            "&f/vctime [player] &7- Check another player's time",
            "&f/vctime help &7- Open this help menu",
            "&f/leaderboard voice &7- Voice time rankings",
            "&f/leaderboard chat &7- Chat message rankings",
            "&f/leaderboard combined &7- Combined rankings",
            "&f/leaderboard voice gui &7- Interactive leaderboard",
            "",
            "&aThese commands are available to everyone!"));
            
        // Admin commands
        gui.setItem(12, createItem(Material.COMMAND_BLOCK, 
            "&c&l👑 Admin Commands", 
            "&7Commands for server administrators:",
            "",
            "&f/vctimeadmin status &7- Plugin status",
            "&f/vctimeadmin reload &7- Reload configuration",
            "&f/vctimeadmin save &7- Save all data",
            "&f/vctimeadmin check [player] &7- Check player details",
            "&f/vctimeadmin reset [player] &7- Reset player data",
            "&f/vctimeadmin list &7- List active players",
            "",
            "&cRequires admin permissions"));
            
        // Leaderboard commands
        gui.setItem(14, createItem(Material.GOLD_INGOT, 
            "&6&l🏆 Leaderboard Commands", 
            "&7View rankings and competitions:",
            "",
            "&f/leaderboard voice &7- Voice time top 10",
            "&f/leaderboard chat &7- Chat messages top 10",
            "&f/leaderboard combined &7- Combined score top 10",
            "&f/leaderboard top &7- Top performers announcement",
            "&f/lb voice gui &7- Interactive voice leaderboard",
            "&f/top chat gui &7- Interactive chat leaderboard",
            "",
            "&aAliases: /lb, /top, /rankings"));
            
        // Permission info
        gui.setItem(16, createItem(Material.PAPER, 
            "&d&l🔑 Permissions", 
            "&7Required permissions for commands:",
            "",
            "&fvctime.check &7- Use /vctime command",
            "&fvctime.admin &7- Use admin commands",
            "&fvctime.leaderboard &7- View leaderboards",
            "&fvctime.notifications &7- Receive notifications",
            "",
            "&7Most permissions default to true for players"));
        
        // Back button
        gui.setItem(45, createItem(Material.ARROW, 
            "&a&l← Back to Main Menu", 
            "&7Return to the main help menu.",
            "",
            "&a➤ Click to go back"));
            
        openMenus.put(player.getUniqueId(), "commands");
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
    
    /**
     * Open placeholders help menu
     */
    private void openPlaceholdersMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.translateColors("&6&l🔤 PlaceholderAPI Guide"));
        
        addMenuBorder(gui, Material.YELLOW_STAINED_GLASS_PANE);
        
        // Voice time placeholders
        gui.setItem(10, createItem(Material.CLOCK, 
            "&e&l🎤 Voice Time Placeholders", 
            "&7Display voice channel time data:",
            "",
            "&f%vctime_total% &7- Total time (5h 30m)",
            "&f%vctime_total_formatted% &7- Formatted time",
            "&f%vctime_session% &7- Current session time",
            "&f%vctime_today% &7- Time spent today",
            "&f%vctime_yesterday% &7- Time spent yesterday",
            "&f%vctime_this_week% &7- This week's time",
            "&f%vctime_this_month% &7- This month's time",
            "",
            "&aUse in scoreboards, holograms, etc."));
            
        // Status placeholders
        gui.setItem(12, createItem(Material.REDSTONE_LAMP, 
            "&b&l📊 Status Placeholders", 
            "&7Show current player activity:",
            "",
            "&f%vctime_status% &7- Current status",
            "&f%vctime_channel% &7- Current voice channel",
            "&f%vctime_last_seen% &7- Last seen timestamp",
            "&f%vctime_is_online% &7- true/false online status",
            "&f%vctime_is_tracking% &7- true/false tracking",
            "",
            "&aGreat for dynamic displays"));
            
        // Ranking placeholders
        gui.setItem(14, createItem(Material.GOLD_BLOCK, 
            "&6&l🏆 Ranking Placeholders", 
            "&7Display leaderboard information:",
            "",
            "&f%vctime_rank% &7- Player's rank (#3)",
            "&f%vctime_rank_suffix% &7- Rank with suffix (3rd)",
            "&f%vctime_top_1_name% &7- #1 player name",
            "&f%vctime_top_1_time% &7- #1 player time",
            "&f%vctime_top_2_name% &7- #2 player name",
            "&f%vctime_top_3_name% &7- #3 player name",
            "",
            "&aPerfect for competitive displays"));
            
        // Progress placeholders
        gui.setItem(16, createItem(Material.EXPERIENCE_BOTTLE, 
            "&d&l📈 Progress Placeholders", 
            "&7Show progress towards goals:",
            "",
            "&f%vctime_progress_1h% &7- Progress to next hour",
            "&f%vctime_progress_bar_1h% &7- Visual progress bar",
            "&f%vctime_until_next_hour% &7- Time until milestone",
            "&f%vctime_next_reward_time% &7- Next reward time",
            "&f%vctime_next_reward_desc% &7- Next reward info",
            "",
            "&aMotivate players with goals"));
            
        // Statistics placeholders
        gui.setItem(28, createItem(Material.BOOK, 
            "&3&l📊 Statistics Placeholders", 
            "&7Detailed player statistics:",
            "",
            "&f%vctime_sessions_today% &7- Sessions today",
            "&f%vctime_average_session% &7- Average session",
            "&f%vctime_longest_session% &7- Longest session",
            "&f%vctime_best_day% &7- Best day record",
            "&f%vctime_streak_days% &7- Current streak",
            "",
            "&aDetailed performance metrics"));
            
        // Chat placeholders
        gui.setItem(30, createItem(Material.WRITABLE_BOOK, 
            "&a&l💬 Chat Placeholders", 
            "&7Discord message activity:",
            "",
            "&f%vctime_messages% &7- Total messages sent",
            "&f%vctime_messages_today% &7- Messages today",
            "&f%vctime_messages_rank% &7- Chat ranking",
            "&f%vctime_combined_score% &7- Combined score",
            "&f%vctime_combined_rank% &7- Combined ranking",
            "",
            "&aTrack Discord chat activity"));
            
        // Usage examples
        gui.setItem(32, createItem(Material.PAPER, 
            "&f&l📝 Usage Examples", 
            "&7How to use placeholders:",
            "",
            "&eScoreboards: &7Add to scoreboard plugins",
            "&eHolograms: &7Use in holographic displays",
            "&eChat: &7Include in chat format",
            "&eSigns: &7Display on dynamic signs",
            "&eTitles: &7Show in title messages",
            "",
            "&7Format: &f%vctime_placeholder%"));
        
        // Back button
        gui.setItem(45, createItem(Material.ARROW, 
            "&a&l← Back to Main Menu", 
            "&7Return to the main help menu.",
            "",
            "&a➤ Click to go back"));
            
        openMenus.put(player.getUniqueId(), "placeholders");
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
    
    /**
     * Open rewards help menu
     */
    private void openRewardsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.translateColors("&d&l🎁 Rewards System"));
        
        addMenuBorder(gui, Material.PINK_STAINED_GLASS_PANE);
        
        // Voice rewards
        gui.setItem(10, createItem(Material.DIAMOND, 
            "&b&l🎤 Voice Channel Rewards", 
            "&7Earn rewards for time in voice channels:",
            "",
            "&f1 minute &7→ &e5 coal",
            "&f5 minutes &7→ &e3 iron ingots",
            "&f30 minutes &7→ &e10 iron ingots",
            "&f1 hour &7→ &e5 diamonds",
            "&f3 hours &7→ &e2 diamond blocks",
            "&f5 hours &7→ &e1 netherite ingot",
            "&f10 hours &7→ &e3 golden apples",
            "&f100 hours &7→ &e1 dragon egg",
            "",
            "&aRewards given automatically!"));
            
        // Chat rewards
        gui.setItem(12, createItem(Material.WRITABLE_BOOK, 
            "&a&l💬 Discord Chat Rewards", 
            "&7Earn rewards for Discord messages:",
            "",
            "&f10 messages &7→ &e3 bread",
            "&f50 messages &7→ &e5 apples",
            "&f100 messages &7→ &e1 golden apple",
            "&f500 messages &7→ &e2 diamonds",
            "&f1000 messages &7→ &e1 emerald block",
            "",
            "&7Link your Discord account to earn these!",
            "",
            "&aMessage rewards stack with voice time!"));
            
        // Boost rewards
        gui.setItem(14, createItem(Material.DRAGON_EGG, 
            "&d&l🚀 Server Boost Rewards", 
            "&7Special rewards for boosting the Discord:",
            "",
            "&fBoost the server &7→ &e3 netherite ingots",
            "",
            "&7Server boosts help the community grow",
            "&7and are rewarded with premium items!",
            "",
            "&dThank you for supporting the server!"));
            
        // How to earn
        gui.setItem(16, createItem(Material.COMPASS, 
            "&e&l📍 How to Earn Rewards", 
            "&7Step-by-step guide:",
            "",
            "&61. &fLink Discord and Minecraft accounts",
            "&62. &fJoin a Discord voice channel",
            "&63. &fStay connected (minimum 2 linked players)",
            "&64. &fEarn time-based rewards automatically",
            "&65. &fSend Discord messages for bonus rewards",
            "&66. &fBoost the server for premium rewards",
            "",
            "&aRewards are given instantly!"));
            
        // Requirements
        gui.setItem(28, createItem(Material.REDSTONE, 
            "&c&l⚠️ Requirements", 
            "&7What you need to earn rewards:",
            "",
            "&f✓ &7Discord account linked to Minecraft",
            "&f✓ &7Be in a tracked voice channel",
            "&f✓ &7Channel must have 2+ linked players",
            "&f✓ &7Channel must not be blacklisted",
            "",
            "&7Some rewards require you to be online,",
            "&7but time tracking works even offline!"));
            
        // Tracking info
        gui.setItem(30, createItem(Material.CLOCK, 
            "&6&l⏰ Time Tracking", 
            "&7How time tracking works:",
            "",
            "&f• &7Time tracked every 10 seconds",
            "&f• &7Works even when Minecraft offline",
            "&f• &7Requires minimum 2 linked players",
            "&f• &7Automatic reward distribution",
            "&f• &7Progress saved every 5 minutes",
            "",
            "&aVery accurate and reliable!"));
            
        // Tips
        gui.setItem(32, createItem(Material.TORCH, 
            "&3&l💡 Pro Tips", 
            "&7Maximize your reward earning:",
            "",
            "&f• &7Stay in voice during peak hours",
            "&f• &7Invite friends to meet minimum requirements",
            "&f• &7Send Discord messages while in voice",
            "&f• &7Check /vctime regularly for progress",
            "&f• &7Use GUI leaderboards to compete",
            "",
            "&aWork together with friends!"));
        
        // Back button
        gui.setItem(45, createItem(Material.ARROW, 
            "&a&l← Back to Main Menu", 
            "&7Return to the main help menu.",
            "",
            "&a➤ Click to go back"));
            
        openMenus.put(player.getUniqueId(), "rewards");
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
    
    /**
     * Open setup guide menu
     */
    private void openSetupMenu(Player player) {
        if (!player.hasPermission("vctime.admin")) {
            player.sendMessage(ColorUtil.translateColors("&cYou don't have permission to view the setup guide!"));
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.translateColors("&c&l⚙️ Administrator Setup Guide"));
        
        addMenuBorder(gui, Material.RED_STAINED_GLASS_PANE);
        
        // Installation
        gui.setItem(10, createItem(Material.CHEST, 
            "&e&l📦 Installation", 
            "&7Basic plugin installation steps:",
            "",
            "&f1. &7Download VCTimeRewards.jar",
            "&f2. &7Install DiscordSRV plugin first",
            "&f3. &7Place jar in plugins folder",
            "&f4. &7Restart the server",
            "&f5. &7Configure Discord bot permissions",
            "",
            "&aRequired: DiscordSRV, PlaceholderAPI (optional)"));
            
        // Configuration
        gui.setItem(12, createItem(Material.WRITABLE_BOOK, 
            "&b&l⚙️ Configuration", 
            "&7Key configuration settings:",
            "",
            "&ftracking-mode &7- whitelist/blacklist channels",
            "&fminimum-members &7- Minimum linked players",
            "&frequire-both-online &7- Online requirement",
            "&fcheck-interval &7- Monitoring frequency",
            "&frewards &7- Customize reward commands",
            "",
            "&7Edit config.yml and restart or reload"));
            
        // Discord setup
        gui.setItem(14, createItem(Material.COMPASS, 
            "&d&l🤖 Discord Bot Setup", 
            "&7Discord bot configuration:",
            "",
            "&f1. &7Configure DiscordSRV bot first",
            "&f2. &7Enable Developer Mode in Discord",
            "&f3. &7Copy voice channel IDs for config",
            "&f4. &7Set up account linking commands",
            "&f5. &7Test bot permissions and events",
            "",
            "&cBot needs message and voice channel access"));
            
        // Permissions
        gui.setItem(16, createItem(Material.PAPER, 
            "&6&l🔑 Permissions Setup", 
            "&7Configure plugin permissions:",
            "",
            "&fvctime.check &7- Basic time checking",
            "&fvctime.admin &7- Administrative commands",
            "&fvctime.leaderboard &7- View leaderboards",
            "&fvctime.notifications &7- Receive alerts",
            "",
            "&7Most permissions default to true"));
            
        // Troubleshooting
        gui.setItem(28, createItem(Material.REDSTONE_TORCH, 
            "&c&l🔧 Troubleshooting", 
            "&7Common issues and solutions:",
            "",
            "&fNo rewards? &7Check Discord linking",
            "&fNo tracking? &7Verify channel IDs",
            "&fBot errors? &7Check DiscordSRV config",
            "&fLag issues? &7Increase check-interval",
            "&fData loss? &7Check save-interval setting",
            "",
            "&7Enable debug-mode for detailed logs"));
            
        // Performance
        gui.setItem(30, createItem(Material.EMERALD, 
            "&a&l⚡ Performance Tips", 
            "&7Optimize plugin performance:",
            "",
            "&f• &7Use reasonable check intervals (10-15s)",
            "&f• &7Set appropriate save intervals (5-10min)",
            "&f• &7Limit tracked channels if possible",
            "&f• &7Monitor server logs for errors",
            "&f• &7Use database storage for large servers",
            "",
            "&aPlugin is designed for efficiency"));
            
        // Support
        gui.setItem(32, createItem(Material.BOOK, 
            "&3&l📞 Support & Updates", 
            "&7Getting help and staying updated:",
            "",
            "&fBuiltByBit: &7Check marketplace page",
            "&fDiscord: &7Join plugin support server",
            "&fLogs: &7Enable debug mode for issues",
            "&fUpdates: &7Check for new versions",
            "",
            "&7Include logs when reporting issues"));
        
        // Back button
        gui.setItem(45, createItem(Material.ARROW, 
            "&a&l← Back to Main Menu", 
            "&7Return to the main help menu.",
            "",
            "&a➤ Click to go back"));
            
        openMenus.put(player.getUniqueId(), "setup");
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
    
    /**
     * Handle inventory click events
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String menuType = openMenus.get(player.getUniqueId());
        
        if (menuType == null) return;
        
        event.setCancelled(true); // Prevent item taking
        
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }
        
        ItemStack item = event.getCurrentItem();
        if (item.getItemMeta() == null) return;
        
        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        
        // Handle navigation
        if (displayName.contains("Back to Main Menu")) {
            openMainMenu(player);
            return;
        }
        
        if (displayName.contains("Close Menu")) {
            player.closeInventory();
            openMenus.remove(player.getUniqueId());
            return;
        }
        
        // Handle main menu clicks
        if (menuType.equals("main")) {
            if (displayName.contains("Getting Started")) {
                player.sendMessage(ColorUtil.translateColors("&e&l📚 Getting Started with VCTimeRewards"));
                player.sendMessage(ColorUtil.translateColors("&f"));
                player.sendMessage(ColorUtil.translateColors("&71. Link your Discord account using DiscordSRV"));
                player.sendMessage(ColorUtil.translateColors("&72. Join a Discord voice channel with friends"));
                player.sendMessage(ColorUtil.translateColors("&73. Stay connected to earn automatic rewards"));
                player.sendMessage(ColorUtil.translateColors("&74. Check your progress with /vctime"));
                player.sendMessage(ColorUtil.translateColors("&75. Compete on leaderboards with /leaderboard"));
                player.closeInventory();
            } else if (displayName.contains("Commands Guide")) {
                openCommandsMenu(player);
            } else if (displayName.contains("Rewards System")) {
                openRewardsMenu(player);
            } else if (displayName.contains("Placeholders")) {
                openPlaceholdersMenu(player);
            } else if (displayName.contains("Setup Guide")) {
                openSetupMenu(player);
            } else if (displayName.contains("Discord Integration")) {
                player.sendMessage(ColorUtil.translateColors("&3&l🌐 Discord Integration Guide"));
                player.sendMessage(ColorUtil.translateColors("&f"));
                player.sendMessage(ColorUtil.translateColors("&7To link your accounts and start earning rewards:"));
                player.sendMessage(ColorUtil.translateColors("&f1. &7Join the Discord server"));
                player.sendMessage(ColorUtil.translateColors("&f2. &7Use the account linking command"));
                player.sendMessage(ColorUtil.translateColors("&f3. &7Follow the instructions from the bot"));
                player.sendMessage(ColorUtil.translateColors("&f4. &7Join a voice channel with other linked players"));
                player.sendMessage(ColorUtil.translateColors("&f5. &7Start earning rewards automatically!"));
                player.closeInventory();
            } else if (displayName.contains("Statistics")) {
                // Execute vctime command to show stats
                player.performCommand("vctime");
                player.closeInventory();
            }
        }
        
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
    
    /**
     * Prevent item dragging in help menus
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        if (openMenus.containsKey(player.getUniqueId())) {
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
     * Add a decorative border to the GUI
     */
    private void addMenuBorder(Inventory gui, Material borderMaterial) {
        ItemStack border = createItem(borderMaterial, " ", "");
        
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
     * Clean up when player leaves
     */
    public void cleanupPlayer(Player player) {
        openMenus.remove(player.getUniqueId());
    }
}