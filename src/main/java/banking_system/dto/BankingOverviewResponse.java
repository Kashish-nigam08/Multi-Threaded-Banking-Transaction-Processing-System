
package banking_system.dto;

import java.math.BigDecimal;

public record BankingOverviewResponse(
        long totalCustomers,
        long totalAccounts,
        long activeAccounts,
        long inactiveAccounts,
        BigDecimal totalBalance,
        long successfulOperations,
        long failedAttempts
) {
}
