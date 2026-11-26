
package banking_system.controller;

import banking_system.dto.AccountRequest;
import banking_system.dto.AccountStatusRequest;
import banking_system.dto.CustomerAccountRequest;

import banking_system.service.BankingService;
import banking_system.service.CustomerAccountService;
import banking_system.security.AccountAuthorizationService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final BankingService bankingService;

    private final CustomerAccountService customerAccountService;

    private final AccountAuthorizationService
            accountAuthorizationService;

    public AccountController(
            BankingService bankingService,
            CustomerAccountService customerAccountService,
            AccountAuthorizationService accountAuthorizationService
    ) {
        this.bankingService = bankingService;
        this.customerAccountService = customerAccountService;
        this.accountAuthorizationService =
                accountAuthorizationService;
    }

    // Administrator account creation
    @PostMapping
    public ResponseEntity<?> createAccount(
            @Valid @RequestBody AccountRequest request
    ) {

        accountAuthorizationService.requireAdmin();

        Long accountId =
                bankingService.createAccount(request);

        return ResponseEntity.ok(Map.of(
                "message", "Account created successfully",
                "accountId", accountId
        ));
    }

    // Customer account creation
    @PostMapping("/me")
    public ResponseEntity<?> createCustomerAccount(
            @Valid @RequestBody CustomerAccountRequest request
    ) {

        Long accountId =
                customerAccountService.createAccount(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Account created successfully",
                        "accountId", accountId
                ));
    }

    // Account balance with ownership validation
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<?> getBalance(
            @PathVariable Long accountId
    ) {

        accountAuthorizationService
                .requireAccountAccess(accountId);

        BigDecimal balance =
                bankingService.getBalance(accountId);

        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "balance", balance
        ));
    }

    // Administrator-only account status update
    @PatchMapping("/{accountId}/status")
    public ResponseEntity<?> updateAccountStatus(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountStatusRequest request
    ) {

        accountAuthorizationService.requireAdmin();

        bankingService.updateAccountStatus(
                accountId,
                request
        );

        return ResponseEntity.ok(Map.of(
                "message", "Account status updated successfully",
                "accountId", accountId,
                "status", request.getStatus()
        ));
    }
}
