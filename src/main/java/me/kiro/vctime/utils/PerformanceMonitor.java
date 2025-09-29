package me.kiro.vctime.utils;

import me.kiro.vctime.VCTimeRewards;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performance monitoring and optimization utilities
 * Tracks memory usage, CPU usage, and plugin performance metrics
 */
public class PerformanceMonitor {
    
    private final VCTimeRewards plugin;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    
    // Performance metrics
    private final Map<String, Long> operationTimes;
    private final Map<String, Integer> operationCounts;
    private long lastGcTime = 0;
    private long lastGcRuns = 0;
    
    // Memory thresholds
    private static final double MEMORY_WARNING_THRESHOLD = 0.8; // 80%
    private static final double MEMORY_CRITICAL_THRESHOLD = 0.9; // 90%
    
    // Monitoring task
    private BukkitRunnable monitoringTask;
    
    public PerformanceMonitor(VCTimeRewards plugin) {
        this.plugin = plugin;
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.operationTimes = new ConcurrentHashMap<>();
        this.operationCounts = new ConcurrentHashMap<>();
        
        // Enable CPU time tracking
        if (threadBean.isCurrentThreadCpuTimeSupported()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }
        
        startMonitoring();
    }
    
    /**
     * Start performance monitoring task
     */
    public void startMonitoring() {
        if (monitoringTask != null) {
            monitoringTask.cancel();
        }
        
        monitoringTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    checkMemoryUsage();
                    checkGarbageCollection();
                    logPerformanceMetrics();
                } catch (Exception e) {
                    plugin.getLogger().warning("Error in performance monitoring: " + e.getMessage());
                }
            }
        };
        
        // Run every 30 seconds
        monitoringTask.runTaskTimerAsynchronously(plugin, 600L, 600L);
        plugin.getLogger().info("Performance monitoring started");
    }
    
    /**
     * Stop performance monitoring
     */
    public void stopMonitoring() {
        if (monitoringTask != null) {
            monitoringTask.cancel();
            monitoringTask = null;
        }
        plugin.getLogger().info("Performance monitoring stopped");
    }
    
    /**
     * Record operation time for performance tracking
     */
    public void recordOperation(String operationName, long timeNanos) {
        operationTimes.merge(operationName, timeNanos, Long::sum);
        operationCounts.merge(operationName, 1, Integer::sum);
    }
    
    /**
     * Get performance statistics
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Memory stats
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
        
        stats.put("heapUsed", heapMemory.getUsed());
        stats.put("heapMax", heapMemory.getMax());
        stats.put("heapUsagePercent", (double) heapMemory.getUsed() / heapMemory.getMax() * 100);
        stats.put("nonHeapUsed", nonHeapMemory.getUsed());
        stats.put("nonHeapMax", nonHeapMemory.getMax());
        
        // Thread stats
        stats.put("threadCount", threadBean.getThreadCount());
        stats.put("peakThreadCount", threadBean.getPeakThreadCount());
        stats.put("daemonThreadCount", threadBean.getDaemonThreadCount());
        
        // Operation stats
        Map<String, Double> avgOperationTimes = new HashMap<>();
        for (String operation : operationTimes.keySet()) {
            long totalTime = operationTimes.get(operation);
            int count = operationCounts.get(operation);
            avgOperationTimes.put(operation, (double) totalTime / count / 1_000_000.0); // Convert to milliseconds
        }
        stats.put("averageOperationTimes", avgOperationTimes);
        stats.put("operationCounts", new HashMap<>(operationCounts));
        
        return stats;
    }
    
    /**
     * Get memory optimization recommendations
     */
    public String[] getOptimizationRecommendations() {
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        double usagePercent = (double) heapMemory.getUsed() / heapMemory.getMax();
        
        if (usagePercent > MEMORY_CRITICAL_THRESHOLD) {
            return new String[]{
                "CRITICAL: Memory usage above 90%",
                "Consider increasing server memory with -Xmx flag",
                "Clear plugin caches immediately",
                "Review data retention policies"
            };
        } else if (usagePercent > MEMORY_WARNING_THRESHOLD) {
            return new String[]{
                "WARNING: Memory usage above 80%",
                "Consider clearing old cache entries",
                "Monitor for memory leaks",
                "Optimize data structures"
            };
        } else {
            return new String[]{
                "Memory usage is within normal limits",
                "Performance appears optimal"
            };
        }
    }
    
    /**
     * Force garbage collection if memory usage is high
     */
    public boolean suggestGarbageCollection() {
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        double usagePercent = (double) heapMemory.getUsed() / heapMemory.getMax();
        
        if (usagePercent > MEMORY_WARNING_THRESHOLD) {
            plugin.getLogger().info("High memory usage detected (" + String.format("%.1f", usagePercent * 100) + "%), suggesting garbage collection");
            System.gc(); // Suggest GC (not guaranteed)
            return true;
        }
        
        return false;
    }
    
    /**
     * Get current CPU usage for the main thread
     */
    public double getCurrentCpuUsage() {
        if (!threadBean.isCurrentThreadCpuTimeSupported()) {
            return -1;
        }
        
        long currentTime = System.nanoTime();
        long currentCpuTime = threadBean.getCurrentThreadCpuTime();
        
        // Calculate CPU usage percentage (simplified)
        return (double) currentCpuTime / currentTime * 100;
    }
    
    /**
     * Check memory usage and issue warnings
     */
    private void checkMemoryUsage() {
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        double usagePercent = (double) heapMemory.getUsed() / heapMemory.getMax();
        
        if (usagePercent > MEMORY_CRITICAL_THRESHOLD) {
            plugin.getLogger().severe("CRITICAL: Memory usage is at " + String.format("%.1f", usagePercent * 100) + "%!");
            plugin.getDataManager().clearAllCaches();
            suggestGarbageCollection();
        } else if (usagePercent > MEMORY_WARNING_THRESHOLD) {
            plugin.getLogger().warning("High memory usage detected: " + String.format("%.1f", usagePercent * 100) + "%");
        }
    }
    
    /**
     * Check garbage collection frequency
     */
    private void checkGarbageCollection() {
        try {
            // This is a simplified GC check
            // In a real implementation, you'd use GarbageCollectorMXBean
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            // Log if memory usage pattern seems problematic
            if (usedMemory > totalMemory * 0.9) {
                plugin.getLogger().info("High memory pressure detected, monitoring GC");
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Error checking GC status: " + e.getMessage());
        }
    }
    
    /**
     * Log performance metrics periodically
     */
    private void logPerformanceMetrics() {
        if (plugin.getConfig().getBoolean("debug-mode", false)) {
            Map<String, Object> stats = getPerformanceStats();
            plugin.getLogger().info("Performance Stats: " + stats);
        }
    }
    
    /**
     * Get formatted performance report
     */
    public String getPerformanceReport() {
        Map<String, Object> stats = getPerformanceStats();
        StringBuilder report = new StringBuilder();
        
        report.append("=== VCTimeRewards Performance Report ===\n");
        report.append(String.format("Heap Memory: %.1f%% used\n", (Double) stats.get("heapUsagePercent")));
        report.append(String.format("Thread Count: %d (Peak: %d)\n", 
                                   (Integer) stats.get("threadCount"), 
                                   (Integer) stats.get("peakThreadCount")));
        
        @SuppressWarnings("unchecked")
        Map<String, Double> avgTimes = (Map<String, Double>) stats.get("averageOperationTimes");
        if (!avgTimes.isEmpty()) {
            report.append("Average Operation Times:\n");
            for (Map.Entry<String, Double> entry : avgTimes.entrySet()) {
                report.append(String.format("  %s: %.2fms\n", entry.getKey(), entry.getValue()));
            }
        }
        
        String[] recommendations = getOptimizationRecommendations();
        report.append("Recommendations:\n");
        for (String recommendation : recommendations) {
            report.append("  - ").append(recommendation).append("\n");
        }
        
        return report.toString();
    }
}