
package banking_system.exception;

public class TransactionReservationConflictException
        extends RuntimeException {

    public TransactionReservationConflictException(
            String transactionUuid,
            Throwable cause) {

        super("Transaction UUID already reserved: "
                + transactionUuid, cause);
    }
}
