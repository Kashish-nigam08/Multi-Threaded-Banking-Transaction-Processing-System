
package banking_system.dto;

public record FailedTransactionReportResponse(
        String transactionType,
        long totalFailures
) {
}
