
package banking_system.benchmark;

public record BenchmarkResult(
        int workers,
        long totalTransactions,
        long successfulTransactions,
        long failedTransactions,
        double executionTimeSeconds,
        double throughput,
        double averageLatencyMs,
        double p95LatencyMs,
        double p99LatencyMs,
        double successRate
) {
}
