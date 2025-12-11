package com.playground.sbeaeronvirtualthreads.monitoring;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.OperatingSystemMXBean;

/**
 * Utility for monitoring resource usage
 */
public class ResourceMonitor {
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final OperatingSystemMXBean osBean;
    
    public ResourceMonitor() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
    }
    
    public ResourceSnapshot takeSnapshot() {
        return new ResourceSnapshot(
            memoryBean.getHeapMemoryUsage().getUsed(),
            memoryBean.getHeapMemoryUsage().getMax(),
            memoryBean.getNonHeapMemoryUsage().getUsed(),
            threadBean.getThreadCount(),
            threadBean.getPeakThreadCount(),
            osBean.getSystemLoadAverage(),
            System.nanoTime()
        );
    }
    
    public void printSnapshot(ResourceSnapshot snapshot, String label) {
        System.out.println("\n=== " + label + " ===");
        System.out.println("Heap Used: " + (snapshot.heapUsed() / 1024 / 1024) + " MB");
        System.out.println("Heap Max: " + (snapshot.heapMax() / 1024 / 1024) + " MB");
        System.out.println("Non-Heap Used: " + (snapshot.nonHeapUsed() / 1024 / 1024) + " MB");
        System.out.println("Thread Count: " + snapshot.threadCount());
        System.out.println("Peak Thread Count: " + snapshot.peakThreadCount());
        System.out.println("System Load Average: " + String.format("%.2f", snapshot.systemLoadAverage()));
    }
    
    public void printDifference(ResourceSnapshot before, ResourceSnapshot after, String label) {
        long heapDiff = after.heapUsed() - before.heapUsed();
        int threadDiff = after.threadCount() - before.threadCount();
        long durationNanos = after.timestamp() - before.timestamp();
        
        System.out.println("\n=== " + label + " - Difference ===");
        System.out.println("Heap Change: " + (heapDiff / 1024 / 1024) + " MB");
        System.out.println("Thread Count Change: " + threadDiff);
        System.out.println("Duration: " + (durationNanos / 1_000_000) + " ms");
    }
    
    public record ResourceSnapshot(
        long heapUsed,
        long heapMax,
        long nonHeapUsed,
        int threadCount,
        int peakThreadCount,
        double systemLoadAverage,
        long timestamp
    ) {}
}
