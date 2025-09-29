package me.kiro.vctime.managers;

import me.kiro.vctime.VCTimeRewards;
import me.kiro.vctime.managers.TimeManager.PlayerTimeEntry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Optimized data manager with async operations, caching, and batch processing
 * Provides high-performance data operations for the VCTimeRewards plugin
 */
public class OptimizedDataManager {
    
    private final VCTimeRewards plugin;
    private final ScheduledExecutorService asyncExecutor;
    
    // Cache for frequently accessed data
    private final Map<UUID, Long> cachedTotalTimes;
    private final Map<UUID, Integer> cachedMessageCounts;
    private final List<PlayerTimeEntry> cachedLeaderboard;
    private final Map<String, Object> generalCache;
    
    // Cache expiration times (in milliseconds)
    private final Map<String, Long> cacheExpiry;
    private final long CACHE_DURATION = 30000; // 30 seconds
    private final long LEADERBOARD_CACHE_DURATION = 60000; // 1 minute
    
    // Batch operation queue
    private final Queue<DataOperation> pendingOperations;
    private final Object batchLock = new Object();
    
    // Performance monitoring
    private volatile long totalOperations = 0;
    private volatile long averageOperationTime = 0;
    private volatile long cacheHits = 0;
    private volatile long cacheMisses = 0;
    
    public OptimizedDataManager(VCTimeRewards plugin) {
        this.plugin = plugin;
        this.asyncExecutor = Executors.newScheduledThreadPool(2);
        
        // Initialize caches
        this.cachedTotalTimes = new ConcurrentHashMap<>();
        this.cachedMessageCounts = new ConcurrentHashMap<>();
        this.cachedLeaderboard = new ArrayList<>();
        this.generalCache = new ConcurrentHashMap<>();
        this.cacheExpiry = new ConcurrentHashMap<>();
        
        // Initialize batch queue
        this.pendingOperations = new ConcurrentLinkedQueue<>();
        
        // Start background tasks
        startCacheCleanup();
        startBatchProcessor();
        startPerformanceMonitor();
        
        plugin.getLogger().info("OptimizedDataManager initialized with async operations and caching");
    }
    
