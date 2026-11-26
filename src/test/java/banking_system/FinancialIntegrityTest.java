
package banking_system;

import banking_system.benchmark.BenchmarkConsistencyValidator;
import banking_system.dto.AccountRequest;
import banking_system.dto.TransactionRequest;
import banking_system.dto.TransferRequest;
import banking_system.service.BankingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FinancialIntegrityTest {

    @Autowired
    private BankingService bankingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BenchmarkConsistencyValidator validator;

    @BeforeEach
    void verifyTestDatabase() {

        String database = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class
        );

        assertEquals("banking_system_test", database);
    }

    @Test
    void correctAccountBalancePassesReconciliation() {

        Long accountId = createAccount();

        deposit(accountId, "1000.00");

        assertDoesNotThrow(
                validator::validateIndividualAccountBalances
        );
    }

    @Test
    void incorrectAccountBalanceIsDetected() {

        Long accountId = createAccount();

        deposit(accountId, "1000.00");

        jdbcTemplate.update(
                """
                UPDATE accounts
                SET balance = balance + 500.00
                WHERE account_id = ?
                """,
                accountId
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                validator::validateIndividualAccountBalances
        );

        assertEquals(
                "Individual account balance reconciliation failed",
                exception.getMessage()
        );
    }

    @Test
    void offsettingBalanceDiscrepanciesAreDetected() {

        Long accountA = createAccount();
        Long accountB = createAccount();

        deposit(accountA, "1000.00");
        deposit(accountB, "1000.00");

        BigDecimal balanceBefore =
                jdbcTemplate.queryForObject(
                        "SELECT SUM(balance) FROM accounts",
                        BigDecimal.class
                );

        jdbcTemplate.update(
                """
                UPDATE accounts
                SET balance = balance + 500.00
                WHERE account_id = ?
                """,
                accountA
        );

        jdbcTemplate.update(
                """
                UPDATE accounts
                SET balance = balance - 500.00
                WHERE account_id = ?
                """,
                accountB
        );

        BigDecimal balanceAfter =
                jdbcTemplate.queryForObject(
                        "SELECT SUM(balance) FROM accounts",
                        BigDecimal.class
                );

        assertNotNull(balanceBefore);
        assertNotNull(balanceAfter);

        assertEquals(
                0,
                balanceBefore.compareTo(balanceAfter)
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                validator::validateIndividualAccountBalances
        );

        assertEquals(
                "Individual account balance reconciliation failed",
                exception.getMessage()
        );
    }

    @Test
    void successfulTransferPassesReconciliation() {

        Long sender = createAccount();
        Long receiver = createAccount();

        deposit(sender, "1000.00");

        transfer(sender, receiver, "300.00");

        assertDoesNotThrow(
                validator::validateIndividualAccountBalances
        );
    }

    @Test
    void invalidTransferDirectionIsDetected() {

        Long sender = createAccount();
        Long receiver = createAccount();

        deposit(sender, "1000.00");

        String operationUuid =
                transfer(sender, receiver, "300.00");

        jdbcTemplate.update(
                """
                UPDATE banking_transactions
                SET description = 'Invalid transfer direction'
                WHERE operation_uuid = ?
                  AND account_id = ?
                """,
                operationUuid,
                sender
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                validator::validateIndividualAccountBalances
        );

        assertEquals(
                "Unrecognized transfer directions detected",
                exception.getMessage()
        );
    }

    private Long createAccount() {

        String uniqueId = UUID.randomUUID()
                .toString()
                .replace("-", "");

        AccountRequest request = new AccountRequest();

        request.setFullName("Financial Integrity Test");
        request.setEmail(
                "integrity-" + uniqueId + "@example.com"
        );
        request.setAccountNumber(
                "F" + uniqueId.substring(0, 12)
        );

        return bankingService.createAccount(request);
    }

    private void deposit(Long accountId, String amount) {

        TransactionRequest request =
                new TransactionRequest();

        request.setAccountId(accountId);
        request.setAmount(new BigDecimal(amount));
        request.setTransactionUuid(
                UUID.randomUUID().toString()
        );

        bankingService.deposit(request);
    }

    private String transfer(
            Long sender,
            Long receiver,
            String amount) {

        String operationUuid =
                UUID.randomUUID().toString();

        TransferRequest request = new TransferRequest();

        request.setSenderAccountId(sender);
        request.setReceiverAccountId(receiver);
        request.setAmount(new BigDecimal(amount));
        request.setTransactionUuid(operationUuid);

        bankingService.transfer(request);

        return operationUuid;
    }
}
