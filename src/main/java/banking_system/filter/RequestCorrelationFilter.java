
package banking_system.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME =
            "X-Correlation-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Generate a server-controlled ID for this request.
        String correlationId = UUID.randomUUID().toString();

        response.setHeader(HEADER_NAME, correlationId);

        try {
            MDC.put("correlationId", correlationId);

            filterChain.doFilter(request, response);

        } finally {
            MDC.remove("correlationId");
        }
    }
}
