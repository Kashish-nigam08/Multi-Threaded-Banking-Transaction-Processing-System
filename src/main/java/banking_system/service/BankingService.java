
package banking_system.service;

import banking_system.repository.AccountRepository;
import banking_system.dto.TransactionRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BankingService {

    private final AccountRepository accountRepository;

    public BankingService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }


    // Create customer and account together
    @Transactional
    public Long createAccount(
            banking_system.dto.AccountRequest request) {

        Long customerId = accountRepository.createCustomer(
                request.getFullName(),
                request.getEmail()
        );

        return accountRepository.createAccount(
                request.getAccountNumber(),
                customerId
        );
    }


    @Transactional
    public void transfer(
            banking_system.dto.TransferRequest request) {

        validateAmount(request.getAmount());

        if (request.getSenderAccountId().equals(
                request.getReceiverAccountId())) {
            throw new IllegalArgumentException(
                    "Sender and receiver accounts must be different");
        }

        if (accountRepository.transactionExists(request.getTransactionUuid())) {
            throw new IllegalArgumentException(
                    "Duplicate transaction ID");
        }

        if (!accountRepository.accountExists(request.getSenderAccountId())
                || !accountRepository.accountExists(request.getReceiverAccountId())) {
            throw new IllegalArgumentException("Account not found");
        }

        int debited = accountRepository.withdraw(
                request.getSenderAccountId(),
                request.getAmount()
        );

        if (debited == 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        accountRepository.deposit(
                request.getReceiverAccountId(),
                request.getAmount()
        );

        accountRepository.saveTransaction(
                request.getTransactionUuid(),
                request.getSenderAccountId(),
                "TRANSFER",
                request.getAmount(),
                "SUCCESS",
                "Transfer to account " + request.getReceiverAccountId()
        );

        accountRepository.saveTransaction(
                request.getTransactionUuid() + "-CREDIT",
                request.getReceiverAccountId(),
                "TRANSFER",
                request.getAmount(),
                "SUCCESS",
                "Transfer from account " + request.getSenderAccountId()
        );
    }


    // Check account balance
    public BigDecimal getBalance(Long accountId) {

        if (!accountRepository.accountExists(accountId)) {
            throw new IllegalArgumentException("Account not found");
        }

        return accountRepository.getBalance(accountId);
    }


    @Transactional
    public BigDecimal deposit(TransactionRequest request) {

        validateAmount(request.getAmount());

        if (accountRepository.transactionExists(request.getTransactionUuid())) {
            throw new IllegalArgumentException("Duplicate transaction ID");
        }

        if (!accountRepository.accountExists(request.getAccountId())) {
            throw new IllegalArgumentException("Account not found");
        }

        int rowsUpdated = accountRepository.deposit(
                request.getAccountId(),
                request.getAmount()
        );

        if (rowsUpdated == 0) {
            throw new IllegalStateException("Deposit failed");
        }

        accountRepository.saveTransaction(
                request.getTransactionUuid(),
                request.getAccountId(),
                "DEPOSIT",
                request.getAmount(),
                "SUCCESS",
                "Money deposited"
        );

        return accountRepository.getBalance(request.getAccountId());
    }

    @Transactional
    public BigDecimal withdraw(TransactionRequest request) {

        validateAmount(request.getAmount());

        if (accountRepository.transactionExists(request.getTransactionUuid())) {
            throw new IllegalArgumentException("Duplicate transaction ID");
        }

        if (!accountRepository.accountExists(request.getAccountId())) {
            throw new IllegalArgumentException("Account not found");
        }

        int rowsUpdated = accountRepository.withdraw(
                request.getAccountId(),
                request.getAmount()
        );

        if (rowsUpdated == 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        accountRepository.saveTransaction(
                request.getTransactionUuid(),
                request.getAccountId(),
                "WITHDRAWAL",
                request.getAmount(),
                "SUCCESS",
                "Money withdrawn"
        );

        return accountRepository.getBalance(request.getAccountId());
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }
    }
}
