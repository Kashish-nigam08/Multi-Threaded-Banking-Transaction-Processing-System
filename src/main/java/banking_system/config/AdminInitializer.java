
package banking_system.config;

import banking_system.model.Role;
import banking_system.model.User;
import banking_system.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Component
@Profile("!test & !benchmark")
@ConditionalOnProperty(
        name = "banking.admin.enabled",
        havingValue = "true"
)
public class AdminInitializer implements CommandLineRunner {

    private static final Logger logger =
            LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${banking.admin.username}")
    private String username;

    @Value("${banking.admin.password}")
    private String password;

    public AdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        String normalizedUsername =
                username.trim().toLowerCase(Locale.ROOT);

        if (!normalizedUsername.matches("[a-z0-9._-]{4,50}")) {
            throw new IllegalStateException(
                    "Invalid administrator username"
            );
        }

        Optional<User> existingUser =
                userRepository.findByUsername(normalizedUsername);

        if (existingUser.isPresent()) {

            if (existingUser.get().role() != Role.ADMIN) {
                throw new IllegalStateException(
                        "Administrator username already belongs to a customer"
                );
            }

            logger.info("Administrator already exists");
            return;
        }

        if (password.isBlank()
                || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new IllegalStateException(
                    "Administrator password must contain at least 12 characters and at most 72 bytes"
            );
        }

        String passwordHash =
                passwordEncoder.encode(password);

        userRepository.createAdminUser(
                normalizedUsername,
                passwordHash
        );

        logger.info("Administrator created successfully");
    }
}
