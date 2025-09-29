package me.kiro.vctime.utils;

import me.kiro.vctime.VCTimeRewards;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Enhanced logging utility with structured logging, error tracking, and performance insights
 * Provides comprehensive logging capabilities for debugging and monitoring
 */
public class EnhancedLogger {
    
    private final VCTimeRewards plugin;
    private final File logFile;
    private final File errorLogFile;
    private final SimpleDateFormat dateFormat;
    private final ScheduledExecutorService logExecutor;
    
    // Log buffer for async writing
    private final Queue<LogEntry> logBuffer;
    private final Map<String, Integer> errorCounts;
    private final Map<String, Long> lastErrorTimes;
    
    // Configuration
    private boolean enableDebugLogging;
    private boolean enableErrorTracking;
    private boolean enablePerformanceLogging;
    private boolean enableAsyncLogging;
    private int maxLogBufferSize = 1000;
    private long errorThrottleTime = 5000; // 5 seconds
    
    // Statistics
    private long totalLogEntries = 0;
    private long totalErrors = 0;
    private long totalWarnings = 0;
    private final Map<String, Long> categoryStats;
    
    public EnhancedLogger(VCTimeRewards plugin) {
        this.plugin = plugin;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        this.logExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Initialize collections
        this.logBuffer = new ConcurrentLinkedQueue<>();
        this.errorCounts = new ConcurrentHashMap<>();
        this.lastErrorTimes = new ConcurrentHashMap<>();
        this.categoryStats = new ConcurrentHashMap<>();
        
        // Setup log files
        File logDir = new File(plugin.getDataFolder(), "logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        
        String dateString = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        this.logFile = new File(logDir, "vctime-" + dateString + ".log");
        this.errorLogFile = new File(logDir, "vctime-errors-" + dateString + ".log");
        
        // Load configuration
        loadConfiguration();
        
        // Start async log processing
        startAsyncLogging();
        
        info("EnhancedLogger", "Enhanced logging system initialized");
    }
    
    /**
     * Load logging configuration from plugin config
     */
    private void loadConfiguration() {
        this.enableDebugLogging = plugin.getConfig().getBoolean("debug-mode", false);
        this.enableErrorTracking = plugin.getConfig().getBoolean("error-tracking", true);
        this.enablePerformanceLogging = plugin.getConfig().getBoolean("performance-logging", true);
        this.enableAsyncLogging = plugin.getConfig().getBoolean("async-logging", true);
    }
    
    /**
     * Log info message
     */
    public void info(String category, String message) {
        log(LogLevel.INFO, category, message, null);
    }
    
    /**
     * Log warning message
     */
    public void warn(String category, String message) {
        warn(category, message, null);
    }
    
    /**
     * Log warning with exception
     */
    public void warn(String category, String message, Throwable throwable) {
        log(LogLevel.WARNING, category, message, throwable);
        totalWarnings++;
    }
    
    /**
     * Log error message
     */
    public void error(String category, String message) {
        error(category, message, null);
    }
    
    /**
     * Log error with exception
     */
    public void error(String category, String message, Throwable throwable) {
        log(LogLevel.ERROR, category, message, throwable);
        totalErrors++;
        
        if (enableErrorTracking) {
            trackError(category, message);
        }
    }
    
    /**
     * Log debug message (only if debug mode is enabled)
     */
    public void debug(String category, String message) {
        if (enableDebugLogging) {
            log(LogLevel.DEBUG, category, message, null);
        }
    }
    
    /**
     * Log performance metric
     */
    public void performance(String operation, long durationMs) {
        performance(operation, durationMs, null);
    }
    
    /**
     * Log performance metric with additional context
     */
    public void performance(String operation, long durationMs, String context) {
        if (enablePerformanceLogging) {
            String message = String.format("Operation '%s' took %dms", operation, durationMs);
            if (context != null) {
                message += " (" + context + ")";
            }
            log(LogLevel.PERFORMANCE, "PERFORMANCE", message, null);
        }
    }
    
    /**
     * Log user action for auditing
     */
    public void userAction(Player player, String action, String details) {
        String message = String.format("Player %s performed action: %s", player.getName(), action);
        if (details != null) {
            message += " - " + details;
        }
        log(LogLevel.USER_ACTION, "USER_ACTION", message, null);
    }
    
    /**
     * Log Discord interaction
     */
    public void discord(String event, String details) {
        log(LogLevel.DISCORD, "DISCORD", event + ": " + details, null);
    }
    
    /**
     * Log database operation
     */
    public void database(String operation, long durationMs, boolean success) {
        String status = success ? "SUCCESS" : "FAILED";
        String message = String.format("Database %s - %s (%dms)", operation, status, durationMs);
        LogLevel level = success ? LogLevel.DATABASE : LogLevel.ERROR;
        log(level, "DATABASE", message, null);
    }
    
    /**
     * Core logging method
     */
    private void log(LogLevel level, String category, String message, Throwable throwable) {
        totalLogEntries++;
        categoryStats.merge(category, 1L, Long::sum);
        
        LogEntry entry = new LogEntry(level, category, message, throwable, System.currentTimeMillis());
        
        // Log to plugin logger
        switch (level) {
            case ERROR:
                plugin.getLogger().severe("[" + category + "] " + message);
                if (throwable != null) {
                    plugin.getLogger().log(Level.SEVERE, "Exception details:", throwable);
                }
                break;
            case WARNING:
                plugin.getLogger().warning("[" + category + "] " + message);
                break;
            case INFO:
            case USER_ACTION:
            case DISCORD:
            case DATABASE:
                plugin.getLogger().info("[" + category + "] " + message);
                break;
            case DEBUG:
            case PERFORMANCE:
                plugin.getLogger().fine("[" + category + "] " + message);
                break;
        }
        
        // Add to async buffer
        if (enableAsyncLogging && logBuffer.size() < maxLogBufferSize) {
            logBuffer.offer(entry);
        } else if (!enableAsyncLogging) {
            writeLogEntry(entry);
        }
    }
    
    /**
     * Track error occurrences and frequency
     */
    private void trackError(String category, String message) {
        String errorKey = category + ":" + message;
        
        // Check if we should throttle this error
        Long lastTime = lastErrorTimes.get(errorKey);
        long currentTime = System.currentTimeMillis();
        
        if (lastTime != null && (currentTime - lastTime) < errorThrottleTime) {
            return; // Throttle duplicate errors
        }
        
        errorCounts.merge(errorKey, 1, Integer::sum);
        lastErrorTimes.put(errorKey, currentTime);
        
        // Alert if error frequency is high
        int count = errorCounts.get(errorKey);
        if (count > 10) {
            plugin.getLogger().severe("ALERT: Error '" + errorKey + "' has occurred " + count + " times!");
            
            // Notify online admins
            String alertMessage = ChatColor.RED + "[VCTime] Critical error detected: " + message;
            Bukkit.getOnlinePlayers().stream()
                    .filter(player -> player.hasPermission("vctime.admin"))
                    .forEach(player -> player.sendMessage(alertMessage));
        }
    }
    
    /**
     * Start async logging task
     */
    private void startAsyncLogging() {
        if (enableAsyncLogging) {
            logExecutor.scheduleAtFixedRate(() -> {
                try {
                    processLogBuffer();
                } catch (Exception e) {
                    plugin.getLogger().severe("Error processing log buffer: " + e.getMessage());
                }
            }, 1, 1, TimeUnit.SECONDS);
        }
    }
    
    /**
     * Process pending log entries
     */
    private void processLogBuffer() {
        List<LogEntry> entries = new ArrayList<>();
        LogEntry entry;
        
        // Drain buffer
        while ((entry = logBuffer.poll()) != null && entries.size() < 100) {
            entries.add(entry);
        }
        
        if (!entries.isEmpty()) {
            writeLogEntries(entries);
        }
    }
    
    /**
     * Write multiple log entries to file
     */
    private void writeLogEntries(List<LogEntry> entries) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true));
             PrintWriter errorWriter = new PrintWriter(new FileWriter(errorLogFile, true))) {
            
            for (LogEntry entry : entries) {
                String logLine = formatLogEntry(entry);
                writer.println(logLine);
                
                // Also write errors to separate error log
                if (entry.level == LogLevel.ERROR) {
                    errorWriter.println(logLine);
                    if (entry.throwable != null) {
                        entry.throwable.printStackTrace(errorWriter);
                    }
                }
            }
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write to log file: " + e.getMessage());
        }
    }
    
    /**
     * Write single log entry to file
     */
    private void writeLogEntry(LogEntry entry) {
        writeLogEntries(Arrays.asList(entry));
    }
    
    /**
     * Format log entry for file output
     */
    private String formatLogEntry(LogEntry entry) {
        return String.format("[%s] [%s] [%s] %s",
                dateFormat.format(new Date(entry.timestamp)),
                entry.level.name(),
                entry.category,
                entry.message);
    }
    
    /**
     * Get logging statistics
     */
    public Map<String, Object> getLoggingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogEntries", totalLogEntries);
        stats.put("totalErrors", totalErrors);
        stats.put("totalWarnings", totalWarnings);
        stats.put("bufferedEntries", logBuffer.size());
        stats.put("categoryStats", new HashMap<>(categoryStats));
        stats.put("topErrors", getTopErrors());
        return stats;
    }
    
    /**
     * Get most frequent errors
     */
    private Map<String, Integer> getTopErrors() {
        return errorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
    }
    
    /**
     * Generate log summary report
     */
    public String generateLogReport() {
        StringBuilder report = new StringBuilder();
        Map<String, Object> stats = getLoggingStats();
        
        report.append("=== VCTimeRewards Log Report ===\n");
        report.append(String.format("Total Log Entries: %d\n", stats.get("totalLogEntries")));
        report.append(String.format("Total Errors: %d\n", stats.get("totalErrors")));
        report.append(String.format("Total Warnings: %d\n", stats.get("totalWarnings")));
        report.append(String.format("Buffered Entries: %d\n", stats.get("bufferedEntries")));
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> topErrors = (Map<String, Integer>) stats.get("topErrors");
        if (!topErrors.isEmpty()) {
            report.append("\nTop Errors:\n");
            topErrors.forEach((error, count) -> 
                report.append(String.format("  %s: %d occurrences\n", error, count)));
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Long> categoryStats = (Map<String, Long>) stats.get("categoryStats");
        if (!categoryStats.isEmpty()) {
            report.append("\nLog Categories:\n");
            categoryStats.forEach((category, count) -> 
                report.append(String.format("  %s: %d entries\n", category, count)));
        }
        
        return report.toString();
    }
    
    /**
     * Force flush all pending logs
     */
    public void flush() {
        processLogBuffer();
    }
    
    /**
     * Shutdown logging system
     */
    public void shutdown() {
        info("EnhancedLogger", "Shutting down enhanced logging system...");
        
        // Process remaining logs
        flush();
        
        // Shutdown executor
        logExecutor.shutdown();
        try {
            if (!logExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Write final stats
        String finalReport = generateLogReport();
        plugin.getLogger().info("Final logging statistics:\n" + finalReport);
    }
    
    /**
     * Log levels for categorization
     */
    private enum LogLevel {
        DEBUG, INFO, WARNING, ERROR, PERFORMANCE, USER_ACTION, DISCORD, DATABASE
    }
    
    /**
     * Internal log entry structure
     */
    private static class LogEntry {
        final LogLevel level;
        final String category;
        final String message;
        final Throwable throwable;
        final long timestamp;
        
        LogEntry(LogLevel level, String category, String message, Throwable throwable, long timestamp) {
            this.level = level;
            this.category = category;
            this.message = message;
            this.throwable = throwable;
            this.timestamp = timestamp;
        }
    }
}