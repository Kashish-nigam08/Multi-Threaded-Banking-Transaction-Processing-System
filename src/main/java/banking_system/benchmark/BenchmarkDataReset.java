
package banking_system.benchmark;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("benchmark")
public class BenchmarkDataReset {

    private final JdbcTemplate jdbcTemplate;

    public BenchmarkDataReset(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void reset() {

        String database = jdbcTemplate.queryForObject(
                "SELECT DATABASE()", String.class
        );

        if (!"banking_system_benchmark".equals(database)) {
            throw new IllegalStateException(
                    "Reset allowed only on benchmark database"
            );
        }

        jdbcTemplate.update("DELETE FROM transaction_audit");
        jdbcTemplate.update("DELETE FROM failed_transactions");
        jdbcTemplate.update("DELETE FROM banking_transactions");
        jdbcTemplate.update("DELETE FROM transaction_requests");
        jdbcTemplate.update("DELETE FROM accounts");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM customers");

        System.out.println("Benchmark database reset completed.");
    }
}
