# Database Setup

The Banking Transaction Processing System uses MySQL with three isolated database environments.

## Database Environments

| Database | Purpose |
|---|---|
| banking_system | Application development |
| banking_system_test | Automated integration testing |
| banking_system_benchmark | Concurrent performance benchmarking |

## Database Tables

- customers
- accounts
- banking_transactions
- transaction_requests
- transaction_audit
- failed_transactions

## Installation

1. Install MySQL Server and MySQL Workbench.
2. Execute `setup.sql` to create the database environments.
3. Select each newly created, empty database.
4. Execute `schema.sql` separately in each database.
5. Execute `views/account_reconciliation_view.sql` in the required database.
6. Configure the Spring Boot datasource credentials.

## Performance Indexes

Existing performance indexes are included in the exported schema. The `indexes.sql` file documents candidate and expected performance indexes.

Verify existing indexes before creating additional ones.

## Security

Database passwords must not be committed to version control. Configure credentials through environment variables.

## Safety

- Never execute benchmark workloads against the development database.
- Never execute automated corruption tests against production data.
- Back up existing databases before making schema changes.
- The schema initialization script is intended for empty databases.
- Benchmark execution resets the benchmark database before each workload.
