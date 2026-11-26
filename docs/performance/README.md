# Banking System Performance Analysis

## 1. Benchmark Environment

- Backend: Java, Spring Boot
- Database: MySQL with InnoDB
- Database access: Spring JdbcTemplate
- Connection pooling: HikariCP
- Benchmark accounts: 100
- Initial balance per account: 100,000
- Operations per configuration: 50,000
- Worker configurations: 1, 5, 10 and 20
- Workload: 40% deposits, 30% withdrawals and 30% transfers (configured probabilities)

## 2. Benchmark Results

| Workers | Operations | Successful | Failed | Time (s) | Throughput (ops/s) | Avg Latency (ms) | P95 (ms) | P99 (ms) |
|---|---|---|---|---|---|---|---|---|
| 1 | 50000 | 50000 | 0 | 425.97 | 117.38 | 8.52 | 11.53 | 20.91 |
| 5 | 50000 | 50000 | 0 | 123.24 | 405.71 | 12.26 | 16.96 | 57.95 |
| 10 | 50000 | 50000 | 0 | 114.71 | 435.87 | 21.84 | 48.64 | 155.84 |
| 20 | 50000 | 50000 | 0 | 77.65 | 643.91 | 29.82 | 62.04 | 127.68 |

## 3. Performance Observations

- Total operations executed: 200,000
- Successful operations: 200,000
- Failed operations: 0
- Success rate: 100%
- Maximum observed throughput: 643.91 operations per second
- Maximum observed speedup: approximately 5.49 times
- Execution time reduction: approximately 81.77%

Increasing concurrency improved overall throughput but also increased average transaction latency.

## 4. Financial Consistency

All four benchmark configurations passed aggregate financial consistency validation.

- Negative account balances: 0
- Expected operations per run, including initial deposits: 50,100
- Actual operations per run: 50,100
- Expected total balance: 11,259,227.00
- Actual total balance: 11,259,227.00
- Balance discrepancy: 0.00
- Invalid transfers detected: 0

## 5. Account-Level Reconciliation

An additional benchmark was executed after implementing individual account balance reconciliation.

- Workers: 20
- Operations: 5,000
- Successful operations: 5,000
- Failed operations: 0
- Execution time: 9.36 seconds
- Throughput: 533.98 operations per second
- Inconsistent accounts: 0
- Expected balance: 10,111,123.00
- Actual balance: 10,111,123.00
- Reconciliation status: PASSED

Manual corruption detection and automated financial integrity tests were also implemented.

## 6. Database Optimization

The database optimization strategy includes:

- Composite indexes for account transaction history.
- Indexes for filtered transaction history.
- Indexes for successful operation reporting.
- Indexes for failed transaction reporting.
- SQL execution plan analysis using EXPLAIN and EXPLAIN ANALYZE.
- Evaluation of an experimental covering index for reporting queries.

The covering index requires further performance evaluation before adoption across all database environments.

## 7. Concurrency and Reliability

The system implements:

- Transactional deposits, withdrawals and transfers.
- Transaction UUID-based idempotency.
- Deterministic account locking.
- Atomic balance updates.
- Failed transaction recording.
- Transaction audit logging.
- Exclusive benchmark execution using MySQL advisory locks.
- Aggregate and individual account balance reconciliation.

## 8. Benchmark Limitations

- Benchmarks measure service-layer performance rather than HTTP request performance.
- Results are specific to the local hardware and database configuration.
- Dedicated warm-up runs and repeated statistical trials were not performed.
- Additional indexes may increase storage and transaction write costs.
- Account reconciliation currently relies on ledger transfer descriptions.

## 9. Conclusion

The Banking Transaction Processing System demonstrated successful concurrent processing of 200,000 operations across four benchmark configurations without transaction failures or detected aggregate financial inconsistencies.

Additional account-level reconciliation testing demonstrated that individual account balances matched their transaction histories under the tested workload.
