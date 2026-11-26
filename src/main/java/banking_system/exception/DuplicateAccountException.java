package banking_system.exception;

public class DuplicateAccountException extends RuntimeException {

    public DuplicateAccountException() {
        super("Account number already exists");
    }
}
