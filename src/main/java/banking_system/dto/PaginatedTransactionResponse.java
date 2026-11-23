
package banking_system.dto;

import java.util.List;

public record PaginatedTransactionResponse(
        Long accountId,
        int page,
        int size,
        long totalElements,
        long totalPages,
        List<TransactionHistoryResponse> transactions
) {
}
