
package banking_system.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyTransactionResponse(
        LocalDate transactionDate,
        String transactionType,
        long totalOperations,
        BigDecimal totalAmount
) {
}
