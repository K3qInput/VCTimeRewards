package me.kiro.vctime.utils;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling color codes including hex colors and legacy codes
 */
public class ColorUtil {
    
    // Pattern for hex color codes: #RRGGBB or &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_ALT = Pattern.compile("#([A-Fa-f0-9]{6})");
    
    /**
     * Translates color codes in a message to proper ChatColor formatting
     * Supports both legacy (&) codes and hex (#) codes
     * 
     * @param message The message to translate
     * @return The translated message with proper color formatting
     */
    public static String translateColors(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // First, handle hex color codes &#RRGGBB
        message = translateHexColors(message, HEX_PATTERN);
        
        // Also handle alternative hex format #RRGGBB  
        message = translateHexColors(message, HEX_PATTERN_ALT);
        
        // Then translate legacy color codes (&a, &6, etc.)
        message = ChatColor.translateAlternateColorCodes('&', message);
        
        return message;
    }
    
    /**
     * Translates hex color codes using the specified pattern
     */
    private static String translateHexColors(String message, Pattern pattern) {
        try {
            // Check if the server supports hex colors (1.16+)
            if (hasHexColorSupport()) {
                Matcher matcher = pattern.matcher(message);
                StringBuffer buffer = new StringBuffer();
                
                while (matcher.find()) {
                    String hexColor = matcher.group(1);
                    String replacement = convertHexToSpigotFormat(hexColor);
                    matcher.appendReplacement(buffer, replacement);
                }
                matcher.appendTail(buffer);
                
                return buffer.toString();
            } else {
                // Fallback: convert hex to nearest legacy color
                Matcher matcher = pattern.matcher(message);
                StringBuffer buffer = new StringBuffer();
                
                while (matcher.find()) {
                    String hexColor = matcher.group(1);
                    String replacement = convertHexToLegacyColor(hexColor);
                    matcher.appendReplacement(buffer, replacement);
                }
                matcher.appendTail(buffer);
                
                return buffer.toString();
            }
        } catch (Exception e) {
            // If hex color processing fails, just return the original message
            return message;
        }
    }
    
    /**
     * Check if the server supports hex colors (Minecraft 1.16+)
     */
    private static boolean hasHexColorSupport() {
        try {
            // Try to access net.md_5.bungee.api.ChatColor which supports hex colors
            Class.forName("net.md_5.bungee.api.ChatColor");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Convert hex color to Spigot hex format for 1.16+
     */
    private static String convertHexToSpigotFormat(String hex) {
        try {
            StringBuilder magic = new StringBuilder();
            magic.append(ChatColor.COLOR_CHAR).append("x");
            
            for (char c : hex.toCharArray()) {
                magic.append(ChatColor.COLOR_CHAR).append(c);
            }
            
            return magic.toString();
        } catch (Exception e) {
            return "§f"; // Default to white if conversion fails
        }
    }
    
    /**
     * Convert hex color to nearest legacy ChatColor for older versions
     */
    private static String convertHexToLegacyColor(String hex) {
        try {
            // Parse hex color
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            
            // Find closest legacy color
            return findClosestLegacyColor(r, g, b).toString();
        } catch (Exception e) {
            return ChatColor.WHITE.toString(); // Default fallback
        }
    }
    
    /**
     * Find the closest legacy ChatColor to RGB values
     */
    private static ChatColor findClosestLegacyColor(int r, int g, int b) {
        ChatColor closest = ChatColor.WHITE;
        double minDistance = Double.MAX_VALUE;
        
        // RGB values for legacy colors (approximate)
        int[][] legacyColors = {
            {0, 0, 0},        // BLACK
            {0, 0, 170},      // DARK_BLUE
            {0, 170, 0},      // DARK_GREEN
            {0, 170, 170},    // DARK_AQUA
            {170, 0, 0},      // DARK_RED
            {170, 0, 170},    // DARK_PURPLE
            {255, 170, 0},    // GOLD
            {170, 170, 170},  // GRAY
            {85, 85, 85},     // DARK_GRAY
            {85, 85, 255},    // BLUE
            {85, 255, 85},    // GREEN
            {85, 255, 255},   // AQUA
            {255, 85, 85},    // RED
            {255, 85, 255},   // LIGHT_PURPLE
            {255, 255, 85},   // YELLOW
            {255, 255, 255}   // WHITE
        };
        
        ChatColor[] colors = {
            ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
            ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
            ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
            ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
        };
        
        for (int i = 0; i < legacyColors.length; i++) {
            double distance = colorDistance(r, g, b, legacyColors[i][0], legacyColors[i][1], legacyColors[i][2]);
            if (distance < minDistance) {
                minDistance = distance;
                closest = colors[i];
            }
        }
        
        return closest;
    }
    
    /**
     * Calculate Euclidean distance between two RGB colors
     */
    private static double colorDistance(int r1, int g1, int b1, int r2, int g2, int b2) {
        return Math.sqrt(Math.pow(r2 - r1, 2) + Math.pow(g2 - g1, 2) + Math.pow(b2 - b1, 2));
    }
    
    /**
     * Remove all color codes from a message
     */
    public static String stripColors(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // Remove hex colors
        message = HEX_PATTERN.matcher(message).replaceAll("");
        message = HEX_PATTERN_ALT.matcher(message).replaceAll("");
        
        // Remove legacy colors
        return ChatColor.stripColor(message);
    }
    
    /**
     * Quick method to translate legacy color codes only (for compatibility)
     */
    public static String translateLegacyColors(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}