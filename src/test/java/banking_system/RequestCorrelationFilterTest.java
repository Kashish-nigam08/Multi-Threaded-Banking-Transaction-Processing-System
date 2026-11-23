
package banking_system;

import banking_system.filter.RequestCorrelationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter =
            new RequestCorrelationFilter();

    @Test
    void shouldGenerateCorrelationIdForEveryRequest()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (req, res) -> {
                    String correlationId =
                            MDC.get("correlationId");

                    assertNotNull(correlationId);
                    assertFalse(correlationId.isBlank());
                }
        );

        String responseId =
                response.getHeader("X-Correlation-ID");

        assertNotNull(responseId);
        assertDoesNotThrow(
                () -> java.util.UUID.fromString(responseId)
        );

        // MDC must be cleared after the request.
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void shouldGenerateDifferentIdsForDifferentRequests()
            throws Exception {

        MockHttpServletResponse firstResponse =
                new MockHttpServletResponse();

        MockHttpServletResponse secondResponse =
                new MockHttpServletResponse();

        FilterChain chain = (request, response) -> {};

        filter.doFilter(
                new MockHttpServletRequest(),
                firstResponse,
                chain
        );

        filter.doFilter(
                new MockHttpServletRequest(),
                secondResponse,
                chain
        );

        assertNotEquals(
                firstResponse.getHeader("X-Correlation-ID"),
                secondResponse.getHeader("X-Correlation-ID")
        );
    }
}
