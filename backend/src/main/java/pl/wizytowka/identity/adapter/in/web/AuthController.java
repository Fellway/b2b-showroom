package pl.wizytowka.identity.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.config.security.JwtProperties;
import pl.wizytowka.identity.application.port.in.AdminUserView;
import pl.wizytowka.identity.application.port.in.AuthenticateUseCase;
import pl.wizytowka.identity.application.port.in.AuthenticationFailedException;
import pl.wizytowka.identity.application.port.in.AuthenticationResult;
import pl.wizytowka.identity.application.port.in.CurrentAdminQuery;
import pl.wizytowka.identity.domain.AdminRole;
import pl.wizytowka.identity.domain.AdminUserId;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Sesja panelu: access token wraca w treści odpowiedzi (frontend trzyma go tylko w pamięci),
 * refresh token wyłącznie w ciasteczku httpOnly ograniczonym do ścieżki tego kontrolera.
 */
@RestController
@RequestMapping(AuthController.BASE_PATH)
class AuthController {

    static final String BASE_PATH = "/api/v1/admin/auth";
    static final String REFRESH_COOKIE = "showroom_refresh";

    private final AuthenticateUseCase authenticate;
    private final CurrentAdminQuery currentAdmin;
    private final JwtProperties jwtProperties;

    AuthController(AuthenticateUseCase authenticate, CurrentAdminQuery currentAdmin, JwtProperties jwtProperties) {
        this.authenticate = authenticate;
        this.currentAdmin = currentAdmin;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/login")
    ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request) {
        return session(authenticate.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    ResponseEntity<?> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        try {
            return session(authenticate.refresh(refreshToken));
        } catch (AuthenticationFailedException e) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Session expired");
            problem.setProperty("code", e.code());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, clearedCookie().toString())
                    .body(problem);
        }
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authenticate.logout(refreshToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearedCookie().toString()).build();
    }

    @GetMapping("/me")
    AdminResponse me(@AuthenticationPrincipal Jwt jwt) {
        return AdminResponse.of(currentAdmin.me(AdminUserId.of(UUID.fromString(jwt.getSubject()))));
    }

    private ResponseEntity<SessionResponse> session(AuthenticationResult result) {
        ResponseCookie cookie = refreshCookie(result.refreshToken(), jwtProperties.refreshTokenTtl());
        SessionResponse body = new SessionResponse(result.accessToken().value(), "Bearer",
                result.accessToken().expiresAt(), AdminResponse.of(result.admin()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }

    private ResponseCookie clearedCookie() {
        return refreshCookie("", Duration.ZERO);
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite("Strict")
                .path(BASE_PATH)
                .maxAge(maxAge)
                .build();
    }

    record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 1024) String password) {

        @Override
        public String toString() {
            return "LoginRequest[email=" + email + "]";
        }
    }

    record SessionResponse(String accessToken, String tokenType, Instant expiresAt, AdminResponse admin) {
    }

    record AdminResponse(UUID id, String email, String displayName, AdminRole role) {

        static AdminResponse of(AdminUserView view) {
            return new AdminResponse(view.id(), view.email(), view.displayName(), view.role());
        }
    }
}
