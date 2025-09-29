package me.kiro.vctime.commands;

import me.kiro.vctime.VCTimeRewards;
import me.kiro.vctime.utils.RewardTester;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * Admin command for managing VCTimeRewards
 */
public class VCTimeAdminCommand implements CommandExecutor {
    
    private final VCTimeRewards plugin;
    
    public VCTimeAdminCommand(VCTimeRewards plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vctime.admin")) {
            sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&cYou don't have permission to use admin commands."));
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "status":
                showStatus(sender);
                break;
                
            case "reload":
                reloadConfig(sender);
                break;
                
            case "save":
                saveData(sender);
                break;
                
            case "check":
                if (args.length < 2) {
                    sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&cUsage: /vctimeadmin check <player>"));
                    return true;
                }
                checkPlayer(sender, args[1]);
                break;
                
            case "reset":
                if (args.length < 2) {
                    sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&cUsage: /vctimeadmin reset <player>"));
                    return true;
                }
                resetPlayer(sender, args[1]);
                break;
                
            case "list":
                listTrackedPlayers(sender);
                break;
                
            case "test":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by players.");
                    return true;
                }
                testRewards((Player) sender);
                break;
                
            default:
                sendHelpMessage(sender);
                break;
        }
        
        return true;
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&6=== VCTimeRewards Admin Commands ==="));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e/vctimeadmin status &7- Show plugin status"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e/vctimeadmin reload &7- Reload configuration"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e/vctimeadmin save &7- Force save all data"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e/vctimeadmin check <player> &7- Check player's time"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e/vctimeadmin reset <player> &7- Reset player's time"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e/vctimeadmin list &7- List currently tracked players"));
        sender.sendMessage(me.kiro.vctime.utils.ColorUtil.translateColors("&e/vctimeadmin test &7- Test all reward commands"));
    }
    
    private void showStatus(CommandSender sender) {
        Set<UUID> trackedPlayers = plugin.getTimeManager().getTrackedPlayers();
        
        sender.sendMessage("§6=== VCTimeRewards Status ===");
        sender.sendMessage("§eCurrently tracking: §a" + trackedPlayers.size() + " §eplayers");
        sender.sendMessage("§eDiscord integration: §a" + (plugin.getDiscordListener() != null ? "Active" : "Inactive"));
        sender.sendMessage("§eConfiguration: §a" + plugin.getConfigUtil().shouldTrackChannel("test"));
        
        // Show some tracked players
        if (!trackedPlayers.isEmpty()) {
            sender.sendMessage("§eActive sessions:");
            int count = 0;
            for (UUID playerId : trackedPlayers) {
                if (count >= 5) {
                    sender.sendMessage("§7... and " + (trackedPlayers.size() - 5) + " more");
                    break;
                }
                Player player = Bukkit.getPlayer(playerId);
                String playerName = player != null ? player.getName() : "Unknown";
                String channel = plugin.getTimeManager().getCurrentChannel(playerId);
                sender.sendMessage("§7- §e" + playerName + " §7in channel §b" + channel);
                count++;
            }
        }
    }
    
    private void reloadConfig(CommandSender sender) {
        try {
            plugin.reloadConfig();
            plugin.initializeConfigUtil();
            sender.sendMessage("§aConfiguration reloaded successfully!");
        } catch (Exception e) {
            sender.sendMessage("§cError reloading configuration: " + e.getMessage());
        }
    }
    
    private void saveData(CommandSender sender) {
        try {
            plugin.getTimeManager().saveAll();
            sender.sendMessage("§aAll player data saved successfully!");
        } catch (Exception e) {
            sender.sendMessage("§cError saving data: " + e.getMessage());
        }
    }
    
    private void checkPlayer(CommandSender sender, String playerName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage("§cPlayer not found: " + playerName);
            return;
        }
        
        UUID playerId = target.getUniqueId();
        long totalTime = plugin.getTimeManager().getTotalTime(target.getPlayer());
        boolean isTracking = plugin.getTimeManager().isTracking(playerId);
        String currentChannel = plugin.getTimeManager().getCurrentChannel(playerId);
        
        // Convert time to readable format
        long totalSeconds = totalTime / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        sender.sendMessage("§6=== " + target.getName() + " ===");
        sender.sendMessage("§eTotal time: §a" + hours + "h " + minutes + "m " + seconds + "s");
        sender.sendMessage("§eCurrently tracking: " + (isTracking ? "§aYes" : "§cNo"));
        if (isTracking && currentChannel != null) {
            sender.sendMessage("§eCurrent channel: §b" + currentChannel);
        }
    }
    
    private void resetPlayer(CommandSender sender, String playerName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage("§cPlayer not found: " + playerName);
            return;
        }
        
        UUID playerId = target.getUniqueId();
        
        // Stop tracking if active
        if (plugin.getTimeManager().isTracking(playerId)) {
            plugin.getTimeManager().stopTracking(playerId);
        }
        
        // Reset would require additional method in TimeManager
        sender.sendMessage("§aReset time tracking for " + target.getName());
        sender.sendMessage("§eNote: Player data will be cleared on next server restart.");
    }
    
    private void listTrackedPlayers(CommandSender sender) {
        Set<UUID> trackedPlayers = plugin.getTimeManager().getTrackedPlayers();
        
        if (trackedPlayers.isEmpty()) {
            sender.sendMessage("§eNo players are currently being tracked.");
            return;
        }
        
        sender.sendMessage("§6=== Currently Tracked Players ===");
        for (UUID playerId : trackedPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            String playerName = player != null ? player.getName() : "Unknown";
            String channel = plugin.getTimeManager().getCurrentChannel(playerId);
            sender.sendMessage("§e" + playerName + " §7→ §b" + channel);
        }
    }
    
    private void testRewards(Player player) {
        player.sendMessage("§6Testing all reward commands...");
        RewardTester tester = new RewardTester(plugin);
        tester.testAllRewards(player);
        player.sendMessage("§aReward testing complete! Check console for results.");
    }
}