
package banking_system.service;
import banking_system.dto.AccountStatusRequest;
import banking_system.dto.AccountRequest;
import banking_system.dto.TransactionRequest;
import banking_system.dto.TransferRequest;
import banking_system.exception.AccountNotFoundException;
import banking_system.exception.DuplicateTransactionException;
import banking_system.exception.InsufficientBalanceException;
import banking_system.exception.TransactionReservationConflictException;
import banking_system.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import banking_system.exception.TransactionReservationConflictException;
import java.math.BigDecimal;
import banking_system.dto.TransactionHistoryResponse;
import banking_system.dto.PaginatedTransactionResponse;
import java.util.List;
import java.util.Set;
@Service
public class BankingService {

    private final AccountRepository repository;
    private final TransactionExecutionService executionService;
    private final FailedTransactionService failedTransactionService;

    public BankingService(
            AccountRepository repository,
            TransactionExecutionService executionService,
            FailedTransactionService failedTransactionService) {

        this.repository = repository;
        this.executionService = executionService;
        this.failedTransactionService = failedTransactionService;
    }

    @Transactional
    public Long createAccount(AccountRequest request) {

        Long customerId = repository.createCustomer(
                request.getFullName(),
                request.getEmail()
        );

        return repository.createAccount(
                request.getAccountNumber(),
                customerId
        );
    }

    public BigDecimal getBalance(Long accountId) {

        if (!repository.accountExists(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        return repository.getBalance(accountId);
    }

    public BigDecimal deposit(TransactionRequest request) {

        validateAmount(request.getAmount());
        validateUuid(request.getTransactionUuid());

        try {
            return executionService.deposit(request);
        } catch (TransactionReservationConflictException ex) {
    throw new DuplicateTransactionException(
            request.getTransactionUuid());
}
    }

    public BigDecimal withdraw(TransactionRequest request) {

        validateAmount(request.getAmount());
        validateUuid(request.getTransactionUuid());

        try {
            return executionService.withdraw(request);

        } catch (TransactionReservationConflictException ex) {
    throw new DuplicateTransactionException(
            request.getTransactionUuid());
} catch (InsufficientBalanceException ex) {

            failedTransactionService.recordFailure(
                    request.getTransactionUuid(),
                    request.getAccountId(),
                    "WITHDRAWAL",
                    request.getAmount(),
                    ex.getMessage()
            );

            throw ex;
        }
    }

    public void transfer(TransferRequest request) {

        validateAmount(request.getAmount());
        validateUuid(request.getTransactionUuid());

        if (request.getSenderAccountId() == null ||
                request.getReceiverAccountId() == null) {
            throw new IllegalArgumentException(
                    "Sender and receiver account IDs are required");
        }

        if (request.getSenderAccountId().equals(
                request.getReceiverAccountId())) {
            throw new IllegalArgumentException(
                    "Sender and receiver accounts must be different");
        }

        try {
            executionService.transfer(request);

        } catch (TransactionReservationConflictException ex) {
    throw new DuplicateTransactionException(
            request.getTransactionUuid());
} catch (InsufficientBalanceException ex) {

            failedTransactionService.recordFailure(
                    request.getTransactionUuid(),
                    request.getSenderAccountId(),
                    "TRANSFER",
                    request.getAmount(),
                    ex.getMessage()
            );

            throw ex;
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero");
        }
    }

    private void validateUuid(String transactionUuid) {
        if (transactionUuid == null ||
                transactionUuid.isBlank() ||
                transactionUuid.length() > 100) {
            throw new IllegalArgumentException(
                    "Transaction UUID must contain 1 to 100 characters");
        }
    }

public PaginatedTransactionResponse getTransactionHistory(
        Long accountId,
        String transactionType,
        int page,
        int size) {

    if (accountId == null || accountId <= 0) {
        throw new IllegalArgumentException(
                "Account ID must be positive"
        );
    }

    if (page < 0) {
        throw new IllegalArgumentException(
                "Page number cannot be negative"
        );
    }

    if (size < 1 || size > 100) {
        throw new IllegalArgumentException(
                "Page size must be between 1 and 100"
        );
    }

    String normalizedType = null;

    if (transactionType != null && !transactionType.isBlank()) {
        normalizedType = transactionType.trim().toUpperCase(
                java.util.Locale.ROOT
        );

        Set<String> allowedTypes =
                Set.of("DEPOSIT", "WITHDRAWAL", "TRANSFER");

        if (!allowedTypes.contains(normalizedType)) {
            throw new IllegalArgumentException(
                    "Invalid transaction type: " + transactionType
            );
        }
    }

    if (!repository.accountExists(accountId)) {
        throw new banking_system.exception.AccountNotFoundException(
                accountId
        );
    }

    long totalElements = repository.countTransactions(
            accountId,
            normalizedType
    );

    List<TransactionHistoryResponse> transactions =
            repository.getTransactionHistory(
                    accountId,
                    normalizedType,
                    page,
                    size
            );

    long totalPages = totalElements / size
            + (totalElements % size == 0 ? 0 : 1);

    return new PaginatedTransactionResponse(
            accountId,
            page,
            size,
            totalElements,
            totalPages,
            transactions
    );
}

@Transactional
public void updateAccountStatus(
        Long accountId,
        AccountStatusRequest request) {

    if (accountId == null || accountId <= 0) {
        throw new IllegalArgumentException(
                "Account ID must be positive"
        );
    }

    if (request == null ||
            request.getStatus() == null ||
            !java.util.Set.of("ACTIVE", "INACTIVE")
                    .contains(request.getStatus())) {
        throw new IllegalArgumentException(
                "Status must be ACTIVE or INACTIVE"
        );
    }

    // Lock the account before updating its status.
    repository.lockAccount(accountId);

    repository.updateAccountStatus(
            accountId,
            request.getStatus()
    );

    // Record the account status change in the audit trail.
    repository.saveAudit(
            java.util.UUID.randomUUID().toString(),
            accountId,
            "ACCOUNT_STATUS_CHANGE",
            "Account status changed to " + request.getStatus()
    );
}




}
