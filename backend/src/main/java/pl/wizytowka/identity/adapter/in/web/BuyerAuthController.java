package pl.wizytowka.identity.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.config.security.BuyerSecurityProperties;
import pl.wizytowka.config.security.JwtProperties;
import pl.wizytowka.identity.application.port.in.AuthenticationFailedException;
import pl.wizytowka.identity.application.port.in.BuyerAuthenticationUseCase;
import pl.wizytowka.identity.application.port.in.BuyerRegistrationUseCase;
import pl.wizytowka.identity.application.port.in.RegisterBuyerCommand;
import pl.wizytowka.identity.adapter.in.web.BuyerSessionCookies.SessionResponse;
import pl.wizytowka.shared.adapter.in.web.RequestedLanguage;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.util.UUID;

/**
 * Publiczne endpointy kont kupców (bez logowania). Wszystkie wymagają nagłówka {@code X-Requested-With}
 * (AuthEndpointCsrfFilter) i mają limity zapytań. Rejestracja, ponowna wysyłka i prośba o reset zwracają
 * zawsze 202 — odpowiedź nie zdradza, czy konto o danym adresie istnieje.
 */
@RestController
@RequestMapping(BuyerSessionCookies.AUTH_PATH)
class BuyerAuthController {

    private final BuyerRegistrationUseCase registration;
    private final BuyerAuthenticationUseCase authentication;
    private final JwtProperties jwtProperties;
    private final BuyerSecurityProperties buyerProperties;

    BuyerAuthController(BuyerRegistrationUseCase registration, BuyerAuthenticationUseCase authentication,
                        JwtProperties jwtProperties, BuyerSecurityProperties buyerProperties) {
        this.registration = registration;
        this.authentication = authentication;
        this.jwtProperties = jwtProperties;
        this.buyerProperties = buyerProperties;
    }

    /** @param language język wiadomości e-mail i konta ({@code ?lang=} / {@code Accept-Language}) */
    @PostMapping("/register")
    ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request, @RequestedLanguage CatalogLanguage language) {
        registration.register(request.toCommand(language));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify-email")
    ResponseEntity<Void> verifyEmail(@Valid @RequestBody TokenRequest request) {
        registration.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    ResponseEntity<Void> resendVerification(@Valid @RequestBody EmailRequest request) {
        registration.resendVerification(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/request")
    ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        registration.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        registration.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request) {
        return BuyerSessionCookies.session(authentication.login(request.email(), request.password()), jwtProperties,
                buyerProperties);
    }

    @PostMapping("/refresh")
    ResponseEntity<?> refresh(@CookieValue(name = BuyerSessionCookies.COOKIE, required = false) String refreshToken) {
        try {
            return BuyerSessionCookies.session(authentication.refresh(refreshToken), jwtProperties, buyerProperties);
        } catch (AuthenticationFailedException e) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Session expired");
            problem.setProperty("code", e.code());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, BuyerSessionCookies.cleared(jwtProperties).toString())
                    .body(problem);
        }
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = BuyerSessionCookies.COOKIE, required = false) String refreshToken) {
        authentication.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, BuyerSessionCookies.cleared(jwtProperties).toString())
                .build();
    }

    record RegisterRequest(@NotBlank @Size(max = 254) String email,
                           @NotBlank @Size(max = 1024) String password,
                           @NotBlank @Size(max = 200) String companyName,
                           @NotBlank @Size(max = 2) String countryCode,
                           @NotBlank @Size(max = 40) String taxId,
                           @NotBlank @Size(max = 120) String contactName,
                           @NotBlank @Size(max = 32) String phone,
                           boolean termsAccepted,
                           boolean privacyAccepted,
                           UUID termsVersionId,
                           UUID privacyVersionId) {

        RegisterBuyerCommand toCommand(CatalogLanguage language) {
            return new RegisterBuyerCommand(email, password, companyName, countryCode, taxId, contactName, phone,
                    language, termsAccepted, privacyAccepted, termsVersionId, privacyVersionId);
        }

        @Override
        public String toString() {
            return "RegisterRequest[email=" + email + ", country=" + countryCode + "]";
        }
    }

    record TokenRequest(@NotBlank @Size(max = 200) String token) {

        @Override
        public String toString() {
            return "TokenRequest[***]";
        }
    }

    record EmailRequest(@NotBlank @Size(max = 254) String email) {
    }

    record PasswordResetRequest(@NotBlank @Size(max = 200) String token, @NotBlank @Size(max = 1024) String password) {

        @Override
        public String toString() {
            return "PasswordResetRequest[***]";
        }
    }

    record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 1024) String password) {

        @Override
        public String toString() {
            return "LoginRequest[email=" + email + "]";
        }
    }
}
