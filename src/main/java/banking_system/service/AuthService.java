package banking_system.service;

import banking_system.dto.RegisterRequest;
import banking_system.dto.RegisterResponse;
import banking_system.exception.DuplicateUserException;
import banking_system.repository.AccountRepository;
import banking_system.repository.UserRepository;
import banking_system.dto.LoginRequest;
import banking_system.dto.AuthResponse;
import banking_system.model.User;
import banking_system.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
@Profile("!benchmark")
@Service
public class AuthService {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
        UserRepository userRepository,
        AccountRepository accountRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
) {
    this.userRepository = userRepository;
    this.accountRepository = accountRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
}

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String username = request.username()
                .trim()
                .toLowerCase(Locale.ROOT);

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!username.matches("[a-z0-9._-]{4,50}")) {
            throw new IllegalArgumentException(
                    "Invalid username format"
            );
        }

        if (request.password().getBytes(
                StandardCharsets.UTF_8).length > 72) {

            throw new IllegalArgumentException(
                    "Password must not exceed 72 bytes"
            );
        }

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUserException();
        }

        String passwordHash =
                passwordEncoder.encode(request.password());

        try {

            Long customerId = accountRepository.createCustomer(
                    request.fullName().trim(),
                    email
            );

            userRepository.createCustomerUser(
                    customerId,
                    username,
                    passwordHash
            );

        } catch (DuplicateKeyException exception) {

            throw new DuplicateUserException();
        }

        return new RegisterResponse(
                "Registration successful",
                username,
                "CUSTOMER"
        );
    }
    @Transactional(readOnly = true)
public AuthResponse login(LoginRequest request) {

    String username = request.username()
            .trim()
            .toLowerCase(Locale.ROOT);

    if (request.password().getBytes(
            StandardCharsets.UTF_8).length > 72) {
        throw new BadCredentialsException(
                "Invalid username or password"
        );
    }

    User user = userRepository.findByUsername(username)
            .orElseThrow(() ->
                    new BadCredentialsException(
                            "Invalid username or password"
                    )
            );

    if (!passwordEncoder.matches(
            request.password(),
            user.passwordHash()
    ) || !user.enabled()) {

        throw new BadCredentialsException(
                "Invalid username or password"
        );
    }

    String token = jwtService.generateToken(user.username());

    return new AuthResponse(
            token,
            "Bearer",
            user.username(),
            user.role().name(),
            jwtService.getExpirationSeconds()
    );
}
}
