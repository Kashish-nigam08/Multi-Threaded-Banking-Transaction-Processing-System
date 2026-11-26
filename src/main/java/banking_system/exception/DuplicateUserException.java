package banking_system.exception;

public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException() {
        super("Username or email already registered");
    }
}
