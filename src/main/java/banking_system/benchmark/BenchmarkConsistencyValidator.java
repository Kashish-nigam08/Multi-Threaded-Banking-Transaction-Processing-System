
package banking_system.benchmark;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile({"benchmark", "test"})
public class BenchmarkConsistencyValidator {

    private final JdbcTemplate jdbcTemplate;

    public BenchmarkConsistencyValidator(
            JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void validate(long expectedOperations) {

        Long negativeAccounts = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM accounts
                WHERE balance < 0
                """,
                Long.class
        );

        Long actualOperations = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT operation_uuid)
                FROM banking_transactions
                WHERE status = 'SUCCESS'
                """,
                Long.class
        );

        BigDecimal actualBalance = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(balance), 0)
                FROM accounts
                """,
                BigDecimal.class
        );

        BigDecimal expectedBalance = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(
                    CASE
                        WHEN transaction_type = 'DEPOSIT'
                            THEN amount
                        WHEN transaction_type = 'WITHDRAWAL'
                            THEN -amount
                        ELSE 0
                    END
                ), 0)
                FROM banking_transactions
                WHERE status = 'SUCCESS'
                """,
                BigDecimal.class
        );

        Long invalidTransfers = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM (
                    SELECT operation_uuid
                    FROM banking_transactions
                    WHERE transaction_type = 'TRANSFER'
                      AND status = 'SUCCESS'
                    GROUP BY operation_uuid
                    HAVING COUNT(*) <> 2
                       OR COUNT(DISTINCT account_id) <> 2
                       OR MIN(amount) <> MAX(amount)
                       OR operation_uuid IS NULL
                ) AS invalid_transfers
                """,
                Long.class
        );

        System.out.println("CONSISTENCY VALIDATION");
        System.out.println("Negative accounts: " + negativeAccounts);
        System.out.println("Expected operations: " + expectedOperations);
        System.out.println("Actual operations: " + actualOperations);
        System.out.println("Expected balance: " + expectedBalance);
        System.out.println("Actual balance: " + actualBalance);
        System.out.println("Invalid transfers: " + invalidTransfers);

        if (negativeAccounts != 0
                || actualOperations != expectedOperations
                || actualBalance.compareTo(expectedBalance) != 0
                || invalidTransfers != 0) {

            throw new IllegalStateException(
                    "Financial consistency validation failed"
            );
        }
        validateIndividualAccountBalances();
        System.out.println("CONSISTENCY CHECK: PASSED");
    }

public void validateIndividualAccountBalances() {

    Long invalidDirections = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM banking_transactions
            WHERE transaction_type = 'TRANSFER'
              AND status = 'SUCCESS'
              AND (
                  description IS NULL
                  OR (
                      description NOT LIKE 'Transfer to account %'
                      AND description NOT LIKE 'Transfer from account %'
                  )
              )
            """,
            Long.class
    );

    if (invalidDirections == null || invalidDirections != 0) {
        throw new IllegalStateException(
                "Unrecognized transfer directions detected"
        );
    }

    Long inconsistentAccounts = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM (
                SELECT
                    a.account_id,
                    a.balance AS actual_balance,

                    COALESCE(SUM(
                        CASE
                            WHEN t.transaction_type = 'DEPOSIT'
                                THEN t.amount

                            WHEN t.transaction_type = 'WITHDRAWAL'
                                THEN -t.amount

                            WHEN t.transaction_type = 'TRANSFER'
                                AND t.description LIKE
                                    'Transfer to account %'
                                THEN -t.amount

                            WHEN t.transaction_type = 'TRANSFER'
                                AND t.description LIKE
                                    'Transfer from account %'
                                THEN t.amount

                            ELSE 0
                        END
                    ), 0) AS expected_balance

                FROM accounts a

                LEFT JOIN banking_transactions t
                    ON a.account_id = t.account_id
                    AND t.status = 'SUCCESS'

                GROUP BY a.account_id, a.balance
            ) AS reconciliation

            WHERE actual_balance <> expected_balance
            """,
            Long.class
    );

    System.out.println(
            "Inconsistent accounts: " + inconsistentAccounts
    );

    if (inconsistentAccounts == null
            || inconsistentAccounts != 0) {

        throw new IllegalStateException(
                "Individual account balance reconciliation failed"
        );
    }

    System.out.println(
            "ACCOUNT-LEVEL RECONCILIATION: PASSED"
    );
}

}
