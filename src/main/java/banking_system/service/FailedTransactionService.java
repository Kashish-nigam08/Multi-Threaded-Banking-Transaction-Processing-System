
package banking_system.service;

import banking_system.repository.AccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class FailedTransactionService {

    private final AccountRepository repository;

    public FailedTransactionService(AccountRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            String transactionUuid,
            Long accountId,
            String transactionType,
            BigDecimal amount,
            String reason) {

        repository.saveFailedTransaction(
                transactionUuid,
                accountId,
                transactionType,
                amount,
                reason
        );
    }
}
