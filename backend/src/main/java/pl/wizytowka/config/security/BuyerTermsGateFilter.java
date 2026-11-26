package pl.wizytowka.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;
import pl.wizytowka.identity.application.port.in.BuyerLegalUseCase;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Brama regulaminu (D25): kupiec, który nie zaakceptował obowiązującej (wymagającej akceptacji) wersji regulaminu,
 * nie dostaje cen, EAN ani zestawień — dopóki jej nie zaakceptuje w „Moim koncie”. Działa na poziomie API, nie
 * tylko interfejsu. Stan sprawdzany przy każdym żądaniu (nowa wersja obowiązuje od razu, nie po wygaśnięciu tokenu).
 */
class BuyerTermsGateFilter extends OncePerRequestFilter {

    static final String CODE = "identity.buyer.terms-acceptance-required";
    private static final List<String> GATED_PREFIXES = List.of("/api/v1/buyer/catalog/", "/api/v1/buyer/line-sheets");
    private static final String BUYER_AUTHORITY = "ROLE_" + SecurityConfig.BUYER_ROLE;

    private final BuyerLegalUseCase legal;
    private final ObjectMapper objectMapper;

    BuyerTermsGateFilter(BuyerLegalUseCase legal, ObjectMapper objectMapper) {
        this.legal = legal;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        return GATED_PREFIXES.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token
                && token.getAuthorities().stream().anyMatch(a -> BUYER_AUTHORITY.equals(a.getAuthority()))
                && mustAcceptTerms(token.getToken().getSubject())) {
            ProblemResponses.write(response, objectMapper, HttpStatus.FORBIDDEN, CODE,
                    "The current terms of service must be accepted first");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean mustAcceptTerms(String subject) {
        try {
            return legal.mustAcceptTerms(UUID.fromString(subject));
        } catch (IllegalArgumentException e) {
            return true; // token kupca zawsze ma UUID konta — inny podmiot nie przechodzi bramy
        }
    }
}
