
package banking_system.controller;

import banking_system.dto.AccountRequest;
import banking_system.service.BankingService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final BankingService bankingService;

    public AccountController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping
    public ResponseEntity<?> createAccount(
            @Valid @RequestBody AccountRequest request) {

        Long accountId = bankingService.createAccount(request);

        return ResponseEntity.ok(Map.of(
                "message", "Account created successfully",
                "accountId", accountId
        ));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable Long accountId) {

        BigDecimal balance = bankingService.getBalance(accountId);

        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "balance", balance
        ));
    }
}

