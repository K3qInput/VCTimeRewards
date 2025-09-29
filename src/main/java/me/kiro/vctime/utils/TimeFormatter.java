package me.kiro.vctime.utils;

/**
 * Utility class for formatting time values
 */
public class TimeFormatter {
    
    /**
     * Format time in minutes to a readable string (e.g., "1h 30m")
     */
    public static String formatTime(long minutes) {
        if (minutes < 1) {
            return "0m";
        }
        
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        
        if (hours > 0) {
            if (remainingMinutes > 0) {
                return hours + "h " + remainingMinutes + "m";
            } else {
                return hours + "h";
            }
        } else {
            return remainingMinutes + "m";
        }
    }
    
    /**
     * Format time with more detail including days
     */
    public static String formatTimeDetailed(long minutes) {
        if (minutes < 1) {
            return "0 minutes";
        }
        
        long days = minutes / (24 * 60);
        long hours = (minutes % (24 * 60)) / 60;
        long remainingMinutes = minutes % 60;
        
        StringBuilder result = new StringBuilder();
        
        if (days > 0) {
            result.append(days).append(days == 1 ? " day" : " days");
        }
        
        if (hours > 0) {
            if (result.length() > 0) result.append(", ");
            result.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        
        if (remainingMinutes > 0) {
            if (result.length() > 0) result.append(", ");
            result.append(remainingMinutes).append(remainingMinutes == 1 ? " minute" : " minutes");
        }
        
        return result.toString();
    }
    
    /**
     * Parse time string to minutes (e.g., "1h30m" -> 90)
     */
    public static long parseTimeToMinutes(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return 0;
        }
        
        timeString = timeString.toLowerCase().trim();
        long totalMinutes = 0;
        
        try {
            // Handle patterns like "1h30m", "90m", "2h", etc.
            if (timeString.contains("h")) {
                int hourIndex = timeString.indexOf("h");
                String hoursPart = timeString.substring(0, hourIndex);
                totalMinutes += Long.parseLong(hoursPart) * 60;
                
                // Check for minutes after hours
                String remainder = timeString.substring(hourIndex + 1);
                if (remainder.contains("m")) {
                    int minuteIndex = remainder.indexOf("m");
                    String minutesPart = remainder.substring(0, minuteIndex);
                    if (!minutesPart.isEmpty()) {
                        totalMinutes += Long.parseLong(minutesPart);
                    }
                }
            } else if (timeString.contains("m")) {
                int minuteIndex = timeString.indexOf("m");
                String minutesPart = timeString.substring(0, minuteIndex);
                totalMinutes = Long.parseLong(minutesPart);
            } else {
                // Assume it's just hours if no unit specified
                totalMinutes = Long.parseLong(timeString) * 60;
            }
        } catch (NumberFormatException e) {
            // Return 0 if parsing fails
            return 0;
        }
        
        return totalMinutes;
    }
}