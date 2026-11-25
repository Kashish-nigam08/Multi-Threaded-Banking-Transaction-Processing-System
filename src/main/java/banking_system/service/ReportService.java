
package banking_system.service;

import banking_system.dto.TransactionSummaryResponse;
import banking_system.repository.ReportRepository;
import banking_system.dto.DailyTransactionResponse;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import banking_system.dto.AccountReportResponse;
import banking_system.dto.FailedTransactionReportResponse;
import banking_system.dto.BankingOverviewResponse;
import banking_system.exception.AccountNotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;
import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionSummaryResponse> getTransactionSummary() {
        return reportRepository.getTransactionSummary();
    }

@Transactional(readOnly = true)
public List<DailyTransactionResponse> getDailyTransactions(
        LocalDate startDate,
        LocalDate endDate) {

    if (startDate == null || endDate == null) {
        throw new IllegalArgumentException(
                "Start date and end date are required"
        );
    }

    if (startDate.isAfter(endDate)) {
        throw new IllegalArgumentException(
                "Start date cannot be after end date"
        );
    }

    return reportRepository.getDailyTransactions(
            startDate, endDate
    );
}

@Transactional(readOnly = true)
public AccountReportResponse getAccountReport(Long accountId) {

    if (accountId == null || accountId <= 0) {
        throw new IllegalArgumentException(
                "Account ID must be positive"
        );
    }

    try {
        return reportRepository.getAccountReport(accountId);
    } catch (EmptyResultDataAccessException ex) {
        throw new AccountNotFoundException(accountId);
    }
}

@Transactional(readOnly = true)
public List<FailedTransactionReportResponse>
        getFailedTransactionReport() {

    return reportRepository.getFailedTransactionReport();
}

@Transactional(readOnly = true)
public BankingOverviewResponse getBankingOverview() {

    return reportRepository.getBankingOverview();
}

}
