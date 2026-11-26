package banking_system.model;

public record User(
        Long userId,
        Long customerId,
        String username,
        String passwordHash,
        Role role,
        boolean enabled
) {
}