    /**
     * Get total time with caching
     */
    public CompletableFuture<Long> getTotalTimeAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            
            try {
                // Check cache first
                if (cachedTotalTimes.containsKey(playerId) && !isCacheExpired("totalTime_" + playerId)) {
                    cacheHits++;
                    return cachedTotalTimes.get(playerId);
                }
                
                // Cache miss - get from TimeManager
                cacheMisses++;
                long totalTime = plugin.getTimeManager().getTotalTime(playerId);
                
                // Update cache
                cachedTotalTimes.put(playerId, totalTime);
                cacheExpiry.put("totalTime_" + playerId, System.currentTimeMillis() + CACHE_DURATION);
                
                return totalTime;
            } finally {
                updateOperationTime(System.nanoTime() - startTime);
            }
        }, asyncExecutor);
    }
    
    /**
     * Get message count with caching
     */
    public CompletableFuture<Integer> getMessageCountAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            
            try {
                // Check cache first
                if (cachedMessageCounts.containsKey(playerId) && !isCacheExpired("messageCount_" + playerId)) {
                    cacheHits++;
                    return cachedMessageCounts.get(playerId);
                }
                
                // Cache miss - get from ChatManager
                cacheMisses++;
                int messageCount = plugin.getChatManager().getMessageCount(playerId);
                
                // Update cache
                cachedMessageCounts.put(playerId, messageCount);
                cacheExpiry.put("messageCount_" + playerId, System.currentTimeMillis() + CACHE_DURATION);
                
                return messageCount;
            } finally {
                updateOperationTime(System.nanoTime() - startTime);
            }
        }, asyncExecutor);
    }
    
    /**
     * Get leaderboard with caching and async loading
     */
    public CompletableFuture<List<PlayerTimeEntry>> getLeaderboardAsync(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            
            try {
                // Check cache first
                if (!cachedLeaderboard.isEmpty() && !isCacheExpired("leaderboard")) {
                    cacheHits++;
                    return cachedLeaderboard.subList(0, Math.min(limit, cachedLeaderboard.size()));
                }
                
                // Cache miss - get from TimeManager
                cacheMisses++;
                List<PlayerTimeEntry> leaderboard = plugin.getTimeManager().getTopPlayers(limit);
                
                // Update cache
                synchronized (cachedLeaderboard) {
                    cachedLeaderboard.clear();
                    cachedLeaderboard.addAll(leaderboard);
                }
                cacheExpiry.put("leaderboard", System.currentTimeMillis() + LEADERBOARD_CACHE_DURATION);
                
                return leaderboard;
            } finally {
                updateOperationTime(System.nanoTime() - startTime);
            }
        }, asyncExecutor);
    }
    
    /**
     * Batch save operation - queues multiple saves to be processed together
     */
    public void queueSaveOperation(String dataType, UUID playerId, Object data) {
        synchronized (batchLock) {
            pendingOperations.offer(new DataOperation(dataType, playerId, data, System.currentTimeMillis()));
        }
    }
    
    /**
     * Force immediate save of all pending operations
     */
    public CompletableFuture<Void> flushPendingOperations() {
        return CompletableFuture.runAsync(() -> {
            processBatchOperations();
        }, asyncExecutor);
    }
    
    /**
     * Invalidate cache for a specific player
     */
    public void invalidatePlayerCache(UUID playerId) {
        cachedTotalTimes.remove(playerId);
        cachedMessageCounts.remove(playerId);
        cacheExpiry.remove("totalTime_" + playerId);
        cacheExpiry.remove("messageCount_" + playerId);
        cacheExpiry.remove("leaderboard"); // Invalidate leaderboard as it may include this player
    }
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        cachedTotalTimes.clear();
        cachedMessageCounts.clear();
        synchronized (cachedLeaderboard) {
            cachedLeaderboard.clear();
        }
        generalCache.clear();
        cacheExpiry.clear();
        plugin.getLogger().info("All caches cleared");
    }
    
    /**
     * Get performance statistics
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOperations", totalOperations);
        stats.put("averageOperationTimeMs", averageOperationTime / 1_000_000.0);
        stats.put("cacheHits", cacheHits);
        stats.put("cacheMisses", cacheMisses);
        stats.put("cacheHitRate", cacheHits + cacheMisses > 0 ? (double) cacheHits / (cacheHits + cacheMisses) * 100 : 0);
        stats.put("cachedTotalTimes", cachedTotalTimes.size());
        stats.put("cachedMessageCounts", cachedMessageCounts.size());
        stats.put("pendingOperations", pendingOperations.size());
        return stats;
    }
    
    /**
     * Warm up cache with commonly accessed data
     */
    public CompletableFuture<Void> warmUpCache() {
        return CompletableFuture.runAsync(() -> {
            plugin.getLogger().info("Warming up cache...");
            
            // Pre-load top players
            getLeaderboardAsync(20).join();
            
            // Pre-load online player data
            Bukkit.getOnlinePlayers().forEach(player -> {
                UUID playerId = player.getUniqueId();
                getTotalTimeAsync(playerId);
                getMessageCountAsync(playerId);
            });
            
            plugin.getLogger().info("Cache warm-up completed");
        }, asyncExecutor);
    }
    
    /**
     * Check if cache entry is expired
     */
    private boolean isCacheExpired(String key) {
        Long expiry = cacheExpiry.get(key);
        return expiry == null || System.currentTimeMillis() > expiry;
    }
    
    /**
     * Update operation time statistics
     */
    private void updateOperationTime(long operationTimeNanos) {
        totalOperations++;
        averageOperationTime = (averageOperationTime * (totalOperations - 1) + operationTimeNanos) / totalOperations;
    }
    
    /**
     * Start cache cleanup task
     */
    private void startCacheCleanup() {
        asyncExecutor.scheduleAtFixedRate(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                
                // Clean expired cache entries
                cacheExpiry.entrySet().removeIf(entry -> currentTime > entry.getValue());
                
                // Clean corresponding cached data
                for (String key : new HashSet<>(cacheExpiry.keySet())) {
                    if (key.startsWith("totalTime_")) {
                        UUID playerId = UUID.fromString(key.substring(10));
                        if (isCacheExpired(key)) {
                            cachedTotalTimes.remove(playerId);
                        }
                    } else if (key.startsWith("messageCount_")) {
                        UUID playerId = UUID.fromString(key.substring(13));
                        if (isCacheExpired(key)) {
                            cachedMessageCounts.remove(playerId);
                        }
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("Error during cache cleanup: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Start batch operation processor
     */
    private void startBatchProcessor() {
        asyncExecutor.scheduleAtFixedRate(() -> {
            try {
                processBatchOperations();
            } catch (Exception e) {
                plugin.getLogger().warning("Error during batch processing: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Process pending batch operations
     */
    private void processBatchOperations() {
        if (pendingOperations.isEmpty()) {
            return;
        }
        
        synchronized (batchLock) {
            List<DataOperation> operations = new ArrayList<>();
            DataOperation operation;
            
            // Collect all pending operations
            while ((operation = pendingOperations.poll()) != null) {
                operations.add(operation);
            }
            
            if (!operations.isEmpty()) {
                plugin.getLogger().fine("Processing " + operations.size() + " batch operations");
                
                // Group operations by type for efficiency
                Map<String, List<DataOperation>> groupedOps = new HashMap<>();
                for (DataOperation op : operations) {
                    groupedOps.computeIfAbsent(op.dataType, k -> new ArrayList<>()).add(op);
                }
                
                // Process each group
                for (Map.Entry<String, List<DataOperation>> entry : groupedOps.entrySet()) {
                    processBatchGroup(entry.getKey(), entry.getValue());
                }
            }
        }
    }
    
    /**
     * Process a group of operations of the same type
     */
    private void processBatchGroup(String dataType, List<DataOperation> operations) {
        try {
            switch (dataType) {
                case "timeData":
                    // Batch save time data
                    plugin.getTimeManager().saveAll();
                    break;
                case "chatData":
                    // Batch save chat data
                    plugin.getChatManager().saveData();
                    break;
                default:
                    plugin.getLogger().warning("Unknown batch operation type: " + dataType);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing batch group " + dataType + ": " + e.getMessage());
        }
    }
    
    /**
     * Start performance monitoring
     */
    private void startPerformanceMonitor() {
        asyncExecutor.scheduleAtFixedRate(() -> {
            try {
                Map<String, Object> stats = getPerformanceStats();
                if (totalOperations > 0 && totalOperations % 1000 == 0) {
                    plugin.getLogger().info("Performance Stats: " + stats);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error during performance monitoring: " + e.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    /**
     * Shutdown the data manager
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down OptimizedDataManager...");
        
        // Process remaining operations
        flushPendingOperations().join();
        
        // Shutdown executor
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Log final stats
        Map<String, Object> finalStats = getPerformanceStats();
        plugin.getLogger().info("Final performance stats: " + finalStats);
        plugin.getLogger().info("OptimizedDataManager shutdown complete");
    }
    
    /**
     * Data operation for batch processing
     */
    private static class DataOperation {
        final String dataType;
        final UUID playerId;
        final Object data;
        final long timestamp;
        
        DataOperation(String dataType, UUID playerId, Object data, long timestamp) {
            this.dataType = dataType;
            this.playerId = playerId;
            this.data = data;
            this.timestamp = timestamp;
        }
    }
}