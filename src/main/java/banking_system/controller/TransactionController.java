
package banking_system.controller;

import banking_system.dto.TransactionRequest;
import banking_system.service.BankingService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final BankingService bankingService;

    public TransactionController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@Valid @RequestBody TransactionRequest request) {

        BigDecimal balance = bankingService.deposit(request);

        return ResponseEntity.ok(Map.of(
                "message", "Deposit successful",
                "newBalance", balance
        ));
    }


    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(
            @Valid @RequestBody
            banking_system.dto.TransferRequest request) {

        bankingService.transfer(request);

        return ResponseEntity.ok(Map.of(
                "message", "Transfer successful",
                "transactionUuid", request.getTransactionUuid()
        ));
    }


    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@Valid @RequestBody TransactionRequest request) {

        BigDecimal balance = bankingService.withdraw(request);

        return ResponseEntity.ok(Map.of(
                "message", "Withdrawal successful",
                "newBalance", balance
        ));
    }
}

