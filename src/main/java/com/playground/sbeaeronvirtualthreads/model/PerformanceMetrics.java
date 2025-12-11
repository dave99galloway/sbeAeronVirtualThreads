package com.playground.sbeaeronvirtualthreads.model;

/**
 * Represents performance metrics for benchmarking
 */
public record PerformanceMetrics(
    long messageCount,
    long totalBytes,
    long averageLatencyNanos,
    long minLatencyNanos,
    long maxLatencyNanos,
    String testName
) {
    public double getAverageLatencyMicros() {
        return averageLatencyNanos / 1000.0;
    }
    
    public double getThroughputMsgsPerSecond() {
        if (averageLatencyNanos == 0) return 0;
        return 1_000_000_000.0 / averageLatencyNanos;
    }
    
    public double getBandwidthMBytesPerSecond() {
        if (messageCount == 0 || averageLatencyNanos == 0) return 0;
        double avgBytesPerMsg = (double) totalBytes / messageCount;
        return (getThroughputMsgsPerSecond() * avgBytesPerMsg) / (1024 * 1024);
    }
}
