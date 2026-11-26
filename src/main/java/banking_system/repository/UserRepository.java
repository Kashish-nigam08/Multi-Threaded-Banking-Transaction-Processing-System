package banking_system.repository;

import banking_system.model.Role;
import banking_system.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByUsername(String username) {

        String sql = """
                SELECT user_id, customer_id, username,
                       password_hash, role, enabled
                FROM users
                WHERE username = ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new User(
                        rs.getLong("user_id"),
                        rs.getObject("customer_id", Long.class),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        Role.valueOf(rs.getString("role")),
                        rs.getBoolean("enabled")
                ),
                username
        ).stream().findFirst();
    }

    public boolean existsByUsername(String username) {

        String sql = """
                SELECT COUNT(*)
                FROM users
                WHERE username = ?
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                username
        );

        return count != null && count > 0;
    }

    public void createCustomerUser(
            Long customerId,
            String username,
            String passwordHash
    ) {

        String sql = """
                INSERT INTO users
                    (customer_id, username, password_hash, role)
                VALUES (?, ?, ?, 'CUSTOMER')
                """;

        jdbcTemplate.update(
                sql,
                customerId,
                username,
                passwordHash
        );
    }
    public void createAdminUser(
        String username,
        String passwordHash
) {
    String sql = """
            INSERT INTO users (
                customer_id,
                username,
                password_hash,
                role,
                enabled
            )
            VALUES (NULL, ?, ?, 'ADMIN', TRUE)
            """;

    jdbcTemplate.update(
            sql,
            username,
            passwordHash
    );
}
}
