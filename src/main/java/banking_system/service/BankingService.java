
package banking_system.service;

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
}
