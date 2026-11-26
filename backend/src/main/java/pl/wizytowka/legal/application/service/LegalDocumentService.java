package pl.wizytowka.legal.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.legal.application.port.in.LegalDocumentQuery;
import pl.wizytowka.legal.application.port.in.ManageLegalDocumentsUseCase;
import pl.wizytowka.legal.application.port.out.LegalDocumentRepository;
import pl.wizytowka.legal.application.view.AdminLegalOverview;
import pl.wizytowka.legal.application.view.AdminLegalVersionView;
import pl.wizytowka.legal.application.view.LegalDocumentView;
import pl.wizytowka.legal.application.view.LegalRequirement;
import pl.wizytowka.legal.application.view.LegalVersionRef;
import pl.wizytowka.legal.application.view.LegalVersionSummary;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.legal.domain.LegalDocumentVersion;
import pl.wizytowka.legal.domain.LegalDocumentVersionId;
import pl.wizytowka.legal.domain.LegalText;
import pl.wizytowka.shared.application.Versions;
import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ResourceConflictException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.time.Clock;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
class LegalDocumentService implements LegalDocumentQuery, ManageLegalDocumentsUseCase {

    private final LegalDocumentRepository documents;
    private final Clock clock;

    LegalDocumentService(LegalDocumentRepository documents, Clock clock) {
        this.documents = documents;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ odczyt publiczny

    @Override
    @Transactional(readOnly = true)
    public Optional<LegalDocumentView> current(LegalDocumentType type, CatalogLanguage language) {
        return documents.findLatestPublished(type).map(version -> toView(version, language));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LegalDocumentView> publishedVersion(UUID versionId, CatalogLanguage language) {
        return documents.findById(LegalDocumentVersionId.of(versionId))
                .filter(LegalDocumentVersion::isPublished)
                .map(version -> toView(version, language));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LegalVersionSummary> publishedVersions(LegalDocumentType type) {
        return documents.findPublished(type).stream().map(LegalVersionSummary::of).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LegalRequirement> requirements() {
        return Arrays.stream(LegalDocumentType.values())
                .map(this::requirementOf)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LegalRequirement> requirement(LegalDocumentType type) {
        return requirementOf(type);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LegalVersionRef> publishedVersionRef(UUID versionId) {
        if (versionId == null) {
            return Optional.empty();
        }
        return documents.findById(LegalDocumentVersionId.of(versionId))
                .filter(LegalDocumentVersion::isPublished)
                .map(v -> new LegalVersionRef(v.id().value(), v.type(), v.versionNumber()));
    }

    private Optional<LegalRequirement> requirementOf(LegalDocumentType type) {
        List<LegalDocumentVersion> published = documents.findPublished(type);
        if (published.isEmpty()) {
            return Optional.empty();
        }
        LegalDocumentVersion current = published.getFirst();
        // regulamin: wystarczy akceptacja najnowszej wersji wymagającej akceptacji (poprawki redakcyjne jej nie
        // wymagają); polityka prywatności: informujemy o każdej nowej wersji
        int minimum = type.canRequireAcceptance()
                ? published.stream().filter(LegalDocumentVersion::acceptanceRequired)
                        .mapToInt(LegalDocumentVersion::versionNumber).max().orElse(current.versionNumber())
                : current.versionNumber();
        return Optional.of(new LegalRequirement(type, current.id().value(), current.versionNumber(), minimum,
                type.canRequireAcceptance()));
    }

    // ------------------------------------------------------------------ panel

    @Override
    @Transactional(readOnly = true)
    public List<AdminLegalOverview> overview() {
        return Arrays.stream(LegalDocumentType.values()).map(type -> {
            List<LegalVersionSummary> published = publishedVersions(type);
            return new AdminLegalOverview(type, published.isEmpty() ? null : published.getFirst(),
                    documents.findDraft(type).map(LegalVersionSummary::of).orElse(null), published);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminLegalVersionView version(LegalDocumentVersionId id) {
        return AdminLegalVersionView.of(load(id));
    }

    @Override
    public LegalDocumentVersionId createDraft(LegalDocumentType type) {
        if (documents.findDraft(type).isPresent()) {
            throw new ResourceConflictException("legal.document.draft-exists",
                    "A draft of this document already exists");
        }
        Map<CatalogLanguage, LegalText> texts = documents.findLatestPublished(type)
                .map(LegalDocumentService::withoutChangeSummary)
                .orElse(Map.of());
        LegalDocumentVersion draft = LegalDocumentVersion.draft(type, texts, clock.instant());
        documents.save(draft);
        return draft.id();
    }

    @Override
    public void updateDraft(LegalDocumentVersionId id, long expectedVersion, Map<CatalogLanguage, LegalText> texts) {
        LegalDocumentVersion draft = load(id);
        Versions.require(expectedVersion, draft.version());
        draft.updateTexts(texts);
        documents.save(draft);
    }

    @Override
    public void publish(LegalDocumentVersionId id, long expectedVersion, boolean requireAcceptance, UUID actor) {
        LegalDocumentVersion draft = load(id);
        Versions.require(expectedVersion, draft.version());
        int next = documents.findLatestPublished(draft.type()).map(v -> v.versionNumber() + 1).orElse(1);
        draft.publish(next, requireAcceptance, actor, clock.instant());
        documents.save(draft);
    }

    @Override
    public void deleteDraft(LegalDocumentVersionId id) {
        LegalDocumentVersion version = load(id);
        if (!version.isDraft()) {
            throw new BusinessRuleViolationException("legal.document.published-immutable",
                    "A published version cannot be deleted");
        }
        documents.delete(version);
    }

    @Override
    public boolean publishTemplateIfEmpty(LegalDocumentType type, Map<CatalogLanguage, LegalText> texts) {
        if (documents.existsAny(type)) {
            return false;
        }
        LegalDocumentVersion version = LegalDocumentVersion.draft(type, texts, clock.instant());
        version.publish(1, true, null, clock.instant());
        documents.save(version);
        return true;
    }

    // ------------------------------------------------------------------ pomocnicze

    private LegalDocumentVersion load(LegalDocumentVersionId id) {
        return documents.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("legal.document.not-found", "Document version not found"));
    }

    /** Szkic kopiuje treść, ale „co się zmieniło” dotyczy tylko poprzedniej wersji. */
    private static Map<CatalogLanguage, LegalText> withoutChangeSummary(LegalDocumentVersion version) {
        EnumMap<CatalogLanguage, LegalText> copy = new EnumMap<>(CatalogLanguage.class);
        version.texts().forEach((language, text) -> copy.put(language, new LegalText(text.title(), text.body(), null)));
        return copy;
    }

    private static LegalDocumentView toView(LegalDocumentVersion version, CatalogLanguage requested) {
        CatalogLanguage language = version.resolvedLanguage(requested);
        LegalText text = version.text(language).orElseThrow();
        return new LegalDocumentView(version.id().value(), version.type(), version.versionNumber(),
                version.publishedAt(), version.acceptanceRequired(), language, text.title(), text.body(),
                text.changeSummary());
    }
}
