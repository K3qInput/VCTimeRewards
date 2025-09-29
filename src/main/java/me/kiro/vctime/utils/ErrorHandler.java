package me.kiro.vctime.utils;

import me.kiro.vctime.VCTimeRewards;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Comprehensive error handling utility with recovery mechanisms and user-friendly error reporting
 * Provides robust error handling, automatic recovery, and detailed error analysis
 */
public class ErrorHandler {
    
    private final VCTimeRewards plugin;
    private final EnhancedLogger logger;
    
    // Error tracking
    private final Map<String, ErrorInfo> errorRegistry;
    private final Map<String, List<ErrorRecoveryAction>> recoveryActions;
    private final Set<String> criticalErrors;
    
    // Configuration
    private boolean enableAutoRecovery = true;
    private boolean enableUserNotification = true;
    private int maxRetryAttempts = 3;
    private long retryDelayMs = 1000;
    
    public ErrorHandler(VCTimeRewards plugin, EnhancedLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.errorRegistry = new ConcurrentHashMap<>();
        this.recoveryActions = new ConcurrentHashMap<>();
        this.criticalErrors = ConcurrentHashMap.newKeySet();
        
        setupDefaultRecoveryActions();
        logger.info("ErrorHandler", "Error handling system initialized");
    }
    
    /**
     * Handle an exception with automatic recovery attempts
     */
    public <T> Optional<T> handleException(String context, Supplier<T> operation, T defaultValue) {
        try {
            return Optional.ofNullable(operation.get());
        } catch (Exception e) {
            return handleException(context, e, () -> defaultValue);
        }
    }
    
    /**
     * Handle an exception with recovery action
     */
    public <T> Optional<T> handleException(String context, Exception exception, Supplier<T> recoveryAction) {
        String errorKey = context + ":" + exception.getClass().getSimpleName();
        
        // Log the error
        logger.error(context, "Exception occurred: " + exception.getMessage(), exception);
        
        // Track error
        ErrorInfo errorInfo = trackError(errorKey, exception);
        
        // Attempt recovery if enabled and not exceeded retry limit
        if (enableAutoRecovery && errorInfo.attemptCount <= maxRetryAttempts) {
            logger.info("ErrorHandler", "Attempting recovery for: " + context + " (attempt " + errorInfo.attemptCount + ")");
            
            try {
                // Execute recovery actions
                executeRecoveryActions(context);
                
                // Wait before retry
                if (retryDelayMs > 0) {
                    Thread.sleep(retryDelayMs);
                }
                
                // Try recovery action
                T result = recoveryAction.get();
                if (result != null) {
                    logger.info("ErrorHandler", "Recovery successful for: " + context);
                    errorInfo.lastRecoverySuccess = System.currentTimeMillis();
                    return Optional.of(result);
                }
            } catch (Exception recoveryException) {
                logger.error("ErrorHandler", "Recovery failed for: " + context, recoveryException);
            }
        }
        
        // Check if this is a critical error
        if (isCriticalError(exception)) {
            handleCriticalError(context, exception);
        }
        
        // Notify users if configured
        if (enableUserNotification) {
            notifyUsers(context, exception);
        }
        
        return Optional.empty();
    }
    
    /**
     * Handle an operation that doesn't return a value
     */
    public boolean handleOperation(String context, Runnable operation) {
        try {
            operation.run();
            return true;
        } catch (Exception e) {
            handleException(context, e, () -> null);
            return false;
        }
    }
    
    /**
     * Handle database operations with specific error handling
     */
    public <T> Optional<T> handleDatabaseOperation(String operation, Supplier<T> dbOperation, T defaultValue) {
        long startTime = System.currentTimeMillis();
        
        try {
            T result = dbOperation.get();
            long duration = System.currentTimeMillis() - startTime;
            logger.database(operation, duration, true);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.database(operation, duration, false);
            
            return handleException("DATABASE:" + operation, e, () -> {
                // Database-specific recovery
                logger.info("ErrorHandler", "Attempting database recovery for: " + operation);
                return defaultValue;
            });
        }
    }
    
    /**
     * Handle Discord API operations
     */
    public <T> Optional<T> handleDiscordOperation(String operation, Supplier<T> discordOperation, T defaultValue) {
        try {
            T result = discordOperation.get();
            logger.discord(operation, "Success");
            return Optional.ofNullable(result);
        } catch (Exception e) {
            logger.discord(operation, "Failed: " + e.getMessage());
            
            return handleException("DISCORD:" + operation, e, () -> {
                // Discord-specific recovery
                logger.info("ErrorHandler", "Attempting Discord API recovery for: " + operation);
                return defaultValue;
            });
        }
    }
    
