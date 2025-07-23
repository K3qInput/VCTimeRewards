package me.kiro.vctime.commands;

import me.kiro.vctime.VCTimeRewards;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to check voice channel time
 */
public class VCTimeCommand implements CommandExecutor {
    
    private final VCTimeRewards plugin;
    
    public VCTimeCommand(VCTimeRewards plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player has permission
        if (!player.hasPermission("vctime.check")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        // Get target player (self if no argument provided)
        Player target = player;
        if (args.length > 0) {
            if (!player.hasPermission("vctime.admin")) {
                player.sendMessage("§cYou don't have permission to check other players' time.");
                return true;
            }
            
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cPlayer not found: " + args[0]);
                return true;
            }
        }
        
        // Get total time for target player
        long totalTimeMs = plugin.getTimeManager().getTotalTime(target);
        
        // Convert to readable format
        long totalSeconds = totalTimeMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        // Send message
        String timeString = String.format("%dh %dm %ds", hours, minutes, seconds);
        if (target.equals(player)) {
            player.sendMessage("§aYour voice channel time: §f" + timeString);
        } else {
            player.sendMessage("§a" + target.getName() + "'s voice channel time: §f" + timeString);
        }
        
        return true;
    }
}