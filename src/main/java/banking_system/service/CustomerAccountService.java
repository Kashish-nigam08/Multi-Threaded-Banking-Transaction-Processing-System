
package banking_system.service;

import banking_system.dto.CustomerAccountRequest;
import banking_system.repository.AccountRepository;
import banking_system.security.AccountAuthorizationService;
import banking_system.exception.DuplicateAccountException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerAccountService {

    private final AccountRepository accountRepository;

    private final AccountAuthorizationService
            accountAuthorizationService;

    public CustomerAccountService(
            AccountRepository accountRepository,
            AccountAuthorizationService accountAuthorizationService
    ) {
        this.accountRepository = accountRepository;
        this.accountAuthorizationService =
                accountAuthorizationService;
    }

    @Transactional
public Long createAccount(CustomerAccountRequest request) {

    Long customerId =
            accountAuthorizationService.requireCustomerId();

    try {

        return accountRepository.createAccount(
                request.accountNumber(),
                customerId
        );

    } catch (DuplicateKeyException exception) {

        throw new DuplicateAccountException();
    }
}
}
