package pl.wizytowka.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ochrona CSRF endpointów sesji ({@code /api/v1/admin/auth/**}), jedynych, które polegają na ciasteczku.
 * Wymagamy nagłówka {@code X-Requested-With: XMLHttpRequest}: przeglądarka nie wyśle niestandardowego nagłówka
 * cross-origin bez preflightu CORS, a API nie odpowiada na preflight zgodą (brak konfiguracji CORS). To druga
 * warstwa obok SameSite=Strict, niezależna od nagłówków Origin/Host przepisywanych przez reverse proxy.
 */
class AuthEndpointCsrfFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Requested-With";
    static final String EXPECTED_VALUE = "XMLHttpRequest";
    private static final String AUTH_PATH_PREFIX = "/api/v1/admin/auth/";

    private final ObjectMapper objectMapper;

    AuthEndpointCsrfFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !request.getRequestURI().startsWith(AUTH_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!EXPECTED_VALUE.equals(request.getHeader(HEADER))) {
            ProblemResponses.write(response, objectMapper, HttpStatus.FORBIDDEN, "auth.csrf-header-missing",
                    "Header " + HEADER + ": " + EXPECTED_VALUE + " is required");
            return;
        }
        chain.doFilter(request, response);
    }
}
