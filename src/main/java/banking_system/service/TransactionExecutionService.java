
package banking_system.service;

import banking_system.dto.TransactionRequest;
import banking_system.dto.TransferRequest;
import banking_system.exception.InsufficientBalanceException;
import banking_system.repository.AccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class TransactionExecutionService {
    private static final Logger log =
        LoggerFactory.getLogger(TransactionExecutionService.class);
    private final AccountRepository repository;

    public TransactionExecutionService(AccountRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public BigDecimal deposit(TransactionRequest request) {


repository.reserveTransactionUuidSafely(
        request.getTransactionUuid(), "DEPOSIT");
log.info(
        "Deposit started | transactionUuid={} | accountId={}",
        request.getTransactionUuid(),
        request.getAccountId()
);
repository.validateActiveAccount(request.getAccountId());


        int updated = repository.deposit(
                request.getAccountId(), request.getAmount());

        if (updated != 1) {
            throw new IllegalStateException("Deposit failed");
        }


repository.saveTransaction(
        UUID.randomUUID().toString(),
        request.getTransactionUuid(),
        request.getAccountId(),
        "DEPOSIT",
        request.getAmount(),
        "SUCCESS",
        "Money deposited"
);


        repository.saveAudit(
                request.getTransactionUuid(),
                request.getAccountId(),
                "DEPOSIT",
                "Deposit completed successfully: " + request.getAmount()
        );

        repository.markTransactionCompleted(request.getTransactionUuid());
        log.info(
        "Deposit completed | transactionUuid={} | accountId={}",
        request.getTransactionUuid(),
        request.getAccountId()
);
        return repository.getBalance(request.getAccountId());
    }

    @Transactional
    public BigDecimal withdraw(TransactionRequest request) {


repository.reserveTransactionUuidSafely(
        request.getTransactionUuid(), "WITHDRAWAL");
log.info(
        "Withdrawal started | transactionUuid={} | accountId={}",
        request.getTransactionUuid(),
        request.getAccountId()
);
repository.validateActiveAccount(request.getAccountId());


        int updated = repository.withdraw(
                request.getAccountId(), request.getAmount());

        if (updated != 1) {
            throw new InsufficientBalanceException(request.getAccountId());
        }


repository.saveTransaction(
        UUID.randomUUID().toString(),
        request.getTransactionUuid(),
        request.getAccountId(),
        "WITHDRAWAL",
        request.getAmount(),
        "SUCCESS",
        "Money withdrawn"
);


        repository.saveAudit(
                request.getTransactionUuid(),
                request.getAccountId(),
                "WITHDRAWAL",
                "Withdrawal completed successfully: " + request.getAmount()
        );

        repository.markTransactionCompleted(request.getTransactionUuid());
        log.info(
        "Withdrawal completed | transactionUuid={} | accountId={}",
        request.getTransactionUuid(),
        request.getAccountId()
);
        return repository.getBalance(request.getAccountId());
    }

    @Transactional
    public void transfer(TransferRequest request) {

        repository.reserveTransactionUuidSafely(
                request.getTransactionUuid(), "TRANSFER");
log.info(
        "Transfer started | transactionUuid={} | sender={} | receiver={}",
        request.getTransactionUuid(),
        request.getSenderAccountId(),
        request.getReceiverAccountId()
);

Long sender = request.getSenderAccountId();
Long receiver = request.getReceiverAccountId();

// Always acquire locks in ascending account-ID order.
Long firstAccount = Math.min(sender, receiver);
Long secondAccount = Math.max(sender, receiver);

repository.lockAccount(firstAccount);
repository.lockAccount(secondAccount);

repository.validateActiveAccount(sender);
repository.validateActiveAccount(receiver);


        int debited = repository.withdraw(sender, request.getAmount());

        if (debited != 1) {
            throw new InsufficientBalanceException(sender);
        }

        int credited = repository.deposit(receiver, request.getAmount());

        if (credited != 1) {
            throw new IllegalStateException("Transfer credit failed");
        }


repository.saveTransaction(
        UUID.randomUUID().toString(),
        request.getTransactionUuid(),
        sender,
        "TRANSFER",
        request.getAmount(),
        "SUCCESS",
        "Transfer to account " + receiver
);



repository.saveTransaction(
        UUID.randomUUID().toString(),
        request.getTransactionUuid(),
        receiver,
        "TRANSFER",
        request.getAmount(),
        "SUCCESS",
        "Transfer from account " + sender
);


        repository.saveAudit(
                request.getTransactionUuid(),
                sender,
                "TRANSFER_DEBIT",
                "Transferred " + request.getAmount() + " to account " + receiver
        );

        repository.saveAudit(
                request.getTransactionUuid(),
                receiver,
                "TRANSFER_CREDIT",
                "Received " + request.getAmount() + " from account " + sender
        );

        repository.markTransactionCompleted(request.getTransactionUuid());
    log.info(
        "Transfer completed | transactionUuid={} | sender={} | receiver={}",
        request.getTransactionUuid(),
        request.getSenderAccountId(),
        request.getReceiverAccountId()
);
    }
}
