package banking_system.dto;

public record RegisterResponse(
        String message,
        String username,
        String role
) {
}
