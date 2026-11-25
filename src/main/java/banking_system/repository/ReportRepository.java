
package banking_system.repository;
import banking_system.dto.DailyTransactionResponse;
import java.time.LocalDate;
import banking_system.dto.TransactionSummaryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import banking_system.dto.AccountReportResponse;
import banking_system.dto.FailedTransactionReportResponse;
import banking_system.dto.BankingOverviewResponse;
import java.util.List;

@Repository
public class ReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TransactionSummaryResponse> getTransactionSummary() {

        String sql = """
                SELECT
                    transaction_type,
                    COUNT(*) AS total_operations,
                    SUM(amount) AS total_amount,
                    ROUND(AVG(amount), 2) AS average_amount,
                    MIN(amount) AS minimum_amount,
                    MAX(amount) AS maximum_amount
                FROM (
                    SELECT
                        operation_uuid,
                        transaction_type,
                        MAX(amount) AS amount
                    FROM banking_transactions
                    WHERE status = 'SUCCESS'
                      AND operation_uuid IS NOT NULL
                      AND TRIM(operation_uuid) <> ''
                    GROUP BY operation_uuid, transaction_type
                ) AS operations
                GROUP BY transaction_type
                ORDER BY transaction_type
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new TransactionSummaryResponse(
                        rs.getString("transaction_type"),
                        rs.getLong("total_operations"),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("average_amount"),
                        rs.getBigDecimal("minimum_amount"),
                        rs.getBigDecimal("maximum_amount")
                )
        );
    }

public List<DailyTransactionResponse> getDailyTransactions(
        LocalDate startDate,
        LocalDate endDate) {

    String sql = """
            SELECT
                DATE(operation_time) AS transaction_date,
                transaction_type,
                COUNT(*) AS total_operations,
                SUM(amount) AS total_amount
            FROM (
                SELECT
                    operation_uuid,
                    transaction_type,
                    MIN(created_at) AS operation_time,
                    MAX(amount) AS amount
                FROM banking_transactions
                WHERE status = 'SUCCESS'
                  AND operation_uuid IS NOT NULL
                  AND TRIM(operation_uuid) <> ''
                GROUP BY operation_uuid, transaction_type
            ) AS operations
            WHERE operation_time >= ?
              AND operation_time < ?
            GROUP BY DATE(operation_time), transaction_type
            ORDER BY transaction_date DESC, transaction_type
            """;

    return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new DailyTransactionResponse(
                    rs.getDate("transaction_date").toLocalDate(),
                    rs.getString("transaction_type"),
                    rs.getLong("total_operations"),
                    rs.getBigDecimal("total_amount")
            ),
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
    );
}

public AccountReportResponse getAccountReport(Long accountId) {

    String sql = """
            SELECT
                a.account_id,
                a.status AS account_status,
                a.balance AS current_balance,
                COUNT(t.transaction_id) AS total_transactions,

                COALESCE(SUM(CASE
                    WHEN t.transaction_type = 'DEPOSIT'
                    THEN t.amount ELSE 0
                END), 0) AS total_deposits,

                COALESCE(SUM(CASE
                    WHEN t.transaction_type = 'WITHDRAWAL'
                    THEN t.amount ELSE 0
                END), 0) AS total_withdrawals,

                COALESCE(SUM(CASE
                    WHEN t.transaction_type = 'TRANSFER'
                    AND t.description LIKE 'Transfer to account %'
                    THEN t.amount ELSE 0
                END), 0) AS total_transfers_sent,

                COALESCE(SUM(CASE
                    WHEN t.transaction_type = 'TRANSFER'
                    AND t.description LIKE 'Transfer from account %'
                    THEN t.amount ELSE 0
                END), 0) AS total_transfers_received

            FROM accounts a
            LEFT JOIN banking_transactions t
                ON a.account_id = t.account_id
                AND t.status = 'SUCCESS'

            WHERE a.account_id = ?

            GROUP BY a.account_id, a.status, a.balance
            """;

    return jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> new AccountReportResponse(
                    rs.getLong("account_id"),
                    rs.getString("account_status"),
                    rs.getBigDecimal("current_balance"),
                    rs.getLong("total_transactions"),
                    rs.getBigDecimal("total_deposits"),
                    rs.getBigDecimal("total_withdrawals"),
                    rs.getBigDecimal("total_transfers_sent"),
                    rs.getBigDecimal("total_transfers_received")
            ),
            accountId
    );
}

public List<FailedTransactionReportResponse>
        getFailedTransactionReport() {

    String sql = """
            SELECT
                transaction_type,
                COUNT(*) AS total_failures
            FROM failed_transactions
            GROUP BY transaction_type
            ORDER BY total_failures DESC
            """;

    return jdbcTemplate.query(
            sql,
            (rs, rowNum) ->
                    new FailedTransactionReportResponse(
                            rs.getString("transaction_type"),
                            rs.getLong("total_failures")
                    )
    );
}

public BankingOverviewResponse getBankingOverview() {

    String sql = """
            SELECT
                (SELECT COUNT(*) FROM customers)
                    AS total_customers,

                (SELECT COUNT(*) FROM accounts)
                    AS total_accounts,

                (SELECT COUNT(*) FROM accounts
                    WHERE status = 'ACTIVE')
                    AS active_accounts,

                (SELECT COUNT(*) FROM accounts
                    WHERE status = 'INACTIVE')
                    AS inactive_accounts,

                (SELECT COALESCE(SUM(balance), 0)
                    FROM accounts)
                    AS total_balance,

                (SELECT COUNT(DISTINCT operation_uuid)
                    FROM banking_transactions
                    WHERE status = 'SUCCESS'
                    AND operation_uuid IS NOT NULL
                    AND TRIM(operation_uuid) <> '')
                    AS successful_operations,

                (SELECT COUNT(*) FROM failed_transactions)
                    AS failed_attempts
            """;

    return jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> new BankingOverviewResponse(
                    rs.getLong("total_customers"),
                    rs.getLong("total_accounts"),
                    rs.getLong("active_accounts"),
                    rs.getLong("inactive_accounts"),
                    rs.getBigDecimal("total_balance"),
                    rs.getLong("successful_operations"),
                    rs.getLong("failed_attempts")
            )
    );
}


}
