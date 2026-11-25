
package banking_system.controller;

import banking_system.dto.TransactionSummaryResponse;
import banking_system.service.ReportService;
import banking_system.dto.AccountReportResponse;
import banking_system.dto.FailedTransactionReportResponse;
import banking_system.dto.BankingOverviewResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import banking_system.dto.DailyTransactionResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<List<TransactionSummaryResponse>>
            getTransactionSummary() {

        return ResponseEntity.ok(
                reportService.getTransactionSummary()
        );
    }

@GetMapping("/daily")
public ResponseEntity<List<DailyTransactionResponse>>
        getDailyTransactions(

        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate) {

    return ResponseEntity.ok(
            reportService.getDailyTransactions(
                    startDate, endDate
            )
    );
}

@GetMapping("/account/{accountId}")
public ResponseEntity<AccountReportResponse> getAccountReport(
        @PathVariable Long accountId) {

    return ResponseEntity.ok(
            reportService.getAccountReport(accountId)
    );
}

@GetMapping("/failed")
public ResponseEntity<List<FailedTransactionReportResponse>>
        getFailedTransactionReport() {

    return ResponseEntity.ok(
            reportService.getFailedTransactionReport()
    );
}

@GetMapping("/overview")
public ResponseEntity<BankingOverviewResponse>
        getBankingOverview() {

    return ResponseEntity.ok(
            reportService.getBankingOverview()
    );
}


}
