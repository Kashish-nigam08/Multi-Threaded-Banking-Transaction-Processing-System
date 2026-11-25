
package banking_system.dto;

import java.math.BigDecimal;

public record TransactionSummaryResponse(
        String transactionType,
        long totalOperations,
        BigDecimal totalAmount,
        BigDecimal averageAmount,
        BigDecimal minimumAmount,
        BigDecimal maximumAmount
) {
}
