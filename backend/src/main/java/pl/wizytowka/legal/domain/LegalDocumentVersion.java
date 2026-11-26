package pl.wizytowka.legal.domain;

import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Jedna wersja dokumentu prawnego (D25) we wszystkich językach. Wersja powstaje jako szkic, który marka edytuje
 * w panelu; publikacja nadaje kolejny numer i od tej chwili treść jest <strong>niezmienna</strong> — rejestr zgód
 * kupców wskazuje konkretną wersję, więc zawsze da się wykazać, co dokładnie zaakceptowano (art. 7 ust. 1 RODO).
 *
 * <p>Szkic może mieć niekompletne tłumaczenia; publikacja wymaga treści we wszystkich językach interfejsu.</p>
 */
public final class LegalDocumentVersion {

    public enum Status { DRAFT, PUBLISHED }

    private final LegalDocumentVersionId id;
    private final LegalDocumentType type;
    private Status status;
    private Integer versionNumber;
    private final Map<CatalogLanguage, LegalText> texts;
    private boolean acceptanceRequired;
    private Instant publishedAt;
    private UUID publishedBy;
    private final Instant createdAt;
    private final long version;

    private LegalDocumentVersion(LegalDocumentVersionId id, LegalDocumentType type, Status status, Integer versionNumber,
                                 Map<CatalogLanguage, LegalText> texts, boolean acceptanceRequired, Instant publishedAt,
                                 UUID publishedBy, Instant createdAt, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.status = Objects.requireNonNull(status, "status");
        this.versionNumber = versionNumber;
        this.texts = new EnumMap<>(CatalogLanguage.class);
        this.texts.putAll(Objects.requireNonNull(texts, "texts"));
        this.acceptanceRequired = acceptanceRequired;
        this.publishedAt = publishedAt;
        this.publishedBy = publishedBy;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.version = version;
        if (status == Status.PUBLISHED && (versionNumber == null || publishedAt == null)) {
            throw new IllegalStateException("Published version requires a number and a publication time");
        }
    }

    public static LegalDocumentVersion draft(LegalDocumentType type, Map<CatalogLanguage, LegalText> texts, Instant now) {
        return new LegalDocumentVersion(LegalDocumentVersionId.newId(), type, Status.DRAFT, null, texts, false, null,
                null, now, 0);
    }

    public static LegalDocumentVersion restore(LegalDocumentVersionId id, LegalDocumentType type, Status status,
                                               Integer versionNumber, Map<CatalogLanguage, LegalText> texts,
                                               boolean acceptanceRequired, Instant publishedAt, UUID publishedBy,
                                               Instant createdAt, long version) {
        return new LegalDocumentVersion(id, type, status, versionNumber, texts, acceptanceRequired, publishedAt,
                publishedBy, createdAt, version);
    }

    /** Zastępuje treść szkicu (brak języka w mapie = tłumaczenie do uzupełnienia). */
    public void updateTexts(Map<CatalogLanguage, LegalText> newTexts) {
        requireDraft();
        texts.clear();
        texts.putAll(Objects.requireNonNull(newTexts, "texts"));
    }

    /**
     * Publikuje szkic jako wersję {@code number}.
     *
     * @param requireAcceptance czy kupcy muszą zaakceptować tę wersję przed dalszym korzystaniem z konta — tylko
     *                          dla regulaminu; pierwsza wersja regulaminu zawsze go wymaga, poprawki redakcyjne
     *                          kolejnych wersji mogą go nie wymagać
     */
    public void publish(int number, boolean requireAcceptance, UUID actor, Instant now) {
        requireDraft();
        List<CatalogLanguage> missing = missingLanguages();
        if (!missing.isEmpty()) {
            throw new BusinessRuleViolationException("legal.document.translation-missing",
                    "Every language needs a title and content before publishing; missing: " + missing);
        }
        if (number < 1) {
            throw new InvalidValueException("legal.document.version-number", "Version number must be positive");
        }
        this.status = Status.PUBLISHED;
        this.versionNumber = number;
        this.acceptanceRequired = type.canRequireAcceptance() && (requireAcceptance || number == 1);
        this.publishedAt = Objects.requireNonNull(now, "now");
        this.publishedBy = actor;
    }

    public List<CatalogLanguage> missingLanguages() {
        return Arrays.stream(CatalogLanguage.values()).filter(language -> !texts.containsKey(language)).toList();
    }

    /** Treść w danym języku, a gdy jej brak — w języku bazowym. */
    public Optional<LegalText> text(CatalogLanguage language) {
        return Optional.ofNullable(texts.getOrDefault(language, texts.get(CatalogLanguage.DEFAULT)));
    }

    /** Język, w którym faktycznie zostanie pokazana treść dla {@code requested}. */
    public CatalogLanguage resolvedLanguage(CatalogLanguage requested) {
        return texts.containsKey(requested) ? requested : CatalogLanguage.DEFAULT;
    }

    public boolean isDraft() {
        return status == Status.DRAFT;
    }

    public boolean isPublished() {
        return status == Status.PUBLISHED;
    }

    private void requireDraft() {
        if (status != Status.DRAFT) {
            throw new BusinessRuleViolationException("legal.document.published-immutable",
                    "A published version cannot be changed; create a new version instead");
        }
    }

    public LegalDocumentVersionId id() { return id; }
    public LegalDocumentType type() { return type; }
    public Status status() { return status; }
    public Integer versionNumber() { return versionNumber; }
    public Map<CatalogLanguage, LegalText> texts() { return Collections.unmodifiableMap(texts); }
    public boolean acceptanceRequired() { return acceptanceRequired; }
    public Instant publishedAt() { return publishedAt; }
    public UUID publishedBy() { return publishedBy; }
    public Instant createdAt() { return createdAt; }
    public long version() { return version; }
}
