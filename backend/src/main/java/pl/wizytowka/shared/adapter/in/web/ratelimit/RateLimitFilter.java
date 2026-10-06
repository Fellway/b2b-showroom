package pl.wizytowka.shared.adapter.in.web.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.net.URI;

/**
 * Odrzuca nadmiarowe żądania z {@code 429 Too Many Requests} zanim dotrą do Spring Security, bazy
 * czy generowania PDF. Odpowiedź ma format reszty API (RFC 9457 + {@code code}) i nagłówek {@code Retry-After}.
 */
final class RateLimitFilter extends OncePerRequestFilter {

    static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final ClientRateLimiter limiter;
    private final ObjectMapper objectMapper;
    private final UrlPathHelper pathHelper;

    RateLimitFilter(ClientRateLimiter limiter, ObjectMapper objectMapper) {
        this.limiter = limiter;
        this.objectMapper = objectMapper;
        // Ścieżka zdekodowana i bez ";…" — tak jak dopasowuje ją routing; "/line-shee%74s" nie ominie limitu.
        this.pathHelper = new UrlPathHelper();
        this.pathHelper.setUrlDecode(true);
        this.pathHelper.setRemoveSemicolonContent(true);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = pathHelper.getPathWithinApplication(request);
        RateLimitDecision decision = limiter.tryAcquire(request.getMethod(), path, ClientKeys.of(request.getRemoteAddr()));
        switch (decision) {
            case RateLimitDecision.NotLimited ignored -> chain.doFilter(request, response);
            case RateLimitDecision.Allowed allowed -> {
                response.setHeader(REMAINING_HEADER, Long.toString(allowed.remaining()));
                chain.doFilter(request, response);
            }
            case RateLimitDecision.Rejected rejected -> reject(request, response, rejected);
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, RateLimitDecision.Rejected rejected)
            throws IOException {
        log.debug("Rate limit '{}' exceeded", rejected.rule());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests; try again in " + rejected.retryAfterSeconds() + " s");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "request.rate-limited");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(rejected.retryAfterSeconds()));
        response.setHeader(REMAINING_HEADER, "0");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
