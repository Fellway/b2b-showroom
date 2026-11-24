package pl.wizytowka.identity.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.config.security.BuyerSecurityProperties;
import pl.wizytowka.config.security.JwtProperties;
import pl.wizytowka.identity.adapter.in.web.BuyerSessionCookies.SessionResponse;
import pl.wizytowka.identity.application.port.in.BuyerSelfServiceUseCase;
import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.util.UUID;

/** Konto zalogowanego kupca (rola BUYER — reguła w SecurityConfig). */
@RestController
@RequestMapping("/api/v1/buyer/me")
class BuyerAccountController {

    private final BuyerSelfServiceUseCase selfService;
    private final JwtProperties jwtProperties;
    private final BuyerSecurityProperties buyerProperties;

    BuyerAccountController(BuyerSelfServiceUseCase selfService, JwtProperties jwtProperties,
                           BuyerSecurityProperties buyerProperties) {
        this.selfService = selfService;
        this.jwtProperties = jwtProperties;
        this.buyerProperties = buyerProperties;
    }

    @GetMapping
    BuyerResponse me(@AuthenticationPrincipal Jwt jwt) {
        return BuyerResponse.of(selfService.me(buyer(jwt)));
    }

    @PutMapping
    ResponseEntity<Void> update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        CatalogLanguage language = CatalogLanguage.fromCode(request.language())
                .orElseThrow(() -> new InvalidValueException("identity.buyer.language", "Unsupported language"));
        selfService.updateProfile(buyer(jwt), request.version(), request.contactName(), request.phone(), language);
        return ResponseEntity.noContent().build();
    }

    /** Zmiana hasła kończy pozostałe sesje i zwraca nową (jak logowanie). */
    @PostMapping("/password")
    ResponseEntity<SessionResponse> changePassword(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody ChangePasswordRequest request) {
        return BuyerSessionCookies.session(
                selfService.changePassword(buyer(jwt), request.currentPassword(), request.newPassword()),
                jwtProperties, buyerProperties);
    }

    static BuyerAccountId buyer(Jwt jwt) {
        return BuyerAccountId.of(UUID.fromString(jwt.getSubject()));
    }

    record UpdateProfileRequest(@NotBlank @Size(max = 120) String contactName,
                                @NotBlank @Size(max = 32) String phone,
                                @NotBlank @Size(max = 2) String language,
                                @NotNull @PositiveOrZero Long version) {
    }

    record ChangePasswordRequest(@NotBlank @Size(max = 1024) String currentPassword,
                                 @NotBlank @Size(max = 1024) String newPassword) {

        @Override
        public String toString() {
            return "ChangePasswordRequest[***]";
        }
    }
}
