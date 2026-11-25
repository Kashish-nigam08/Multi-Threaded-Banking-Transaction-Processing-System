
package banking_system.benchmark;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("benchmark")
@ConditionalOnProperty(
        name = "benchmark.enabled",
        havingValue = "true"
)
public class BenchmarkRunner implements CommandLineRunner {

    private final BenchmarkEngine benchmarkEngine;
    private final BenchmarkAccountGenerator accountGenerator;
    private final BenchmarkDataReset dataReset;
    private final BenchmarkConsistencyValidator validator;
    private final BenchmarkReportExporter exporter;
    private final JdbcTemplate jdbcTemplate;
    private final BenchmarkExecutionLock executionLock;
    @Value("${benchmark.accounts:100}")
    private int numberOfAccounts;

    @Value("${benchmark.transactions:50000}")
    private int totalTransactions;

    @Value("${benchmark.workers:1,5,10,20}")
    private String workerConfiguration;

    public BenchmarkRunner(
        BenchmarkEngine benchmarkEngine,
        BenchmarkAccountGenerator accountGenerator,
        BenchmarkDataReset dataReset,
        BenchmarkConsistencyValidator validator,
        BenchmarkReportExporter exporter,
        JdbcTemplate jdbcTemplate,
        BenchmarkExecutionLock executionLock) {

    this.benchmarkEngine = benchmarkEngine;
    this.accountGenerator = accountGenerator;
    this.dataReset = dataReset;
    this.validator = validator;
    this.exporter = exporter;
    this.jdbcTemplate = jdbcTemplate;
    this.executionLock = executionLock;
}

    @Override
public void run(String... args) throws Exception {

    executionLock.executeWithLock(this::runBenchmark);
}

private void runBenchmark() throws Exception {

        String database = jdbcTemplate.queryForObject(
                "SELECT DATABASE()", String.class
        );

        if (!"banking_system_benchmark".equals(database)) {
            throw new IllegalStateException(
                    "Invalid benchmark database"
            );
        }

        List<BenchmarkResult> results = new ArrayList<>();

        for (String value : workerConfiguration.split(",")) {

            int workers = Integer.parseInt(value.trim());

            if (workers <= 0 || numberOfAccounts < 2
                    || totalTransactions <= 0) {
                throw new IllegalArgumentException(
                        "Invalid benchmark configuration"
                );
            }

            System.out.println("==============================");
            System.out.println("Starting benchmark");
            System.out.println("Workers: " + workers);
            System.out.println("Operations: " + totalTransactions);
            System.out.println("==============================");

            dataReset.reset();

            List<Long> accountIds =
                    accountGenerator.generateAccounts(
                            numberOfAccounts
                    );
            System.out.println(
        "Accounts generated: " + accountIds.size()
);

System.out.println(
        "Executing " + totalTransactions + " operations..."
);

            BenchmarkResult result = benchmarkEngine.execute(
                    accountIds,
                    totalTransactions,
                    workers
            );

            validator.validate(
                    numberOfAccounts
                    + result.successfulTransactions()
            );

            results.add(result);

            printResult(result);

            exporter.export(results);
        }

        System.out.println("ALL BENCHMARKS COMPLETED");
    }

    private void printResult(BenchmarkResult result) {

        System.out.println("==============================");
        System.out.println("BENCHMARK RESULTS");
        System.out.println("==============================");

        System.out.println("Workers: " + result.workers());
        System.out.println("Total: " + result.totalTransactions());
        System.out.println("Successful: " + result.successfulTransactions());
        System.out.println("Failed: " + result.failedTransactions());

        System.out.printf(
                "Execution time: %.2f seconds%n",
                result.executionTimeSeconds()
        );

        System.out.printf(
                "Throughput: %.2f operations/sec%n",
                result.throughput()
        );

        System.out.printf(
                "Average latency: %.2f ms%n",
                result.averageLatencyMs()
        );

        System.out.printf(
                "P95 latency: %.2f ms%n",
                result.p95LatencyMs()
        );

        System.out.printf(
                "P99 latency: %.2f ms%n",
                result.p99LatencyMs()
        );

        System.out.printf(
                "Success rate: %.2f%%%n",
                result.successRate()
        );
    }
}
