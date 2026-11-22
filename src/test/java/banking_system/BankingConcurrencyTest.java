
package banking_system;

import banking_system.dto.AccountRequest;
import banking_system.dto.TransactionRequest;
import banking_system.exception.DuplicateTransactionException;
import banking_system.exception.InsufficientBalanceException;
import banking_system.service.BankingService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import banking_system.dto.TransferRequest;
import banking_system.repository.AccountRepository;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@ActiveProfiles("test")
class BankingConcurrencyTest {

    @Autowired
    private BankingService bankingService;
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

private Long createTestAccount() {

    String uniqueId = UUID.randomUUID()
            .toString()
            .replace("-", "");

    AccountRequest request = new AccountRequest();

    request.setFullName("Concurrency Test");

    request.setEmail(
            "test-" + uniqueId + "@example.com"
    );

    // Generate a short, unique test account number
    request.setAccountNumber(
            "T" + uniqueId.substring(0, 12)
    );

    return bankingService.createAccount(request);
}


    private TransactionRequest transactionRequest(
            Long accountId,
            BigDecimal amount,
            String uuid) {

        TransactionRequest request = new TransactionRequest();
        request.setAccountId(accountId);
        request.setAmount(amount);
        request.setTransactionUuid(uuid);

        return request;
    }

    @Test
    void concurrentWithdrawalsPreventOverdraft() throws Exception {

        Long accountId = createTestAccount();

        bankingService.deposit(transactionRequest(
                accountId,
                new BigDecimal("1000.00"),
                UUID.randomUUID().toString()
        ));

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger insufficientFunds = new AtomicInteger();

        try {
            Callable<Void> task = () -> {
                ready.countDown();
                start.await();

                try {
                    bankingService.withdraw(transactionRequest(
                            accountId,
                            new BigDecimal("700.00"),
                            UUID.randomUUID().toString()
                    ));
                    successes.incrementAndGet();

                } catch (InsufficientBalanceException ex) {
                    insufficientFunds.incrementAndGet();
                }

                return null;
            };

            Future<Void> first = executor.submit(task);
            Future<Void> second = executor.submit(task);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);

            assertEquals(1, successes.get());
            assertEquals(1, insufficientFunds.get());

            assertEquals(
                    0,
                    bankingService.getBalance(accountId)
                            .compareTo(new BigDecimal("300.00"))
            );

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void duplicateTransactionUuidAllowsOnlyOneDeposit()
            throws Exception {

        Long accountId = createTestAccount();
        String sharedUuid = UUID.randomUUID().toString();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger duplicates = new AtomicInteger();

        try {
            Callable<Void> task = () -> {
                ready.countDown();
                start.await();

                try {
                    bankingService.deposit(transactionRequest(
                            accountId,
                            new BigDecimal("500.00"),
                            sharedUuid
                    ));
                    successes.incrementAndGet();

                } catch (DuplicateTransactionException ex) {
                    duplicates.incrementAndGet();
                }

                return null;
            };

            Future<Void> first = executor.submit(task);
            Future<Void> second = executor.submit(task);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);

            assertEquals(1, successes.get());
            assertEquals(1, duplicates.get());

            assertEquals(
                    0,
                    bankingService.getBalance(accountId)
                            .compareTo(new BigDecimal("500.00"))
            );

        } finally {
            executor.shutdownNow();
        }
    }

@Test
void concurrentDepositsDoNotLoseUpdates() throws Exception {

    Long accountId = createTestAccount();

    int numberOfDeposits = 20;
    BigDecimal depositAmount = new BigDecimal("100.00");

    ExecutorService executor =
            Executors.newFixedThreadPool(10);

    CountDownLatch ready = new CountDownLatch(10);
    CountDownLatch start = new CountDownLatch(1);

    try {
        java.util.List<Future<?>> futures =
                new java.util.ArrayList<>();

        for (int i = 0; i < numberOfDeposits; i++) {

            futures.add(executor.submit(() -> {

                ready.countDown();
                start.await();

                bankingService.deposit(transactionRequest(
                        accountId,
                        depositAmount,
                        UUID.randomUUID().toString()
                ));

                return null;
            }));
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();

        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }

        assertEquals(
                0,
                bankingService.getBalance(accountId)
                        .compareTo(new BigDecimal("2000.00"))
        );

    } finally {
        start.countDown();
        executor.shutdownNow();
    }
}

private TransferRequest transferRequest(
        Long sender,
        Long receiver,
        BigDecimal amount,
        String uuid) {

    TransferRequest request = new TransferRequest();

    request.setSenderAccountId(sender);
    request.setReceiverAccountId(receiver);
    request.setAmount(amount);
    request.setTransactionUuid(uuid);

    return request;
}

@Test
void oppositeDirectionTransfersPreserveMoney()
        throws Exception {

    Long accountA = createTestAccount();
    Long accountB = createTestAccount();

    bankingService.deposit(transactionRequest(
            accountA,
            new BigDecimal("1000.00"),
            UUID.randomUUID().toString()
    ));

    bankingService.deposit(transactionRequest(
            accountB,
            new BigDecimal("1000.00"),
            UUID.randomUUID().toString()
    ));

    ExecutorService executor =
            Executors.newFixedThreadPool(2);

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    String uuid1 = UUID.randomUUID().toString();
    String uuid2 = UUID.randomUUID().toString();

    try {
        Future<?> first = executor.submit(() -> {
            ready.countDown();
            start.await();

            bankingService.transfer(transferRequest(
                    accountA,
                    accountB,
                    new BigDecimal("300.00"),
                    uuid1
            ));

            return null;
        });

        Future<?> second = executor.submit(() -> {
            ready.countDown();
            start.await();

            bankingService.transfer(transferRequest(
                    accountB,
                    accountA,
                    new BigDecimal("200.00"),
                    uuid2
            ));

            return null;
        });

        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();

        first.get(30, TimeUnit.SECONDS);
        second.get(30, TimeUnit.SECONDS);

        BigDecimal balanceA =
                bankingService.getBalance(accountA);

        BigDecimal balanceB =
                bankingService.getBalance(accountB);

        assertEquals(
                0,
                balanceA.compareTo(new BigDecimal("900.00"))
        );

        assertEquals(
                0,
                balanceB.compareTo(new BigDecimal("1100.00"))
        );

        assertEquals(
                0,
                balanceA.add(balanceB)
                        .compareTo(new BigDecimal("2000.00"))
        );

    } finally {
        start.countDown();
        executor.shutdownNow();
    }
}

@Test
void successfulTransferCreatesTwoLinkedLedgerEntries() {

    Long sender = createTestAccount();
    Long receiver = createTestAccount();

    bankingService.deposit(transactionRequest(
            sender,
            new BigDecimal("500.00"),
            UUID.randomUUID().toString()
    ));

    String operationUuid = UUID.randomUUID().toString();

    bankingService.transfer(transferRequest(
            sender,
            receiver,
            new BigDecimal("150.00"),
            operationUuid
    ));

    Integer ledgerCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM banking_transactions
            WHERE operation_uuid = ?
            """,
            Integer.class,
            operationUuid
    );

    Integer distinctEntryCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(DISTINCT transaction_uuid)
            FROM banking_transactions
            WHERE operation_uuid = ?
            """,
            Integer.class,
            operationUuid
    );

