
package banking_system.benchmark;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Component
@Profile("benchmark")
public class BenchmarkExecutionLock {

    private static final String LOCK_NAME =
            "banking_system_benchmark_execution";

    private final DataSource dataSource;

    public BenchmarkExecutionLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @FunctionalInterface
    public interface BenchmarkTask {
        void execute() throws Exception;
    }

    public void executeWithLock(BenchmarkTask task)
            throws Exception {

        try (Connection connection =
                     dataSource.getConnection()) {

            boolean acquired = acquireLock(connection);

            if (!acquired) {
                throw new IllegalStateException(
                        "Another benchmark instance is already running."
                );
            }

            System.out.println(
                    "Exclusive benchmark lock acquired."
            );

            try {

                task.execute();

            } finally {

                releaseLock(connection);
            }
        }
    }

    private boolean acquireLock(Connection connection)
            throws Exception {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "SELECT GET_LOCK(?, 0)"
                     )) {

            statement.setString(1, LOCK_NAME);

            try (ResultSet result = statement.executeQuery()) {

                return result.next()
                        && result.getInt(1) == 1;
            }
        }
    }

    private void releaseLock(Connection connection)
            throws Exception {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "SELECT RELEASE_LOCK(?)"
                     )) {

            statement.setString(1, LOCK_NAME);

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next() || result.getInt(1) != 1) {
                    throw new IllegalStateException(
                            "Benchmark lock release could not be confirmed."
                    );
                }
            }

            System.out.println(
                    "Exclusive benchmark lock released."
            );
        }
    }
}