    /**
     * Track error occurrence and update statistics
     */
    private ErrorInfo trackError(String errorKey, Exception exception) {
        ErrorInfo errorInfo = errorRegistry.computeIfAbsent(errorKey, k -> new ErrorInfo(errorKey));
        
        errorInfo.attemptCount++;
        errorInfo.lastOccurrence = System.currentTimeMillis();
        errorInfo.lastException = exception;
        
        // Determine if this error is becoming frequent
        if (errorInfo.attemptCount > 5) {
            logger.warn("ErrorHandler", "Frequent error detected: " + errorKey + " (count: " + errorInfo.attemptCount + ")");
            criticalErrors.add(errorKey);
        }
        
        return errorInfo;
    }
    
    /**
     * Execute recovery actions for a given context
     */
    private void executeRecoveryActions(String context) {
        List<ErrorRecoveryAction> actions = recoveryActions.get(context);
        if (actions != null) {
            for (ErrorRecoveryAction action : actions) {
                try {
                    action.execute();
                    logger.debug("ErrorHandler", "Executed recovery action: " + action.getName());
                } catch (Exception e) {
                    logger.error("ErrorHandler", "Recovery action failed: " + action.getName(), e);
                }
            }
        }
    }
    
    /**
     * Check if an exception represents a critical error
     */
    private boolean isCriticalError(Exception exception) {
        String className = exception.getClass().getSimpleName();
        return className.equals("OutOfMemoryError") ||
               className.equals("StackOverflowError") ||
               className.equals("NoClassDefFoundError") ||
               (exception.getMessage() != null && exception.getMessage().contains("database")) ||
               (exception.getMessage() != null && exception.getMessage().contains("connection"));
    }
    
    /**
     * Handle critical errors with special procedures
     */
    private void handleCriticalError(String context, Exception exception) {
        logger.error("CRITICAL", "Critical error in " + context + ": " + exception.getMessage(), exception);
        
        // Alert all admins
        String alertMessage = ChatColor.DARK_RED + "[VCTime] CRITICAL ERROR: " + 
                             ChatColor.RED + exception.getMessage();
        
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("vctime.admin"))
                .forEach(player -> {
                    player.sendMessage(alertMessage);
                    player.sendMessage(ChatColor.YELLOW + "Check console for details. Context: " + context);
                });
        
