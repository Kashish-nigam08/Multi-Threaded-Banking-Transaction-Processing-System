
package banking_system.benchmark;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
@Profile("benchmark")
public class BenchmarkReportExporter {

    public void export(List<BenchmarkResult> results)
            throws IOException {

        Path directory = Path.of("docs", "performance");

        Files.createDirectories(directory);

        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
        );

        Path file = directory.resolve(
                "benchmark-results-" + timestamp + ".csv"
        );

        StringBuilder csv = new StringBuilder();

        csv.append(
                "Workers,TotalOperations,Successful,Failed,"
                + "ExecutionTimeSeconds,Throughput,"
                + "AverageLatencyMs,P95LatencyMs,"
                + "P99LatencyMs,SuccessRate\n"
        );

        for (BenchmarkResult result : results) {

            csv.append(String.format(
                    Locale.US,
                    "%d,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f%n",
                    result.workers(),
                    result.totalTransactions(),
                    result.successfulTransactions(),
                    result.failedTransactions(),
                    result.executionTimeSeconds(),
                    result.throughput(),
                    result.averageLatencyMs(),
                    result.p95LatencyMs(),
                    result.p99LatencyMs(),
                    result.successRate()
            ));
        }

        Files.writeString(file, csv.toString());

        System.out.println(
                "Benchmark report saved: "
                + file.toAbsolutePath()
        );
    }
}
