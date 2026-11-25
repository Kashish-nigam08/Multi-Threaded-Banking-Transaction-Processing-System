
package banking_system;

import banking_system.dto.AccountRequest;
import banking_system.dto.TransactionRequest;
import banking_system.dto.TransferRequest;
import banking_system.exception.AccountNotFoundException;
import banking_system.service.BankingService;
import banking_system.service.ReportService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ReportServiceTest {

    @Autowired
    private BankingService bankingService;

    @Autowired
    private ReportService reportService;

    private Long createTestAccount() {

        String uniqueId = UUID.randomUUID()
                .toString()
                .replace("-", "");

        AccountRequest request = new AccountRequest();

        request.setFullName("Reporting Test");
        request.setEmail(
                "report-" + uniqueId + "@example.com"
        );

        request.setAccountNumber(
                "R" + uniqueId.substring(0, 12)
        );

        return bankingService.createAccount(request);
    }

    private TransactionRequest transactionRequest(
            Long accountId, String amount) {

        TransactionRequest request = new TransactionRequest();

        request.setAccountId(accountId);
        request.setAmount(new BigDecimal(amount));
        request.setTransactionUuid(
                UUID.randomUUID().toString()
        );

        return request;
    }

    @Test
    void accountReportShouldCalculateCorrectTotals() {

        Long accountId = createTestAccount();

        bankingService.deposit(
                transactionRequest(accountId, "1000.00")
        );

        bankingService.withdraw(
                transactionRequest(accountId, "300.00")
        );

        var report = reportService.getAccountReport(accountId);

        assertEquals(accountId, report.accountId());
        assertEquals(2, report.totalTransactions());

        assertEquals(0, report.totalDeposits()
                .compareTo(new BigDecimal("1000.00")));

        assertEquals(0, report.totalWithdrawals()
                .compareTo(new BigDecimal("300.00")));

        assertEquals(0, report.currentBalance()
                .compareTo(new BigDecimal("700.00")));
    }

    @Test
    void emptyAccountReportShouldReturnZeroTotals() {

        Long accountId = createTestAccount();

        var report = reportService.getAccountReport(accountId);

        assertEquals(0, report.totalTransactions());

        assertEquals(0, report.totalDeposits()
                .compareTo(BigDecimal.ZERO));

        assertEquals(0, report.totalWithdrawals()
                .compareTo(BigDecimal.ZERO));

        assertEquals(0, report.currentBalance()
                .compareTo(BigDecimal.ZERO));
    }

    @Test
    void transferReportShouldIdentifySentAndReceivedAmounts() {

        Long sender = createTestAccount();
        Long receiver = createTestAccount();

        bankingService.deposit(
                transactionRequest(sender, "1000.00")
        );

        TransferRequest request = new TransferRequest();

        request.setSenderAccountId(sender);
        request.setReceiverAccountId(receiver);
        request.setAmount(new BigDecimal("250.00"));
        request.setTransactionUuid(
                UUID.randomUUID().toString()
        );

        bankingService.transfer(request);

        var senderReport =
                reportService.getAccountReport(sender);

        var receiverReport =
                reportService.getAccountReport(receiver);

        assertEquals(0, senderReport.totalTransfersSent()
                .compareTo(new BigDecimal("250.00")));

        assertEquals(0, receiverReport.totalTransfersReceived()
                .compareTo(new BigDecimal("250.00")));

        assertEquals(0, senderReport.currentBalance()
                .compareTo(new BigDecimal("750.00")));

        assertEquals(0, receiverReport.currentBalance()
                .compareTo(new BigDecimal("250.00")));
    }

    @Test
    void invalidDateRangeShouldBeRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> reportService.getDailyTransactions(
                        LocalDate.of(2026, 12, 31),
                        LocalDate.of(2026, 1, 1)
                )
        );
    }

    @Test
    void nonexistentAccountReportShouldFail() {

        assertThrows(
                AccountNotFoundException.class,
                () -> reportService.getAccountReport(
                        Long.MAX_VALUE
                )
        );
    }

    @Test
    void bankingOverviewShouldHaveConsistentAccountCounts() {

        var overview = reportService.getBankingOverview();

        assertEquals(
                overview.totalAccounts(),
                overview.activeAccounts()
                        + overview.inactiveAccounts()
        );

        assertTrue(overview.successfulOperations() >= 0);
        assertTrue(overview.failedAttempts() >= 0);
    }

    @Test
    void transferSummaryShouldNotDoubleCountOperations() {

        Long sender = createTestAccount();
        Long receiver = createTestAccount();

        bankingService.deposit(
                transactionRequest(sender, "1000.00")
        );

        long countBefore = reportService.getTransactionSummary()
                .stream()
                .filter(r -> "TRANSFER".equals(r.transactionType()))
                .mapToLong(r -> r.totalOperations())
                .sum();

        BigDecimal amountBefore =
                reportService.getTransactionSummary()
                        .stream()
                        .filter(r -> "TRANSFER".equals(r.transactionType()))
                        .map(r -> r.totalAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        TransferRequest request = new TransferRequest();

        request.setSenderAccountId(sender);
        request.setReceiverAccountId(receiver);
        request.setAmount(new BigDecimal("200.00"));
        request.setTransactionUuid(
                UUID.randomUUID().toString()
        );

        bankingService.transfer(request);

        var summary = reportService.getTransactionSummary()
                .stream()
                .filter(r -> "TRANSFER".equals(r.transactionType()))
                .findFirst()
                .orElseThrow();

        assertEquals(
                countBefore + 1,
                summary.totalOperations()
        );

        assertEquals(
                0,
                amountBefore.add(new BigDecimal("200.00"))
                        .compareTo(summary.totalAmount())
        );
    }
}
