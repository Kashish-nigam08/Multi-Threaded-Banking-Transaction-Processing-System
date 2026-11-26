
package banking_system.security;

import banking_system.model.Role;
import banking_system.model.User;
import banking_system.repository.AccountRepository;
import banking_system.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AccountAuthorizationService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public AccountAuthorizationService(
            UserRepository userRepository,
            AccountRepository accountRepository
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    public User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "Authentication required"
            );
        }

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Access denied"
                        )
                );

        if (!user.enabled()) {
            throw new AccessDeniedException(
                    "Access denied"
            );
        }

        return user;
    }

    public void requireAccountAccess(Long accountId) {

        User user = getCurrentUser();

        if (user.role() == Role.ADMIN) {
            return;
        }

        if (user.customerId() == null
                || !accountRepository.isAccountOwnedByCustomer(
                        accountId,
                        user.customerId()
                )) {

            throw new AccessDeniedException(
                    "You do not have access to this account"
            );
        }
    }

    public Long requireCustomerId() {

        User user = getCurrentUser();

        if (user.role() != Role.CUSTOMER
                || user.customerId() == null) {

            throw new AccessDeniedException(
                    "Customer account required"
            );
        }

        return user.customerId();
    }

    public void requireAdmin() {

        User user = getCurrentUser();

        if (user.role() != Role.ADMIN) {

            throw new AccessDeniedException(
                    "Administrator access required"
            );
        }
    }
}
