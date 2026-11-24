package pl.wizytowka.identity.adapter.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import pl.wizytowka.config.security.BuyerSecurityProperties;
import pl.wizytowka.config.security.JwtProperties;
import pl.wizytowka.identity.application.port.in.BuyerSession;

import java.time.Duration;
import java.time.Instant;

/**
 * Sesja kupca w odpowiedzi HTTP: access token w treści (frontend trzyma go tylko w pamięci), refresh token
 * wyłącznie w ciasteczku httpOnly ograniczonym do ścieżki endpointów sesji kupca — osobnym niż ciasteczko panelu.
 */
final class BuyerSessionCookies {

    static final String AUTH_PATH = "/api/v1/buyer/auth";
    static final String COOKIE = "showroom_buyer_refresh";

    private BuyerSessionCookies() {
    }

    static ResponseEntity<SessionResponse> session(BuyerSession session, JwtProperties jwt, BuyerSecurityProperties buyer) {
        ResponseCookie cookie = cookie(session.refreshToken(), buyer.refreshTokenTtl(), jwt);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new SessionResponse(session.accessToken().value(), "Bearer", session.accessToken().expiresAt(),
                        BuyerResponse.of(session.buyer())));
    }

    static ResponseCookie cleared(JwtProperties jwt) {
        return cookie("", Duration.ZERO, jwt);
    }

    private static ResponseCookie cookie(String value, Duration maxAge, JwtProperties jwt) {
        return ResponseCookie.from(COOKIE, value)
                .httpOnly(true)
                .secure(jwt.refreshCookieSecure())
                .sameSite("Strict")
                .path(AUTH_PATH)
                .maxAge(maxAge)
                .build();
    }

    record SessionResponse(String accessToken, String tokenType, Instant expiresAt, BuyerResponse buyer) {
    }
}
