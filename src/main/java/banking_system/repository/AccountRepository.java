
package banking_system.repository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import banking_system.dto.TransactionHistoryResponse;
import java.util.List;
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
        String operationUuid,
        Long accountId,
        String transactionType,
        BigDecimal amount,
        String status,
        String description) {

    String sql = """
            INSERT INTO banking_transactions
            (transaction_uuid, operation_uuid, account_id,
             transaction_type, amount, status, description)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    jdbcTemplate.update(
            sql,
            transactionUuid,
            operationUuid,
            accountId,
            transactionType,
            amount,
            status,
            description
    );
}

    public void saveAudit(
        String transactionUuid,
        Long accountId,
        String action,
        String details) {

    String sql = """
            INSERT INTO transaction_audit
            (transaction_uuid, account_id, action, details)
            VALUES (?, ?, ?, ?)
            """;

    jdbcTemplate.update(sql, transactionUuid, accountId, action, details);
}

public void saveFailedTransaction(
        String transactionUuid,
        Long accountId,
        String transactionType,
        BigDecimal amount,
        String failureReason) {

    String sql = """
            INSERT INTO failed_transactions
            (transaction_uuid, account_id, transaction_type, amount, failure_reason)
            VALUES (?, ?, ?, ?, ?)
            """;

    jdbcTemplate.update(
            sql,
            transactionUuid,
            accountId,
            transactionType,
            amount,
            failureReason
    );
}


/**
 * Atomically reserves a transaction UUID.
 * The database primary key prevents duplicate reservations.
 */
public void reserveTransactionUuid(
        String transactionUuid,
        String transactionType) {

    String sql = """
            INSERT INTO transaction_requests
            (transaction_uuid, transaction_type, status)
            VALUES (?, ?, 'PROCESSING')
            """;

    jdbcTemplate.update(
            sql,
            transactionUuid,
            transactionType
    );
}

public void markTransactionCompleted(String transactionUuid) {

    String sql = """
            UPDATE transaction_requests
            SET status = 'COMPLETED'
            WHERE transaction_uuid = ?
            """;

    int rowsUpdated = jdbcTemplate.update(sql, transactionUuid);

    if (rowsUpdated != 1) {
        throw new IllegalStateException(
                "Transaction reservation not found: " + transactionUuid
        );
    }
}

public void lockAccount(Long accountId) {

    String sql = """
            SELECT account_id
            FROM accounts
            WHERE account_id = ?
            FOR UPDATE
            """;

    var accountIds = jdbcTemplate.queryForList(
            sql,
            Long.class,
            accountId
    );

    if (accountIds.isEmpty()) {
        throw new banking_system.exception.AccountNotFoundException(
                accountId
        );
    }
}

public void reserveTransactionUuidSafely(
        String transactionUuid,
        String transactionType) {

    try {
        reserveTransactionUuid(transactionUuid, transactionType);
    } catch (org.springframework.dao.DuplicateKeyException ex) {
        throw new banking_system.exception.TransactionReservationConflictException(
                transactionUuid, ex);
    }
}

public List<TransactionHistoryResponse> getTransactionHistory(
        Long accountId,
        String transactionType,
        int page,
        int size) {

    StringBuilder sql = new StringBuilder("""
        SELECT transaction_uuid,
               operation_uuid,
               account_id,
               transaction_type,
               amount,
               status,
               description,
               created_at
        FROM banking_transactions
        WHERE account_id = ?
        """);

    List<Object> params = new java.util.ArrayList<>();
    params.add(accountId);

    if (transactionType != null && !transactionType.isBlank()) {
        sql.append(" AND transaction_type = ?");
        params.add(transactionType);
    }

    sql.append(" ORDER BY created_at DESC, transaction_id DESC");
    sql.append(" LIMIT ? OFFSET ?");

    params.add(size);
    params.add((long) page * size);

    return jdbcTemplate.query(
            sql.toString(),
            (rs, rowNum) -> {

                TransactionHistoryResponse response =
                        new TransactionHistoryResponse();

                response.setTransactionUuid(
                        rs.getString("transaction_uuid")
                );

                response.setOperationUuid(
                        rs.getString("operation_uuid")
                );

                response.setAccountId(
                        rs.getLong("account_id")
                );

                response.setTransactionType(
                        rs.getString("transaction_type")
                );

                response.setAmount(
                        rs.getBigDecimal("amount")
                );

                response.setStatus(
                        rs.getString("status")
                );

                response.setDescription(
                        rs.getString("description")
                );

                java.sql.Timestamp timestamp =
                        rs.getTimestamp("created_at");

                if (timestamp != null) {
                    response.setCreatedAt(
                            timestamp.toLocalDateTime()
                    );
                }

                return response;
            },
            params.toArray()
    );
}

public long countTransactions(
        Long accountId,
        String transactionType) {

    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(*)
        FROM banking_transactions
        WHERE account_id = ?
        """);

    List<Object> params = new java.util.ArrayList<>();
    params.add(accountId);

    if (transactionType != null && !transactionType.isBlank()) {
        sql.append(" AND transaction_type = ?");
        params.add(transactionType);
    }

    Long count = jdbcTemplate.queryForObject(
            sql.toString(),
            Long.class,
            params.toArray()
    );

    return count == null ? 0 : count;
}

public void validateActiveAccount(Long accountId) {

    List<String> statuses = jdbcTemplate.query(
            """
            SELECT status
            FROM accounts
            WHERE account_id = ?
            FOR UPDATE
            """,
            (rs, rowNum) -> rs.getString("status"),
            accountId
    );

    if (statuses.isEmpty()) {
        throw new banking_system.exception.AccountNotFoundException(
                accountId
        );
    }

    if (!"ACTIVE".equals(statuses.get(0))) {
        throw new banking_system.exception.AccountInactiveException(
                accountId
        );
    }
}

public void updateAccountStatus(
        Long accountId,
        String status) {

    int rowsUpdated = jdbcTemplate.update(
            """
            UPDATE accounts
            SET status = ?, version = version + 1
            WHERE account_id = ?
            """,
            status,
            accountId
    );

    if (rowsUpdated == 0) {
        throw new banking_system.exception.AccountNotFoundException(
                accountId
        );
    }
}


}
