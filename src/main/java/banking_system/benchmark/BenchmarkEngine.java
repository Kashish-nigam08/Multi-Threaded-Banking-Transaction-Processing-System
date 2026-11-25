
package banking_system.benchmark;

import banking_system.dto.TransactionRequest;
import banking_system.dto.TransferRequest;
import banking_system.service.BankingService;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Profile("benchmark")
public class BenchmarkEngine {

    private final BankingService bankingService;

    public BenchmarkEngine(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    public BenchmarkResult execute(
            List<Long> accountIds,
            int totalTransactions,
            int workers) throws InterruptedException {

        if (accountIds.size() < 2
                || totalTransactions <= 0
                || workers <= 0) {

            throw new IllegalArgumentException(
                    "Invalid benchmark configuration"
            );
        }

        ExecutorService executor =
                Executors.newFixedThreadPool(workers);

        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(workers);

        AtomicLong successful = new AtomicLong();
        AtomicLong failed = new AtomicLong();
        AtomicLong completed = new AtomicLong();
        long[] latencies = new long[totalTransactions];

        // Generate an identical workload for each run.
        Random random = new Random(42);

        int[] transactionTypes = new int[totalTransactions];
        int[] senders = new int[totalTransactions];
        int[] receivers = new int[totalTransactions];
        BigDecimal[] amounts = new BigDecimal[totalTransactions];
        String[] uuids = new String[totalTransactions];

        for (int i = 0; i < totalTransactions; i++) {

            transactionTypes[i] = random.nextInt(100);

            senders[i] = random.nextInt(accountIds.size());

            do {
                receivers[i] = random.nextInt(accountIds.size());
            } while (senders[i] == receivers[i]);

            amounts[i] = BigDecimal.valueOf(
                    random.nextInt(500) + 1
            );

            uuids[i] = UUID.randomUUID().toString();
        }

        try {

            for (int worker = 0; worker < workers; worker++) {

                final int workerId = worker;

                executor.submit(() -> {

                    ready.countDown();

                    try {

                        start.await();

                        for (int i = workerId;
                             i < totalTransactions;
                             i += workers) {

                            long begin = System.nanoTime();

                            try {

                                executeTransaction(
                                        transactionTypes[i],
                                        accountIds.get(senders[i]),
                                        accountIds.get(receivers[i]),
                                        amounts[i],
                                        uuids[i]
                                );

                                successful.incrementAndGet();

                            } catch (Exception ex) {

                                long failures =
                                        failed.incrementAndGet();

                                if (failures <= 5) {
                                    System.err.println(
                                            "Transaction failed: "
                                            + ex.getClass().getSimpleName()
                                            + " - " + ex.getMessage()
                                    );
                                }

                            } finally {

    latencies[i] =
            System.nanoTime() - begin;

    long count = completed.incrementAndGet();

    if (count % 5000 == 0
            || count == totalTransactions) {

        System.out.printf(
                "Progress: %d/%d operations (%.1f%%)%n",
                count,
                totalTransactions,
                count * 100.0 / totalTransactions
        );
    }
}
}
}
 catch (InterruptedException ex) {

                        Thread.currentThread().interrupt();

                    } finally {

                        finished.countDown();
                    }
                });
            }

            ready.await();

            long benchmarkStart = System.nanoTime();

            start.countDown();

            finished.await();

            long benchmarkEnd = System.nanoTime();

            if (successful.get() + failed.get()
                    != totalTransactions) {

                throw new IllegalStateException(
                        "Benchmark workload did not complete"
                );
            }

            double executionTime =
                    (benchmarkEnd - benchmarkStart)
                            / 1_000_000_000.0;

            Arrays.sort(latencies);

            double averageLatency =
                    Arrays.stream(latencies)
                            .average()
                            .orElse(0)
                            / 1_000_000.0;

            return new BenchmarkResult(
                    workers,
                    totalTransactions,
                    successful.get(),
                    failed.get(),
                    executionTime,
                    totalTransactions / executionTime,
                    averageLatency,
                    percentile(latencies, 0.95),
                    percentile(latencies, 0.99),
                    successful.get() * 100.0
                            / totalTransactions
            );

        } finally {

            executor.shutdownNow();
        }
    }

    private void executeTransaction(
            int type,
            Long sender,
            Long receiver,
            BigDecimal amount,
            String uuid) {

        if (type < 40) {

            TransactionRequest request =
                    new TransactionRequest();

            request.setAccountId(sender);
            request.setAmount(amount);
            request.setTransactionUuid(uuid);

            bankingService.deposit(request);

        } else if (type < 70) {

            TransactionRequest request =
                    new TransactionRequest();

            request.setAccountId(sender);
            request.setAmount(amount);
            request.setTransactionUuid(uuid);

            bankingService.withdraw(request);

        } else {

            TransferRequest request =
                    new TransferRequest();

            request.setSenderAccountId(sender);
            request.setReceiverAccountId(receiver);
            request.setAmount(amount);
            request.setTransactionUuid(uuid);

            bankingService.transfer(request);
        }
    }

    private double percentile(long[] values, double percentile) {

        int index = (int) Math.ceil(
                percentile * values.length
        ) - 1;

        index = Math.max(
                0, Math.min(index, values.length - 1)
        );

        return values[index] / 1_000_000.0;
    }
}
