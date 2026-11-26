package banking_system.dto;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        String role,
        long expiresIn
) {
}
