
package banking_system;

import banking_system.dto.RegisterRequest;
import banking_system.repository.AccountRepository;
import banking_system.repository.UserRepository;
import banking_system.security.JwtService;
import banking_system.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long accountA;
    private Long accountB;

    private String tokenA;
    private String tokenB;
    private String adminToken;

    @BeforeEach
    void setup() {

        assertEquals(
                "banking_system_test",
                jdbcTemplate.queryForObject(
                        "SELECT DATABASE()", String.class
                )
        );

        String id = UUID.randomUUID()
                .toString().replace("-", "");

        String usernameA = "a_" + id;
        String usernameB = "b_" + id;
        String adminUsername = "admin_" + id;

        authService.register(new RegisterRequest(
                "Security Customer A",
                usernameA + "@example.com",
                usernameA,
                "SecurityTest@123"
        ));

        authService.register(new RegisterRequest(
                "Security Customer B",
                usernameB + "@example.com",
                usernameB,
                "SecurityTest@123"
        ));

        Long customerA = userRepository
                .findByUsername(usernameA)
                .orElseThrow()
                .customerId();

        Long customerB = userRepository
                .findByUsername(usernameB)
                .orElseThrow()
                .customerId();

        accountA = accountRepository.createAccount(
                "A" + id.substring(0, 12),
                customerA
        );

        accountB = accountRepository.createAccount(
                "B" + id.substring(0, 12),
                customerB
        );

        userRepository.createAdminUser(
                adminUsername,
                passwordEncoder.encode("AdminTest@12345")
        );

        tokenA = jwtService.generateToken(usernameA);
        tokenB = jwtService.generateToken(usernameB);
        adminToken = jwtService.generateToken(adminUsername);
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {

        mockMvc.perform(
                get("/api/accounts/{id}/balance", accountA)
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturns401() throws Exception {

        mockMvc.perform(
                get("/api/accounts/{id}/balance", accountA)
                        .header(
                                "Authorization",
                                "Bearer invalid-token"
                        )
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void customerCanViewOwnBalance() throws Exception {

        mockMvc.perform(
                get("/api/accounts/{id}/balance", accountA)
                        .header("Authorization", "Bearer " + tokenA)
        ).andExpect(status().isOk());
    }

    @Test
    void customerCannotViewAnotherAccount() throws Exception {

        mockMvc.perform(
                get("/api/accounts/{id}/balance", accountB)
                        .header("Authorization", "Bearer " + tokenA)
        ).andExpect(status().isForbidden());
    }

    @Test
    void adminCanViewAnyAccount() throws Exception {

        mockMvc.perform(
                get("/api/accounts/{id}/balance", accountA)
                        .header("Authorization", "Bearer " + adminToken)
        ).andExpect(status().isOk());

        mockMvc.perform(
                get("/api/accounts/{id}/balance", accountB)
                        .header("Authorization", "Bearer " + adminToken)
        ).andExpect(status().isOk());
    }

    @Test
    void customerCannotAccessReports() throws Exception {

        mockMvc.perform(
                get("/api/reports/overview")
                        .header("Authorization", "Bearer " + tokenA)
        ).andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessReports() throws Exception {

        mockMvc.perform(
                get("/api/reports/overview")
                        .header("Authorization", "Bearer " + adminToken)
        ).andExpect(status().isOk());
    }

    @Test
    void customerCannotWithdrawFromAnotherAccount()
            throws Exception {

        String body = """
                {
                    "accountId": %d,
                    "amount": 100,
                    "transactionUuid": "%s"
                }
                """.formatted(accountA, UUID.randomUUID());

        mockMvc.perform(
                post("/api/transactions/withdraw")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andExpect(status().isForbidden());

        assertZeroBalance(accountA);
    }

    @Test
    void customerCannotDepositIntoAnotherAccount()
            throws Exception {

        String body = """
                {
                    "accountId": %d,
                    "amount": 100,
                    "transactionUuid": "%s"
                }
                """.formatted(accountA, UUID.randomUUID());

        mockMvc.perform(
                post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andExpect(status().isForbidden());

        assertZeroBalance(accountA);
    }

    @Test
    void customerCannotTransferFromAnotherAccount()
            throws Exception {

        String body = """
                {
                    "senderAccountId": %d,
                    "receiverAccountId": %d,
                    "amount": 100,
                    "transactionUuid": "%s"
                }
                """.formatted(
                        accountA,
                        accountB,
                        UUID.randomUUID()
                );

        mockMvc.perform(
                post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andExpect(status().isForbidden());

        assertZeroBalance(accountA);
        assertZeroBalance(accountB);
    }

    @Test
    void customerCannotViewAnotherAccountHistory()
            throws Exception {

        mockMvc.perform(
                get("/api/transactions/account/{id}", accountB)
                        .header("Authorization", "Bearer " + tokenA)
        ).andExpect(status().isForbidden());
    }

    @Test
    void customerCannotChangeAccountStatus()
            throws Exception {

        mockMvc.perform(
                patch("/api/accounts/{id}/status", accountA)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "INACTIVE"
                                }
                                """)
        ).andExpect(status().isForbidden());

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM accounts WHERE account_id = ?",
                String.class,
                accountA
        );

        assertEquals("ACTIVE", status);
    }

    @Test
    void adminCanChangeAccountStatus() throws Exception {

        mockMvc.perform(
                patch("/api/accounts/{id}/status", accountA)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "INACTIVE"
                                }
                                """)
        ).andExpect(status().isOk());

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM accounts WHERE account_id = ?",
                String.class,
                accountA
        );

        assertEquals("INACTIVE", status);
    }

    private void assertZeroBalance(Long accountId) {

        java.math.BigDecimal balance =
                jdbcTemplate.queryForObject(
                        "SELECT balance FROM accounts WHERE account_id = ?",
                        java.math.BigDecimal.class,
                        accountId
                );

        assertNotNull(balance);
        assertEquals(0, balance.signum());
    }
}