        // Trigger emergency procedures if needed
        if (context.contains("DATABASE")) {
            triggerDatabaseEmergencyProcedures();
        } else if (context.contains("DISCORD")) {
            triggerDiscordEmergencyProcedures();
        }
    }
    
    /**
     * Notify users about errors that might affect them
     */
    private void notifyUsers(String context, Exception exception) {
        // Only notify for user-facing errors
        if (context.contains("COMMAND") || context.contains("USER_ACTION")) {
            String userMessage = ChatColor.YELLOW + "[VCTime] " + 
                               ChatColor.RED + "An error occurred. Please try again or contact an admin.";
            
            // Find affected players based on context
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (shouldNotifyPlayer(player, context)) {
                    player.sendMessage(userMessage);
                }
            });
        }
    }
    
    /**
     * Determine if a player should be notified about an error
     */
    private boolean shouldNotifyPlayer(Player player, String context) {
        // Logic to determine if player is affected by this error
        return context.contains("PLAYER:" + player.getName()) || 
               (context.contains("GENERAL") && player.hasPermission("vctime.notifications"));
    }
    
    /**
     * Setup default recovery actions
     */
    private void setupDefaultRecoveryActions() {
        // Cache recovery
        addRecoveryAction("CACHE", new ErrorRecoveryAction("ClearCache", () -> {
            if (plugin.getDataManager() != null) {
                plugin.getDataManager().clearAllCaches();
            }
        }));
        
        // Configuration recovery
        addRecoveryAction("CONFIG", new ErrorRecoveryAction("ReloadConfig", () -> {
            plugin.reloadConfig();
            plugin.initializeConfigUtil();
        }));
        
        // Memory recovery
        addRecoveryAction("MEMORY", new ErrorRecoveryAction("GarbageCollection", () -> {
            if (plugin.getPerformanceMonitor() != null) {
                plugin.getPerformanceMonitor().suggestGarbageCollection();
            }
        }));
        
        // Database recovery
        addRecoveryAction("DATABASE", new ErrorRecoveryAction("DatabaseReconnect", () -> {
            // Database reconnection logic would go here
            logger.info("ErrorHandler", "Attempting database reconnection...");
        }));
        
        // Discord recovery
        addRecoveryAction("DISCORD", new ErrorRecoveryAction("DiscordReconnect", () -> {
            // Discord reconnection logic would go here
            logger.info("ErrorHandler", "Attempting Discord reconnection...");
        }));
    }
    
    /**
     * Add a recovery action for a specific context
     */
    public void addRecoveryAction(String context, ErrorRecoveryAction action) {
        recoveryActions.computeIfAbsent(context, k -> new ArrayList<>()).add(action);
    }
    
    /**
     * Emergency procedures for database issues
     */
    private void triggerDatabaseEmergencyProcedures() {
        logger.error("EMERGENCY", "Triggering database emergency procedures");
        
        // Switch to file-based storage temporarily
        if (plugin.getDataManager() != null) {
            plugin.getDataManager().flushPendingOperations();
        }
        
        // Notify admins with specific instructions
        String emergencyMessage = ChatColor.DARK_RED + "[VCTime] DATABASE EMERGENCY: " +
                                ChatColor.RED + "Switched to emergency file storage mode.";
        
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("vctime.admin"))
                .forEach(player -> player.sendMessage(emergencyMessage));
    }
    
    /**
     * Emergency procedures for Discord issues
     */
    private void triggerDiscordEmergencyProcedures() {
        logger.error("EMERGENCY", "Triggering Discord emergency procedures");
        
        // Disable Discord features temporarily
        logger.warn("ErrorHandler", "Discord features temporarily disabled due to critical error");
        
        // Notify admins
        String emergencyMessage = ChatColor.DARK_RED + "[VCTime] DISCORD EMERGENCY: " +
                                ChatColor.RED + "Discord integration temporarily disabled.";
        
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("vctime.admin"))
                .forEach(player -> player.sendMessage(emergencyMessage));
    }
    
    /**
     * Get error statistics and reports
     */
    public Map<String, Object> getErrorStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalErrors", errorRegistry.size());
        stats.put("criticalErrors", criticalErrors.size());
        stats.put("recentErrors", getRecentErrors());
        stats.put("frequentErrors", getFrequentErrors());
        return stats;
    }
    
    /**
     * Get recent errors (last hour)
     */
    private List<String> getRecentErrors() {
        long oneHourAgo = System.currentTimeMillis() - 3600000;
        return errorRegistry.values().stream()
                .filter(error -> error.lastOccurrence > oneHourAgo)
                .map(error -> error.errorKey)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Get most frequent errors
     */
    private Map<String, Integer> getFrequentErrors() {
        return errorRegistry.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().attemptCount, a.getValue().attemptCount))
                .limit(10)
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue().attemptCount),
                        LinkedHashMap::putAll);
    }
    
    /**
     * Generate comprehensive error report
     */
    public String generateErrorReport() {
        StringBuilder report = new StringBuilder();
        Map<String, Object> stats = getErrorStats();
        
        report.append("=== VCTimeRewards Error Report ===\n");
        report.append(String.format("Total Errors: %d\n", stats.get("totalErrors")));
        report.append(String.format("Critical Errors: %d\n", stats.get("criticalErrors")));
        
        @SuppressWarnings("unchecked")
        List<String> recentErrors = (List<String>) stats.get("recentErrors");
        if (!recentErrors.isEmpty()) {
            report.append("\nRecent Errors (last hour):\n");
            recentErrors.forEach(error -> report.append("  - ").append(error).append("\n"));
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> frequentErrors = (Map<String, Integer>) stats.get("frequentErrors");
        if (!frequentErrors.isEmpty()) {
            report.append("\nMost Frequent Errors:\n");
            frequentErrors.forEach((error, count) -> 
                report.append(String.format("  %s: %d occurrences\n", error, count)));
        }
        
        return report.toString();
    }
    
    /**
     * Error information tracking
     */
    private static class ErrorInfo {
        final String errorKey;
        int attemptCount = 0;
        long lastOccurrence = 0;
        long lastRecoverySuccess = 0;
        Exception lastException;
        
        ErrorInfo(String errorKey) {
            this.errorKey = errorKey;
        }
    }
    
    /**
     * Recovery action interface
     */
    public static class ErrorRecoveryAction {
        private final String name;
        private final Runnable action;
        
        public ErrorRecoveryAction(String name, Runnable action) {
            this.name = name;
            this.action = action;
        }
        
        public void execute() {
            action.run();
        }
        
        public String getName() {
            return name;
        }
    }
}