    assertEquals(2, ledgerCount);
    assertEquals(2, distinctEntryCount);

    assertEquals(
            0,
            bankingService.getBalance(sender)
                    .compareTo(new BigDecimal("350.00"))
    );

    assertEquals(
            0,
            bankingService.getBalance(receiver)
                    .compareTo(new BigDecimal("150.00"))
    );
}

@Test
void failedTransferRollsBackAndRecordsFailure() {

    Long sender = createTestAccount();
    Long receiver = createTestAccount();

    bankingService.deposit(transactionRequest(
            sender,
            new BigDecimal("500.00"),
            UUID.randomUUID().toString()
    ));

    String operationUuid = UUID.randomUUID().toString();

    assertThrows(
            InsufficientBalanceException.class,
            () -> bankingService.transfer(transferRequest(
                    sender,
                    receiver,
                    new BigDecimal("800.00"),
                    operationUuid
            ))
    );

    // Sender's money must remain unchanged.
    assertEquals(
            0,
            bankingService.getBalance(sender)
                    .compareTo(new BigDecimal("500.00"))
    );

    // Receiver must not receive any money.
    assertEquals(
            0,
            bankingService.getBalance(receiver)
                    .compareTo(BigDecimal.ZERO)
    );

    // The failed transfer must not create ledger entries.
    Integer ledgerCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM banking_transactions
            WHERE operation_uuid = ?
            """,
            Integer.class,
            operationUuid
    );

    assertEquals(0, ledgerCount);

    // The transaction reservation must be rolled back.
    Integer reservationCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM transaction_requests
            WHERE transaction_uuid = ?
            """,
            Integer.class,
            operationUuid
    );

    assertEquals(0, reservationCount);

    // Failure record must survive the rollback.
    Integer failureCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM failed_transactions
            WHERE transaction_uuid = ?
            """,
            Integer.class,
            operationUuid
    );

    assertEquals(1, failureCount);
}

@Test
void failedWithdrawalCanBeRetriedWithSameUuid() {

    Long accountId = createTestAccount();

    String operationUuid = UUID.randomUUID().toString();

    // First attempt: insufficient balance.
    assertThrows(
            InsufficientBalanceException.class,
            () -> bankingService.withdraw(transactionRequest(
                    accountId,
                    new BigDecimal("300.00"),
                    operationUuid
            ))
    );

    // Add sufficient money using a different UUID.
    bankingService.deposit(transactionRequest(
            accountId,
            new BigDecimal("500.00"),
            UUID.randomUUID().toString()
    ));

    // Retry with the original UUID.
    bankingService.withdraw(transactionRequest(
            accountId,
            new BigDecimal("300.00"),
            operationUuid
    ));

    assertEquals(
            0,
            bankingService.getBalance(accountId)
                    .compareTo(new BigDecimal("200.00"))
    );

    // A completed UUID must not be processed again.
    assertThrows(
            DuplicateTransactionException.class,
            () -> bankingService.withdraw(transactionRequest(
                    accountId,
                    new BigDecimal("300.00"),
                    operationUuid
            ))
    );

    // Exactly one successful withdrawal ledger entry.
    Integer ledgerCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM banking_transactions
            WHERE operation_uuid = ?
            """,
            Integer.class,
            operationUuid
    );

    assertEquals(1, ledgerCount);

    // The original failed attempt should still be recorded.
    Integer failureCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM failed_transactions
            WHERE transaction_uuid = ?
            """,
            Integer.class,
            operationUuid
    );

    assertEquals(1, failureCount);
}

}
