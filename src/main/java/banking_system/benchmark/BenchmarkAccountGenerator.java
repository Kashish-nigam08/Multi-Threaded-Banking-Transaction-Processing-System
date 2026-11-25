
package banking_system.benchmark;

import banking_system.dto.AccountRequest;
import banking_system.dto.TransactionRequest;
import banking_system.service.BankingService;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Profile("benchmark")
public class BenchmarkAccountGenerator {

    private final BankingService bankingService;

    public BenchmarkAccountGenerator(
            BankingService bankingService) {

        this.bankingService = bankingService;
    }

    public List<Long> generateAccounts(int numberOfAccounts) {

        List<Long> accountIds = new ArrayList<>();

        for (int i = 0; i < numberOfAccounts; i++) {

            String uniqueId = UUID.randomUUID()
                    .toString()
                    .replace("-", "");

            AccountRequest request = new AccountRequest();

            request.setFullName("Benchmark Customer " + i);

            request.setEmail(
                    "benchmark-" + uniqueId + "@example.com"
            );

            request.setAccountNumber(
                    "B" + uniqueId.substring(0, 12)
            );

            Long accountId =
                    bankingService.createAccount(request);

            TransactionRequest deposit =
                    new TransactionRequest();

            deposit.setAccountId(accountId);

            deposit.setAmount(
                    new BigDecimal("100000.00")
            );

            deposit.setTransactionUuid(
                    UUID.randomUUID().toString()
            );

            bankingService.deposit(deposit);

            accountIds.add(accountId);
        }

        return accountIds;
    }
}
