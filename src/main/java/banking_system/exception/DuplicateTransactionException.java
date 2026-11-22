package banking_system.exception;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String transactionUuid) {
        super("Duplicate transaction ID: " + transactionUuid);
    }
}