
package banking_system.controller;

import banking_system.dto.TransactionRequest;
import banking_system.dto.TransferRequest;
import banking_system.service.BankingService;
import banking_system.security.AccountAuthorizationService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final BankingService bankingService;

    private final AccountAuthorizationService
            accountAuthorizationService;

    public TransactionController(
            BankingService bankingService,
            AccountAuthorizationService accountAuthorizationService
    ) {
        this.bankingService = bankingService;
        this.accountAuthorizationService =
                accountAuthorizationService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(
            @Valid @RequestBody TransactionRequest request
    ) {

        accountAuthorizationService.requireAccountAccess(
                request.getAccountId()
        );

        BigDecimal balance = bankingService.deposit(request);

        return ResponseEntity.ok(Map.of(
                "message", "Deposit successful",
                "newBalance", balance
        ));
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(
            @Valid @RequestBody TransferRequest request
    ) {

        accountAuthorizationService.requireAccountAccess(
                request.getSenderAccountId()
        );

        bankingService.transfer(request);

        return ResponseEntity.ok(Map.of(
                "message", "Transfer successful",
                "transactionUuid", request.getTransactionUuid()
        ));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(
            @Valid @RequestBody TransactionRequest request
    ) {

        accountAuthorizationService.requireAccountAccess(
                request.getAccountId()
        );

        BigDecimal balance = bankingService.withdraw(request);

        return ResponseEntity.ok(Map.of(
                "message", "Withdrawal successful",
                "newBalance", balance
        ));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getTransactionHistory(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type
    ) {

        accountAuthorizationService.requireAccountAccess(
                accountId
        );

        return ResponseEntity.ok(
                bankingService.getTransactionHistory(
                        accountId,
                        type,
                        page,
                        size
                )
        );
    }
}
