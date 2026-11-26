
package banking_system;

import banking_system.dto.AuthResponse;
import banking_system.dto.LoginRequest;
import banking_system.dto.RegisterRequest;
import banking_system.dto.RegisterResponse;
import banking_system.exception.DuplicateUserException;
import banking_system.model.User;
import banking_system.repository.UserRepository;
import banking_system.security.JwtService;
import banking_system.service.AuthService;

import io.jsonwebtoken.JwtException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthenticationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String PASSWORD = "BankingTest@123";

    @BeforeEach
    void verifyTestDatabase() {
        assertEquals(
                "banking_system_test",
                jdbcTemplate.queryForObject(
                        "SELECT DATABASE()", String.class
                )
        );
    }

    @Test
    void customerRegistrationIsSuccessful() {

        RegisterRequest request = createRequest();

        RegisterResponse response =
                authService.register(request);

        assertEquals(request.username(), response.username());
        assertEquals("CUSTOMER", response.role());

        User user = userRepository
                .findByUsername(request.username())
                .orElseThrow();

        assertNotNull(user.customerId());
        assertTrue(user.enabled());
        assertEquals("CUSTOMER", user.role().name());
    }

    @Test
    void passwordIsStoredAsBCryptHash() {

        RegisterRequest request = createRequest();

        authService.register(request);

        User user = userRepository
                .findByUsername(request.username())
                .orElseThrow();

        assertNotEquals(PASSWORD, user.passwordHash());

        assertTrue(
                passwordEncoder.matches(
                        PASSWORD,
                        user.passwordHash()
                )
        );

        assertEquals(60, user.passwordHash().length());
    }

    @Test
    void duplicateRegistrationIsRejected() {

        RegisterRequest request = createRequest();

        authService.register(request);

        assertThrows(
                DuplicateUserException.class,
                () -> authService.register(request)
        );
    }

    @Test
    void validCredentialsGenerateJwtToken() {

        RegisterRequest request = createRequest();

        authService.register(request);

        AuthResponse response = authService.login(
                new LoginRequest(
                        request.username(),
                        PASSWORD
                )
        );

        assertNotNull(response.token());
        assertFalse(response.token().isBlank());
        assertEquals("Bearer", response.tokenType());
        assertEquals("CUSTOMER", response.role());
        assertEquals(1800L, response.expiresIn());

        assertEquals(
                request.username(),
                jwtService.extractUsername(response.token())
        );
    }

    @Test
    void incorrectPasswordIsRejected() {

        RegisterRequest request = createRequest();

        authService.register(request);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(
                        new LoginRequest(
                                request.username(),
                                "IncorrectPassword123"
                        )
                )
        );
    }

    @Test
    void nonexistentUserIsRejected() {

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(
                        new LoginRequest(
                                "missing-" + UUID.randomUUID(),
                                PASSWORD
                        )
                )
        );
    }

    @Test
    void invalidJwtTokenIsRejected() {

        assertThrows(
                JwtException.class,
                () -> jwtService.extractUsername(
                        "invalid.jwt.token"
                )
        );
    }

    @Test
    void disabledUserCannotLogin() {

        RegisterRequest request = createRequest();

        authService.register(request);

        jdbcTemplate.update(
                "UPDATE users SET enabled = FALSE WHERE username = ?",
                request.username()
        );

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(
                        new LoginRequest(
                                request.username(),
                                PASSWORD
                        )
                )
        );
    }

    private RegisterRequest createRequest() {

        String uniqueId = UUID.randomUUID()
                .toString()
                .replace("-", "");

        return new RegisterRequest(
                "Authentication Test Customer",
                "auth-" + uniqueId + "@example.com",
                "auth_" + uniqueId,
                PASSWORD
        );
    }
}
