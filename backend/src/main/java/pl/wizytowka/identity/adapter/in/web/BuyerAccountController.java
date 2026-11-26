package pl.wizytowka.identity.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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
import pl.wizytowka.identity.application.port.in.BuyerConsentView;
import pl.wizytowka.identity.application.port.in.BuyerLegalUseCase;
import pl.wizytowka.identity.application.port.in.BuyerSelfServiceUseCase;
import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.linesheet.application.port.in.BuyerLineSheetQuery;
import pl.wizytowka.linesheet.application.view.LineSheetSummaryView;
import pl.wizytowka.shared.adapter.in.web.RequestedLanguage;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Konto zalogowanego kupca (rola BUYER — reguła w SecurityConfig). */
@RestController
@RequestMapping("/api/v1/buyer/me")
class BuyerAccountController {

    private final BuyerSelfServiceUseCase selfService;
    private final BuyerLegalUseCase legal;
    private final BuyerLineSheetQuery lineSheets;
    private final Clock clock;
    private final JwtProperties jwtProperties;
    private final BuyerSecurityProperties buyerProperties;

    BuyerAccountController(BuyerSelfServiceUseCase selfService, BuyerLegalUseCase legal, BuyerLineSheetQuery lineSheets,
                           Clock clock, JwtProperties jwtProperties, BuyerSecurityProperties buyerProperties) {
        this.selfService = selfService;
        this.legal = legal;
        this.lineSheets = lineSheets;
        this.clock = clock;
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

    // ------------------------------------------------------------------ dokumenty prawne (D25)

    /** Dokumenty do zaakceptowania / przyjęcia do wiadomości i historia zgód. */
    @GetMapping("/legal")
    BuyerLegalUseCase.BuyerLegalStatus legal(@AuthenticationPrincipal Jwt jwt) {
        return legal.status(buyer(jwt));
    }

    @PostMapping("/legal/accept")
    BuyerLegalUseCase.BuyerLegalStatus accept(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AcceptRequest request,
                                              @RequestedLanguage CatalogLanguage language) {
        legal.accept(buyer(jwt), request.versionIds(), language);
        return legal.status(buyer(jwt));
    }

    // ------------------------------------------------------------------ prawa z RODO

    /** Eksport danych (art. 15 i 20 RODO): konto, historia zgód i zestawienia — plik JSON do pobrania. */
    @GetMapping("/export")
    ResponseEntity<DataExport> export(@AuthenticationPrincipal Jwt jwt) {
        BuyerAccountId id = buyer(jwt);
        BuyerSelfServiceUseCase.BuyerDataExport data = selfService.export(id);
        List<LineSheetSummaryView> sheets = new ArrayList<>();
        PageResult<LineSheetSummaryView> page;
        int index = 0;
        do {
            page = lineSheets.lineSheetsOfBuyer(id.value(), new PageQuery(index++, PageQuery.MAX_SIZE));
            sheets.addAll(page.items());
        } while (index < page.totalPages());
        Instant now = clock.instant();
        DataExport body = new DataExport(now, BuyerResponse.of(data.account()), data.emailVerifiedAt(),
                data.reviewedAt(), data.lastLoginAt(), data.consents(),
                sheets.stream().map(ExportedLineSheet::of).toList());
        String filename = "showroom-data-" + DateTimeFormatter.ISO_LOCAL_DATE.format(now.atZone(ZoneOffset.UTC)) + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    /** Usunięcie konta przez kupca (art. 17 RODO) — wymaga hasła; kończy sesję (czyści ciasteczko). */
    @PostMapping("/delete")
    ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeleteAccountRequest request) {
        selfService.deleteAccount(buyer(jwt), request.currentPassword());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, BuyerSessionCookies.cleared(jwtProperties).toString())
                .build();
    }

    static BuyerAccountId buyer(Jwt jwt) {
        return BuyerAccountId.of(UUID.fromString(jwt.getSubject()));
    }

    record UpdateProfileRequest(@NotBlank @Size(max = 120) String contactName,
                                @NotBlank @Size(max = 32) String phone,
                                @NotBlank @Size(max = 2) String language,
                                @NotNull @PositiveOrZero Long version) {
    }

    record AcceptRequest(@NotEmpty @Size(max = 10) List<@NotNull UUID> versionIds) {
    }

    record DeleteAccountRequest(@NotBlank @Size(max = 1024) String currentPassword) {

        @Override
        public String toString() {
            return "DeleteAccountRequest[***]";
        }
    }

    /** Plik eksportu — format opisany w polityce prywatności („pobranie danych”). */
    record DataExport(Instant generatedAt,
                      BuyerResponse account,
                      Instant emailVerifiedAt,
                      Instant reviewedAt,
                      Instant lastLoginAt,
                      List<BuyerConsentView> consents,
                      List<ExportedLineSheet> lineSheets) {
    }

    record ExportedLineSheet(String referenceNumber, String language, Instant createdAt, int itemCount,
                             String companyName, String contactName, String email, String phone) {

        static ExportedLineSheet of(LineSheetSummaryView view) {
            return new ExportedLineSheet(view.referenceNumber(), view.language().code(), view.createdAt(),
                    view.itemCount(), view.companyName(), view.contactName(), view.email(), view.phone());
        }
    }

    record ChangePasswordRequest(@NotBlank @Size(max = 1024) String currentPassword,
                                 @NotBlank @Size(max = 1024) String newPassword) {

        @Override
        public String toString() {
            return "ChangePasswordRequest[***]";
        }
    }
}
