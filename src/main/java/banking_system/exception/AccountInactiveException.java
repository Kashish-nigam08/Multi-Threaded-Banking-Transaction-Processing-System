
package banking_system.exception;

public class AccountInactiveException extends RuntimeException {

    public AccountInactiveException(Long accountId) {
        super("Account is inactive: " + accountId);
    }
}
