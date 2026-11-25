
package banking_system.dto;

import java.math.BigDecimal;

public record AccountReportResponse(
        Long accountId,
        String accountStatus,
        BigDecimal currentBalance,
        long totalTransactions,
        BigDecimal totalDeposits,
        BigDecimal totalWithdrawals,
        BigDecimal totalTransfersSent,
        BigDecimal totalTransfersReceived
) {
}
