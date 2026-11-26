package pl.wizytowka.legal.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.wizytowka.legal.application.port.in.ManageLegalDocumentsUseCase;
import pl.wizytowka.legal.application.view.AdminLegalOverview;
import pl.wizytowka.legal.application.view.AdminLegalVersionView;
import pl.wizytowka.legal.domain.LegalDocumentVersionId;
import pl.wizytowka.legal.domain.LegalText;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.net.URI;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dokumenty prawne w panelu (D25). Szkice edytują ADMIN i EDITOR; publikację — zmianę warunków dla wszystkich
 * kupców — wyłącznie ADMIN (reguła w SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/admin/legal")
class AdminLegalController {

    private final ManageLegalDocumentsUseCase manage;

    AdminLegalController(ManageLegalDocumentsUseCase manage) {
        this.manage = manage;
    }

    @GetMapping
    List<AdminLegalOverview> overview() {
        return manage.overview();
    }

    @GetMapping("/versions/{id}")
    AdminLegalVersionView version(@PathVariable UUID id) {
        return manage.version(LegalDocumentVersionId.of(id));
    }

    /** Szkic następnej wersji — kopia obowiązującej treści. */
    @PostMapping("/{slug}/drafts")
    ResponseEntity<AdminLegalVersionView> createDraft(@PathVariable String slug) {
        LegalDocumentVersionId id = manage.createDraft(PublicLegalController.type(slug));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/admin/legal/versions/{id}").buildAndExpand(id.value()).toUri();
        return ResponseEntity.created(location).body(manage.version(id));
    }

    @PutMapping("/versions/{id}")
    AdminLegalVersionView update(@PathVariable UUID id, @Valid @RequestBody UpdateDraftRequest request) {
        LegalDocumentVersionId versionId = LegalDocumentVersionId.of(id);
        manage.updateDraft(versionId, request.version(), request.toTexts());
        return manage.version(versionId);
    }

    @PostMapping("/versions/{id}/publish")
    AdminLegalVersionView publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                  @Valid @RequestBody PublishRequest request) {
        LegalDocumentVersionId versionId = LegalDocumentVersionId.of(id);
        manage.publish(versionId, request.version(), Boolean.TRUE.equals(request.requireAcceptance()),
                UUID.fromString(jwt.getSubject()));
        return manage.version(versionId);
    }

    @DeleteMapping("/versions/{id}")
    ResponseEntity<Void> deleteDraft(@PathVariable UUID id) {
        manage.deleteDraft(LegalDocumentVersionId.of(id));
        return ResponseEntity.noContent().build();
    }

    /**
     * @param texts treść w językach ({@code pl}, {@code en}…); język bez tytułu i treści = tłumaczenie do uzupełnienia
     */
    record UpdateDraftRequest(@NotNull @Size(max = 4) Map<String, @Valid TextRequest> texts,
                              @NotNull @PositiveOrZero Long version) {

        Map<CatalogLanguage, LegalText> toTexts() {
            Map<CatalogLanguage, LegalText> result = new EnumMap<>(CatalogLanguage.class);
            texts.forEach((code, text) -> {
                CatalogLanguage language = CatalogLanguage.fromCode(code)
                        .orElseThrow(() -> new InvalidValueException("legal.text.language", "Unsupported language: " + code));
                if (text != null && !text.isEmpty()) {
                    result.put(language, new LegalText(text.title(), text.body(), text.changeSummary()));
                }
            });
            return result;
        }
    }

    record TextRequest(@Size(max = LegalText.MAX_TITLE) String title,
                       @Size(max = LegalText.MAX_BODY) String body,
                       @Size(max = LegalText.MAX_SUMMARY) String changeSummary) {

        boolean isEmpty() {
            return (title == null || title.isBlank()) && (body == null || body.isBlank());
        }
    }

    record PublishRequest(@NotNull @PositiveOrZero Long version, Boolean requireAcceptance) {
    }
}
