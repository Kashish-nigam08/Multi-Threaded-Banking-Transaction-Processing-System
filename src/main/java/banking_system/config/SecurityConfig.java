package banking_system.config;

import banking_system.security.CustomUserDetailsService;
import banking_system.security.JwtAuthenticationFilter;
import banking_system.security.JwtService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    @Profile("!benchmark")
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            CustomUserDetailsService userDetailsService
    ) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(
                        jwtService,
                        userDetailsService
                );

        http
                .csrf(AbstractHttpConfigurer::disable)

                .formLogin(AbstractHttpConfigurer::disable)

                .httpBasic(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

        // Public authentication endpoints
        .requestMatchers(
                HttpMethod.POST,
                "/api/auth/register",
                "/api/auth/login"
        ).permitAll()

        // Customer self-service account creation
        .requestMatchers(
                HttpMethod.POST,
                "/api/accounts/me"
        ).hasRole("CUSTOMER")

        // Administrator account creation
        .requestMatchers(
                HttpMethod.POST,
                "/api/accounts"
        ).hasRole("ADMIN")

        // Administrator account status management
        .requestMatchers(
                HttpMethod.PATCH,
                "/api/accounts/*/status"
        ).hasRole("ADMIN")

        // Account balance
        .requestMatchers(
                HttpMethod.GET,
                "/api/accounts/*/balance"
        ).hasAnyRole("CUSTOMER", "ADMIN")

        // Financial transactions
        .requestMatchers(
                HttpMethod.POST,
                "/api/transactions/deposit",
                "/api/transactions/withdraw",
                "/api/transactions/transfer"
        ).hasAnyRole("CUSTOMER", "ADMIN")

        // Transaction history
        .requestMatchers(
                HttpMethod.GET,
                "/api/transactions/account/*"
        ).hasAnyRole("CUSTOMER", "ADMIN")

        // Administrator reporting
        .requestMatchers("/api/reports/**")
        .hasRole("ADMIN")

        // Deny all unspecified endpoints
        .anyRequest().denyAll()
)

                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint(
                                (request, response, error) -> {
                                    response.setStatus(401);
                                    response.setContentType(
                                            "application/json"
                                    );
                                    response.getWriter().write(
                                            """
                                            {
                                              "status": 401,
                                              "error": "UNAUTHORIZED",
                                              "message": "Authentication required"
                                            }
                                            """
                                    );
                                }
                        )

                        .accessDeniedHandler(
                                (request, response, error) -> {
                                    response.setStatus(403);
                                    response.setContentType(
                                            "application/json"
                                    );
                                    response.getWriter().write(
                                            """
                                            {
                                              "status": 403,
                                              "error": "ACCESS_DENIED",
                                              "message": "Insufficient permissions"
                                            }
                                            """
                                    );
                                }
                        )
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
