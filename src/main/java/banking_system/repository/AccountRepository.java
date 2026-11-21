
package banking_system.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    // Create a new customer
    public Long createCustomer(String fullName, String email) {

        String sql = """
                INSERT INTO customers (full_name, email)
                VALUES (?, ?)
                """;

        org.springframework.jdbc.core.PreparedStatementCreator psc =
                con -> {
                    var ps = con.prepareStatement(
                            sql,
                            java.sql.Statement.RETURN_GENERATED_KEYS
                    );
                    ps.setString(1, fullName);
                    ps.setString(2, email);
                    return ps;
                };

        org.springframework.jdbc.support.KeyHolder keyHolder =
                new org.springframework.jdbc.support.GeneratedKeyHolder();

        jdbcTemplate.update(psc, keyHolder);

        return keyHolder.getKey().longValue();
    }

    // Create a bank account for a customer
    public Long createAccount(String accountNumber, Long customerId) {

        String sql = """
                INSERT INTO accounts (account_number, customer_id, balance)
                VALUES (?, ?, 0.00)
                """;

        org.springframework.jdbc.support.KeyHolder keyHolder =
                new org.springframework.jdbc.support.GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(
                    sql,
                    java.sql.Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, accountNumber);
            ps.setLong(2, customerId);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    // Get account details and balance
    public java.util.Map<String, Object> getAccountDetails(Long accountId) {

        String sql = """
                SELECT account_id, account_number, customer_id, balance, created_at
                FROM accounts
                WHERE account_id = ?
                """;

        return jdbcTemplate.queryForMap(sql, accountId);
    }

    // Deposit money
    public int deposit(Long accountId, java.math.BigDecimal amount) {
        String sql = """
                UPDATE accounts
                SET balance = balance + ?, version = version + 1
                WHERE account_id = ?
                """;

        return jdbcTemplate.update(sql, amount, accountId);
    }

    // Withdraw money only if sufficient balance exists
    public int withdraw(Long accountId, java.math.BigDecimal amount) {
        String sql = """
                UPDATE accounts
                SET balance = balance - ?, version = version + 1
                WHERE account_id = ?
                AND balance >= ?
                """;

        return jdbcTemplate.update(sql, amount, accountId, amount);
    }

    // Check whether account exists
    public boolean accountExists(Long accountId) {
        String sql = """
                SELECT COUNT(*)
                FROM accounts
                WHERE account_id = ?
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, accountId);
        return count != null && count > 0;
    }

    // Fetch current balance
    public java.math.BigDecimal getBalance(Long accountId) {
        String sql = """
                SELECT balance
                FROM accounts
                WHERE account_id = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                java.math.BigDecimal.class,
                accountId
        );
    }

    // Check idempotency key
    public boolean transactionExists(String transactionUuid) {
        String sql = """
                SELECT COUNT(*)
                FROM banking_transactions
                WHERE transaction_uuid = ?
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql, Integer.class, transactionUuid
        );

        return count != null && count > 0;
    }

    // Save transaction record
    public void saveTransaction(
            String transactionUuid,
            Long accountId,
            String transactionType,
            java.math.BigDecimal amount,
            String status,
            String description
    ) {
        String sql = """
                INSERT INTO banking_transactions
                (transaction_uuid, account_id, transaction_type,
                 amount, status, description)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                transactionUuid,
                accountId,
                transactionType,
                amount,
                status,
                description
        );
    }
}
